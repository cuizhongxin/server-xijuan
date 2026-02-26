package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.UserResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserResourceMapper {
    
    UserResource findByUserId(@Param("odUserId") String odUserId);
    
    void upsert(UserResource resource);
    
    int existsByUserId(@Param("odUserId") String odUserId);
}
