package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AllianceBossMapper {

    Map<String, Object> findCurrentBoss();

    void insertBoss(@Param("bossLevel") int bossLevel,
                    @Param("bossName") String bossName,
                    @Param("maxHp") long maxHp);

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
                      @Param("feedAmount") int feedAmount);

    List<Map<String, Object>> findRecords(@Param("limit") int limit);

    List<Map<String, Object>> findRankings(@Param("limit") int limit);

    int findUserDailyAttackCount(@Param("userId") String userId);
}
