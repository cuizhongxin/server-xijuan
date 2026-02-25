package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlunderRecordMapper {

    void insert(@Param("attackerId") String attackerId,
                @Param("attackerName") String attackerName,
                @Param("attackerLevel") Integer attackerLevel,
                @Param("defenderId") String defenderId,
                @Param("defenderName") String defenderName,
                @Param("defenderLevel") Integer defenderLevel,
                @Param("defenderFaction") String defenderFaction,
                @Param("isNpc") Boolean isNpc,
                @Param("victory") Boolean victory,
                @Param("silverGain") Long silverGain,
                @Param("woodGain") Long woodGain,
                @Param("paperGain") Long paperGain,
                @Param("foodGain") Long foodGain,
                @Param("createTime") Long createTime);

    /** 查询某用户发起的掠夺记录（攻击记录） */
    List<Map<String, Object>> findByAttacker(@Param("attackerId") String attackerId,
                                              @Param("limit") int limit);

    /** 查询某用户被掠夺的记录（被攻击记录） */
    List<Map<String, Object>> findByDefender(@Param("defenderId") String defenderId,
                                              @Param("limit") int limit);

    /** 查询指定攻击者对指定防御者在指定时间之后的记录（冷却检查） */
    int countRecentAttack(@Param("attackerId") String attackerId,
                          @Param("defenderId") String defenderId,
                          @Param("sinceTime") Long sinceTime);
}
