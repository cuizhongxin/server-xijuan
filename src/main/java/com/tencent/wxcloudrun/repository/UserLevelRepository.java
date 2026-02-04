package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.UserLevel;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户等级数据仓库（内存存储）
 */
@Repository
public class UserLevelRepository {
    
    private final Map<String, UserLevel> levelStore = new ConcurrentHashMap<>();
    
    /**
     * 获取用户等级信息
     */
    public UserLevel findByUserId(String userId) {
        return levelStore.get(userId);
    }
    
    /**
     * 保存或更新用户等级信息
     */
    public UserLevel save(UserLevel userLevel) {
        userLevel.setUpdateTime(System.currentTimeMillis());
        if (userLevel.getCreateTime() == null) {
            userLevel.setCreateTime(System.currentTimeMillis());
        }
        levelStore.put(userLevel.getUserId(), userLevel);
        return userLevel;
    }
    
    /**
     * 初始化用户等级
     */
    public UserLevel initUserLevel(String userId) {
        UserLevel userLevel = UserLevel.builder()
            .userId(userId)
            .level(1)
            .totalExp(0L)
            .currentLevelExp(0L)
            .expToNextLevel(100L)
            .vipLevel(0)
            .todayExp(0L)
            .lastUpdateDate("")
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
        
        return save(userLevel);
    }
    
    /**
     * 删除用户等级数据
     */
    public void deleteByUserId(String userId) {
        levelStore.remove(userId);
    }
    
    /**
     * 清空所有数据
     */
    public void clear() {
        levelStore.clear();
    }
}


