package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 武将品质配置 Mapper
 */
@Mapper
public interface GeneralQualityMapper {

    /**
     * 查询所有品质配置
     */
    List<Map<String, Object>> findAll();
}
