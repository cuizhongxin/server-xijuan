package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.UserResourceMapper;
import com.tencent.wxcloudrun.model.UserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 用户资源仓库（数据库存储）
 */
@Repository
public class UserResourceRepository {
    
    @Autowired
    private UserResourceMapper userResourceMapper;
    
    /**
     * 根据用户ID获取资源
     */
    public UserResource findByUserId(String odUserId) {
        return userResourceMapper.findByUserId(odUserId);
    }
    
    /**
     * 保存资源
     */
    public UserResource save(UserResource resource) {
        resource.setUpdateTime(System.currentTimeMillis());
        if (resource.getCreateTime() == null) {
            resource.setCreateTime(System.currentTimeMillis());
        }
        userResourceMapper.upsert(resource);
        return resource;
    }
    
    /**
     * 初始化用户资源
     */
    public UserResource initUserResource(String odUserId) {
        UserResource existing = findByUserId(odUserId);
        if (existing != null) {
            return existing;
        }
        
        UserResource resource = UserResource.createDefault(odUserId);
        resource.setCreateTime(System.currentTimeMillis());
        resource.setUpdateTime(System.currentTimeMillis());
        userResourceMapper.upsert(resource);
        return resource;
    }
    
    /**
     * 判断是否存在
     */
    public boolean existsByUserId(String odUserId) {
        return userResourceMapper.existsByUserId(odUserId) > 0;
    }
}
