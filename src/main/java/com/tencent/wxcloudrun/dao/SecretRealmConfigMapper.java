package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SecretRealmConfigMapper {

    List<Map<String, Object>> findAll();

    Map<String, Object> findById(@Param("id") String id);
}
