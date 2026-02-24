package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 武将槽位 Mapper
 */
@Mapper
public interface GeneralSlotMapper {

    /**
     * 根据 id 查询槽位
     */
    Map<String, Object> findById(@Param("id") int id);
}
