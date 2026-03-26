package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GeneralFamousTraitMapper {

    List<Map<String, Object>> findByGeneralPreId(@Param("generalPreId") int generalPreId);

    List<Map<String, Object>> findByGeneralName(@Param("name") String name);
}
