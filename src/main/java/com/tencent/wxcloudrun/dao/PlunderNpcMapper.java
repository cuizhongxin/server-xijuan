package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlunderNpcMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findByFaction(@Param("faction") String faction);
}
