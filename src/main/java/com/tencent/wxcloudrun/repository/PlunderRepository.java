package com.tencent.wxcloudrun.repository;

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

    public PlunderData getOrInit(String userId) {
        PlunderData pd = findByUserId(userId);
        if (pd == null) {
            pd = PlunderData.builder()
                    .userId(userId)
                    .todayCount(0)
                    .todayPurchased(0)
                    .lastResetDate(todayStr())
                    .build();
            save(pd);
        }
        resetIfNewDay(pd);
        return pd;
    }

    public void save(PlunderData pd) {
        long now = System.currentTimeMillis();
        plunderMapper.upsert(pd.getUserId(), pd.getTodayCount(), pd.getTodayPurchased(),
                pd.getLastResetDate(), now, now);
    }

    public List<Map<String, Object>> findAllUserLevels() {
        return plunderMapper.findAllUserLevels();
    }

    private void resetIfNewDay(PlunderData pd) {
        String today = todayStr();
        if (!today.equals(pd.getLastResetDate())) {
            pd.setTodayCount(0);
            pd.setTodayPurchased(0);
            pd.setLastResetDate(today);
            save(pd);
        }
    }

    private String todayStr() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }
}
