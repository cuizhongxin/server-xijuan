package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserIdMappingMapper {
    
    Long findUserIdByOpenId(@Param("openId") String openId);
    
    String findOpenIdByUserId(@Param("userId") Long userId);
    
    void insert(@Param("openId") String openId);
    
    Long getMaxUserId();
}
