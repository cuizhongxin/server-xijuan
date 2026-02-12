package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.UserIdMappingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户ID管理服务
 * 负责维护openId到userId的映射关系（数据库存储）
 */
@Service
public class UserIdService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserIdService.class);
    
    @Autowired
    private UserIdMappingMapper userIdMappingMapper;
    
    /**
     * 获取或创建用户ID
     * 如果openId已存在，返回已有的userId；否则创建新的userId
     * 
     * @param openId 微信openId
     * @return 用户ID
     */
    public Long getOrCreateUserId(String openId) {
        if (openId == null || openId.isEmpty()) {
            throw new IllegalArgumentException("openId不能为空");
        }
        
        // 先检查是否已存在
        Long userId = userIdMappingMapper.findUserIdByOpenId(openId);
        if (userId != null) {
            logger.debug("找到已有用户ID: openId={}, userId={}", openId, userId);
            return userId;
        }
        
        // 不存在则创建新的userId（使用数据库自增ID）
        synchronized (this) {
            // 双重检查，防止并发创建
            userId = userIdMappingMapper.findUserIdByOpenId(openId);
            if (userId != null) {
                return userId;
            }
            
            // 插入新记录，使用数据库自增ID
            userIdMappingMapper.insert(openId);
            userId = userIdMappingMapper.findUserIdByOpenId(openId);
            
            logger.info("创建新用户ID: openId={}, userId={}", openId, userId);
            return userId;
        }
    }
    
    /**
     * 根据openId获取userId
     * 
     * @param openId 微信openId
     * @return 用户ID，如果不存在返回null
     */
    public Long getUserId(String openId) {
        return userIdMappingMapper.findUserIdByOpenId(openId);
    }
    
    /**
     * 根据userId获取openId
     * 
     * @param userId 用户ID
     * @return openId，如果不存在返回null
     */
    public String getOpenId(Long userId) {
        return userIdMappingMapper.findOpenIdByUserId(userId);
    }
    
    /**
     * 验证userId是否有效
     * 
     * @param userId 用户ID
     * @return 是否有效
     */
    public boolean isValidUserId(Long userId) {
        if (userId == null) return false;
        String openId = userIdMappingMapper.findOpenIdByUserId(userId);
        return openId != null;
    }
}
