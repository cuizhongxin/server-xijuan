package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SimulationConfigMapper {

    List<Map<String, Object>> findAllServerProfiles();

    Map<String, Object> findServerProfile(@Param("serverId") int serverId);

    void upsertServerProfile(@Param("serverId") int serverId,
                             @Param("activityProfile") String activityProfile,
                             @Param("pveMultiplier") Double pveMultiplier,
                             @Param("pvpMultiplier") Double pvpMultiplier,
                             @Param("economyMultiplier") Double economyMultiplier,
                             @Param("productionMultiplier") Double productionMultiplier,
                             @Param("socialMultiplier") Double socialMultiplier,
                             @Param("growthMultiplier") Double growthMultiplier,
                             @Param("chatMultiplier") Double chatMultiplier,
                             @Param("daytimeProductionMultiplier") Double daytimeProductionMultiplier,
                             @Param("nightPvpMultiplier") Double nightPvpMultiplier,
                             @Param("enabled") Boolean enabled,
                             @Param("updatedTime") Long updatedTime);

    List<Map<String, Object>> findChatTemplatesByServer(@Param("serverId") int serverId);

    void deleteChatTemplatesByServer(@Param("serverId") int serverId);

    void insertChatTemplate(@Param("serverId") int serverId,
                            @Param("channel") String channel,
                            @Param("content") String content,
                            @Param("weight") Double weight,
                            @Param("enabled") Boolean enabled,
                            @Param("updatedTime") Long updatedTime);
}
