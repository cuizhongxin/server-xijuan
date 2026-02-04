package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.UserResource;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户资源仓库（内存存储）
 */
@Repository
public class UserResourceRepository {
    
    private final Map<String, UserResource> resourceStore = new ConcurrentHashMap<>();
    
    /**
     * 根据用户ID获取资源
     */
    public UserResource findByUserId(String odUserId) {
        return resourceStore.get(odUserId);
    }
    
    /**
     * 保存资源
     */
    public UserResource save(UserResource resource) {
        resource.setUpdateTime(System.currentTimeMillis());
        resourceStore.put(resource.getOdUserId(), resource);
        return resource;
    }
    
    /**
     * 初始化用户资源
     */
    public UserResource initUserResource(String odUserId) {
        UserResource existing = resourceStore.get(odUserId);
        if (existing != null) {
            return existing;
        }
        
        UserResource resource = UserResource.createDefault(odUserId);
        resourceStore.put(odUserId, resource);
        return resource;
    }
    
    /**
     * 判断是否存在
     */
    public boolean existsByUserId(String odUserId) {
        return resourceStore.containsKey(odUserId);
    }
}
