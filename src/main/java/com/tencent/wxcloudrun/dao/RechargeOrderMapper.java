package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RechargeOrderMapper {
    
    String findById(@Param("id") String id);
    
    List<Map<String, Object>> findByUserId(@Param("odUserId") String odUserId);
    
    void upsert(@Param("id") String id, @Param("odUserId") String odUserId, @Param("status") String status,
                @Param("data") String data, @Param("createTime") Long createTime);
}
