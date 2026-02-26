package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipmentMapper {
    
    Equipment findById(@Param("id") String id);
    
    List<Equipment> findByUserId(@Param("userId") String userId);
    
    void upsert(Equipment equipment);
    
    void deleteById(@Param("id") String id);
    
    void deleteByUserId(@Param("userId") String userId);
    
    int countByUserId(@Param("userId") String userId);
    
    List<Equipment> findUnequippedByUserId(@Param("userId") String userId);
    
    List<Equipment> findEquippedByGeneralId(@Param("generalId") String generalId);
}
