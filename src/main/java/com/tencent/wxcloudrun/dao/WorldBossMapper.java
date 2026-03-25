package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WorldBossMapper {

    Map<String, Object> findState(@Param("serverId") int serverId, @Param("bossId") int bossId);

    void upsertState(@Param("serverId") int serverId,
                     @Param("bossId") int bossId,
                     @Param("status") String status,
                     @Param("unitSoldiers") String unitSoldiers,
                     @Param("lastKiller") String lastKiller,
                     @Param("windowStartMs") long windowStartMs);

    void updateStatus(@Param("serverId") int serverId,
                      @Param("bossId") int bossId,
                      @Param("status") String status,
                      @Param("lastKiller") String lastKiller);

    void updateUnitSoldiers(@Param("serverId") int serverId,
                            @Param("bossId") int bossId,
                            @Param("unitSoldiers") String unitSoldiers);

    Map<String, Object> findPlayerDamage(@Param("serverId") int serverId,
                                         @Param("bossId") int bossId,
                                         @Param("userId") String userId,
                                         @Param("windowStartMs") long windowStartMs);

    void upsertPlayerDamage(@Param("serverId") int serverId,
                            @Param("bossId") int bossId,
                            @Param("userId") String userId,
                            @Param("windowStartMs") long windowStartMs,
                            @Param("damage") long damage,
                            @Param("cooldownUntil") long cooldownUntil);

    List<Map<String, Object>> findDamageRanking(@Param("serverId") int serverId,
                                                @Param("bossId") int bossId,
                                                @Param("windowStartMs") long windowStartMs,
                                                @Param("limit") int limit);
}
