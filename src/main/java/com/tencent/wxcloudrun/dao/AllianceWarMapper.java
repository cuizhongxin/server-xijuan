package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AllianceWarMapper {
    
    String findByDate(@Param("warDate") String warDate);
    
    void upsert(@Param("warDate") String warDate, @Param("data") String data);
}
