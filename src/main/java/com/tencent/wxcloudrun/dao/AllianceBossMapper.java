package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AllianceBossMapper {

    Map<String, Object> findCurrentBossByServerId(@Param("serverId") int serverId);

    void insertBoss(@Param("bossLevel") int bossLevel,
                    @Param("bossName") String bossName,
                    @Param("maxHp") long maxHp,
                    @Param("serverId") int serverId);

    void updateBossHp(@Param("id") long id, @Param("currentHp") long currentHp);

    void updateBossStatus(@Param("id") long id, @Param("status") String status);

    void resetBoss(@Param("id") long id,
                   @Param("bossLevel") int bossLevel,
                   @Param("bossName") String bossName,
                   @Param("maxHp") long maxHp,
                   @Param("currentHp") long currentHp);

    void incrementFeed(@Param("id") long id, @Param("amount") int amount);

    void insertRecord(@Param("userId") String userId,
                      @Param("actionType") String actionType,
                      @Param("damage") long damage,
                      @Param("feedAmount") int feedAmount,
                      @Param("serverId") int serverId);

    List<Map<String, Object>> findRecordsByServerId(@Param("serverId") int serverId,
                                                    @Param("limit") int limit);

    List<Map<String, Object>> findRankingsByServerId(@Param("serverId") int serverId,
                                                     @Param("limit") int limit);

    int findUserDailyAttackCount(@Param("userId") String userId);
}
