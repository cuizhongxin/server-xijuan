package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.config.PlunderConfig;
import com.tencent.wxcloudrun.dao.PlunderMapper;
import com.tencent.wxcloudrun.model.PlunderData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.*;

@Repository
public class PlunderRepository {

    private static final Logger logger = LoggerFactory.getLogger(PlunderRepository.class);

    @Autowired
    private PlunderMapper plunderMapper;

    public PlunderData findByUserId(String userId) {
        return plunderMapper.findByUserId(userId);
    }

    /**
     * 获取或初始化掠夺数据，自动执行时间恢复 & 每日购买次数重置
     */
    public PlunderData getOrInit(String userId) {
        PlunderData pd = findByUserId(userId);
        long now = System.currentTimeMillis();
        if (pd == null) {
            pd = PlunderData.builder()
                    .userId(userId)
                    .availableCount(PlunderConfig.MAX_PLUNDER_COUNT)
                    .lastRecoverTime(now)
                    .todayPurchased(0)
                    .lastResetDate(todayStr())
                    .todayCount(0)
                    .build();
            save(pd);
            return pd;
        }

        boolean dirty = false;

        // 时间自动恢复掠夺次数
        dirty |= autoRecover(pd, now);

        // 每日重置购买次数
        dirty |= resetPurchaseIfNewDay(pd);

        if (dirty) save(pd);
        return pd;
    }

    /**
     * 按时间恢复掠夺次数: 每30分钟+1, 上限 MAX_PLUNDER_COUNT
     */
    private boolean autoRecover(PlunderData pd, long now) {
        int current = pd.getAvailableCount() != null ? pd.getAvailableCount() : 0;
        if (current >= PlunderConfig.MAX_PLUNDER_COUNT) {
            pd.setLastRecoverTime(now);
            return false;
        }

        long lastRecover = pd.getLastRecoverTime() != null ? pd.getLastRecoverTime() : now;
        if (lastRecover <= 0) {
            pd.setLastRecoverTime(now);
            return true;
        }

        long elapsed = now - lastRecover;
        int ticks = (int) (elapsed / PlunderConfig.PLUNDER_RECOVER_MS);
        if (ticks > 0) {
            int recovered = ticks * PlunderConfig.PLUNDER_RECOVER_AMOUNT;
            pd.setAvailableCount(Math.min(PlunderConfig.MAX_PLUNDER_COUNT, current + recovered));
            pd.setLastRecoverTime(lastRecover + (long) ticks * PlunderConfig.PLUNDER_RECOVER_MS);
            return true;
        }
        return false;
    }

    /**
     * 每天重置购买次数（购买的额外次数不累积到第二天）
     */
    private boolean resetPurchaseIfNewDay(PlunderData pd) {
        String today = todayStr();
        if (!today.equals(pd.getLastResetDate())) {
            pd.setTodayPurchased(0);
            pd.setLastResetDate(today);
            return true;
        }
        return false;
    }

    public void save(PlunderData pd) {
        long now = System.currentTimeMillis();
        plunderMapper.upsert(pd.getUserId(),
                pd.getAvailableCount(),
                pd.getLastRecoverTime(),
                pd.getTodayPurchased(),
                pd.getLastResetDate(),
                pd.getTodayCount(),
                now);
    }

    public List<Map<String, Object>> findUserLevelsByServerId(int serverId) {
        return plunderMapper.findUserLevelsByServerId(String.valueOf(serverId));
    }

    private String todayStr() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }
}
