package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ServerMergeMapper {

    void renameConflictAlliances(@Param("sourceServerId") int sourceServerId,
                                 @Param("targetServerId") int targetServerId);

    void migrateAlliance(@Param("source") int source, @Param("target") int target);

    void migrateChat(@Param("source") int source, @Param("target") int target);

    void cancelMarketListings(@Param("serverId") int serverId);

    void deleteCityOwners(@Param("serverId") int serverId);

    void migratePlayerNation(@Param("source") int source, @Param("target") int target);

    void migrateSupplyTransport(@Param("source") int source, @Param("target") int target);

    void deleteCornucopiaPeriod(@Param("serverId") int serverId);

    void deleteWorldBossState(@Param("serverId") int serverId);

    void deleteWorldBossDamage(@Param("serverId") int serverId);

    void deleteAllianceBoss(@Param("serverId") int serverId);

    void deleteAllianceBossRecord(@Param("serverId") int serverId);
}
