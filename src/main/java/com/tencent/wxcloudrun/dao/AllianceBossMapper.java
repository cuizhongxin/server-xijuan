package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AllianceBossMapper {

    Map<String, Object> findCurrentBossByServerAndAlliance(@Param("serverId") int serverId,
                                                            @Param("allianceId") String allianceId);

    Map<String, Object> findCurrentBossByServerAndAllianceForUpdate(@Param("serverId") int serverId,
                                                                     @Param("allianceId") String allianceId);

    Map<String, Object> findLegacyBossByServerId(@Param("serverId") int serverId);

    void insertBoss(@Param("bossLevel") int bossLevel,
                    @Param("bossName") String bossName,
                    @Param("maxHp") long maxHp,
                    @Param("serverId") int serverId,
                    @Param("allianceId") String allianceId);

    void updateBossHp(@Param("id") long id, @Param("currentHp") long currentHp);

    void updateBossHpConfig(@Param("id") long id,
                            @Param("maxHp") long maxHp,
                            @Param("currentHp") long currentHp);

    void updateBossStatus(@Param("id") long id, @Param("status") String status);

    int updateBossStatusByExpect(@Param("id") long id,
                                 @Param("fromStatus") String fromStatus,
                                 @Param("toStatus") String toStatus);

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
                      @Param("serverId") int serverId,
                      @Param("allianceId") String allianceId,
                      @Param("cycleId") int cycleId);

    List<Map<String, Object>> findRecordsByServerAndAlliance(@Param("serverId") int serverId,
                                                              @Param("allianceId") String allianceId,
                                                              @Param("limit") int limit);

    List<Map<String, Object>> findRankingsByServerAndAlliance(@Param("serverId") int serverId,
                                                               @Param("allianceId") String allianceId,
                                                               @Param("limit") int limit);

    List<Map<String, Object>> findDailyAttackRankingsByScope(@Param("serverId") int serverId,
                                                              @Param("allianceId") String allianceId,
                                                              @Param("cycleId") int cycleId,
                                                              @Param("limit") int limit);

    List<Map<String, Object>> findDailyFeedRankingsByScope(@Param("serverId") int serverId,
                                                            @Param("allianceId") String allianceId,
                                                            @Param("cycleId") int cycleId,
                                                            @Param("limit") int limit);

    int countActionByScope(@Param("serverId") int serverId,
                           @Param("allianceId") String allianceId,
                           @Param("cycleId") int cycleId,
                           @Param("actionType") String actionType);

    int findUserDailyAttackCount(@Param("userId") String userId,
                                 @Param("serverId") int serverId,
                                 @Param("allianceId") String allianceId,
                                 @Param("cycleId") int cycleId);
}
