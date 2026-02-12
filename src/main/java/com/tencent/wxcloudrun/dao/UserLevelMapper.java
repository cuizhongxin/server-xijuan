package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserLevelMapper {
    
    String findByUserId(@Param("userId") String userId);
    
    void upsert(@Param("userId") String userId, @Param("data") String data,
                @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);
    
    void deleteByUserId(@Param("userId") String userId);
}
