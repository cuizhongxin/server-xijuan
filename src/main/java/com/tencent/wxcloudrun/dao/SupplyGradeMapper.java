package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SupplyGradeMapper {
    List<Map<String, Object>> findAll();
}
