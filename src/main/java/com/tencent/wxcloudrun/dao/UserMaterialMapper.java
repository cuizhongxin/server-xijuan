package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMaterialMapper {
    
    List<Map<String, Object>> findByUserId(@Param("userId") String userId);
    
    Integer findCount(@Param("userId") String userId, @Param("materialId") String materialId);
    
    void upsert(@Param("userId") String userId, @Param("materialId") String materialId, @Param("count") int count);
    
    void deleteByUserId(@Param("userId") String userId);
    
    void deleteByUserIdAndMaterialId(@Param("userId") String userId, @Param("materialId") String materialId);
}
