package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WarehouseMapper {
    
    String findByUserId(@Param("userId") String userId);
    
    void upsert(@Param("id") String id, @Param("userId") String userId, @Param("data") String data,
                @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);
}
