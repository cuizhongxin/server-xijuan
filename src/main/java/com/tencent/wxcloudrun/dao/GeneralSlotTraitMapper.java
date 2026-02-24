package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 武将槽位特征 Mapper
 */
@Mapper
public interface GeneralSlotTraitMapper {

    /**
     * 根据槽位 id 列表批量查询特征，返回 (slot_id, trait_type, trait_value)
     */
    List<Map<String, Object>> findBySlotIds(@Param("slotIds") List<Integer> slotIds);
}
