package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface SecretRealmPityMapper {

    Map<String, Object> findByUserAndRealm(@Param("userId") String userId, @Param("realmId") String realmId);

    void upsert(@Param("userId") String userId,
                @Param("realmId") String realmId,
                @Param("countSinceEquip") int countSinceEquip,
                @Param("totalExploreCount") int totalExploreCount,
                @Param("totalEquipCount") int totalEquipCount,
                @Param("lastEquipTime") long lastEquipTime,
                @Param("dailyCount") int dailyCount,
                @Param("dailyResetDate") String dailyResetDate,
                @Param("updateTime") long updateTime);
}
