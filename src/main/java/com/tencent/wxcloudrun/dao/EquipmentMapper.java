package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface EquipmentMapper {
    
    String findById(@Param("id") String id);
    
    List<Map<String, Object>> findByUserId(@Param("userId") String userId);
    
    void upsert(@Param("id") String id, @Param("userId") String userId, @Param("data") String data,
                @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);
    
    void deleteById(@Param("id") String id);
    
    void deleteByUserId(@Param("userId") String userId);
    
    int countByUserId(@Param("userId") String userId);
}
