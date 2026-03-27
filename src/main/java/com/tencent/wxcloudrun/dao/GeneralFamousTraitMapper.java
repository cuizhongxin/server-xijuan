package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GeneralFamousTraitMapper {

    List<Map<String, Object>> findByGeneralTemplateId(@Param("generalTemplateId") int generalTemplateId);

    List<Map<String, Object>> findByGeneralName(@Param("name") String name);

    List<Map<String, Object>> findByTemplateIds(@Param("ids") List<Integer> ids);
}
