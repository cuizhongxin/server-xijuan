package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.UserLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserLevelMapper {
    
    UserLevel findByUserId(@Param("userId") String userId);
    
    void upsert(UserLevel userLevel);
    
    void deleteByUserId(@Param("userId") String userId);
}
