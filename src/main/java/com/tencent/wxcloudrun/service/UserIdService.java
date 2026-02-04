package com.tencent.wxcloudrun.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户ID管理服务
 * 负责生成自增ID并维护openId到userId的映射关系
 */
@Service
public class UserIdService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserIdService.class);
    
    // 自增ID生成器
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    // openId -> userId 映射（存储在内存中）
    private final ConcurrentHashMap<String, Long> openIdToUserIdMap = new ConcurrentHashMap<>();
    
    // userId -> openId 映射（反向映射，用于验证）
    private final ConcurrentHashMap<Long, String> userIdToOpenIdMap = new ConcurrentHashMap<>();
    
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
        Long userId = openIdToUserIdMap.get(openId);
        if (userId != null) {
            logger.debug("找到已有用户ID: openId={}, userId={}", openId, userId);
            return userId;
        }
        
        // 不存在则创建新的userId
        synchronized (this) {
            // 双重检查，防止并发创建
            userId = openIdToUserIdMap.get(openId);
            if (userId != null) {
                return userId;
            }
            
            // 生成新的自增ID
            userId = idGenerator.getAndIncrement();
            
            // 存储映射关系
            openIdToUserIdMap.put(openId, userId);
            userIdToOpenIdMap.put(userId, openId);
            
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
        return openIdToUserIdMap.get(openId);
    }
    
    /**
     * 根据userId获取openId
     * 
     * @param userId 用户ID
     * @return openId，如果不存在返回null
     */
    public String getOpenId(Long userId) {
        return userIdToOpenIdMap.get(userId);
    }
    
    /**
     * 验证userId是否有效
     * 
     * @param userId 用户ID
     * @return 是否有效
     */
    public boolean isValidUserId(Long userId) {
        return userId != null && userIdToOpenIdMap.containsKey(userId);
    }
}
