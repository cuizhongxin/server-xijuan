package com.tencent.wxcloudrun.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PlunderMapper plunderMapper;

    public PlunderData findByUserId(String userId) {
        String data = plunderMapper.findByUserId(userId);
        if (data == null) return null;
        try {
            return objectMapper.readValue(data, PlunderData.class);
        } catch (Exception e) {
            logger.error("解析掠夺数据失败: userId={}", userId, e);
            return null;
        }
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
        try {
            String data = objectMapper.writeValueAsString(pd);
            plunderMapper.upsert(pd.getUserId(), data,
                    System.currentTimeMillis(), System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("保存掠夺数据失败: userId={}", pd.getUserId(), e);
        }
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
