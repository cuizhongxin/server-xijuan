package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductionMapper {
    
    String findByUserId(@Param("odUserId") String odUserId);
    
    void upsert(@Param("odUserId") String odUserId, @Param("data") String data);
}
