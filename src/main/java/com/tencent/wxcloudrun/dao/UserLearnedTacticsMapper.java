package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserLearnedTacticsMapper {
    
    String findByUserId(@Param("userId") String userId);
    
    void upsert(@Param("userId") String userId, @Param("tacticsIds") String tacticsIds);
}
