package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SuitConfigMapper {

    List<Map<String, Object>> findAll();

    Map<String, Object> findById(@Param("suitId") int suitId);

    Map<String, Object> findByName(@Param("name") String name);

    List<Map<String, Object>> findByEquipId(@Param("equipId") String equipId);
}
