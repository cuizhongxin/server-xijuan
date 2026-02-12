package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserResourceMapper {
    
    String findByUserId(@Param("odUserId") String odUserId);
    
    void upsert(@Param("odUserId") String odUserId, @Param("data") String data,
                @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);
    
    int existsByUserId(@Param("odUserId") String odUserId);
}
