package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface SupplyRobberyMapper {

    void insert(@Param("attackerId") String attackerId,
                @Param("attackerName") String attackerName,
                @Param("defenderId") String defenderId,
                @Param("defenderName") String defenderName,
                @Param("transportId") long transportId,
                @Param("gradeName") String gradeName,
                @Param("victory") boolean victory,
                @Param("silverStolen") long silverStolen,
                @Param("paperStolen") long paperStolen,
                @Param("foodStolen") long foodStolen,
                @Param("metalStolen") long metalStolen,
                @Param("createTime") long createTime,
                @Param("createDate") String createDate);

    List<Map<String, Object>> findByAttacker(@Param("attackerId") String attackerId,
                                              @Param("limit") int limit);

    List<Map<String, Object>> findByDefender(@Param("defenderId") String defenderId,
                                              @Param("limit") int limit);

    int countTodayByAttacker(@Param("attackerId") String attackerId,
                             @Param("createDate") String createDate);
}
