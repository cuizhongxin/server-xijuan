package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 武将模板 Mapper
 */
@Mapper
public interface GeneralTemplateMapper {

    /**
     * 按品质查询所有模板（含槽位信息：type, troop_type, base_* 等）
     * 用于招募时按品质随机选将
     */
    List<Map<String, Object>> listByQualityCode(@Param("qualityCode") String qualityCode);

    /**
     * 按品质与势力筛选可招募模板（本国 + 群 + 虚构）
     */
    List<Map<String, Object>> listRecruitableByQualityAndFaction(
            @Param("qualityCode") String qualityCode,
            @Param("factions") List<String> factions);
}
