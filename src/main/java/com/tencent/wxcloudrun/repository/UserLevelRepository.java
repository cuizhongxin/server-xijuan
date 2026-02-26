package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.UserLevelMapper;
import com.tencent.wxcloudrun.model.UserLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 用户等级数据仓库（数据库存储）
 */
@Repository
public class UserLevelRepository {
    
    @Autowired
    private UserLevelMapper userLevelMapper;
    
    /**
     * 获取用户等级信息
     */
    public UserLevel findByUserId(String userId) {
        return userLevelMapper.findByUserId(userId);
    }
    
    /**
     * 保存或更新用户等级信息
     */
    public UserLevel save(UserLevel userLevel) {
        userLevel.setUpdateTime(System.currentTimeMillis());
        if (userLevel.getCreateTime() == null) {
            userLevel.setCreateTime(System.currentTimeMillis());
        }
        userLevelMapper.upsert(userLevel);
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
        userLevelMapper.deleteByUserId(userId);
    }
    
    /**
     * 清空所有数据
     */
    public void clear() {
        // 数据库模式下不支持清空全表，忽略此操作
    }
}
