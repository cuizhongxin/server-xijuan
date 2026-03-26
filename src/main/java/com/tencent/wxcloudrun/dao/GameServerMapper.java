package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface GameServerMapper {

    List<Map<String, Object>> findAllServers();

    Map<String, Object> findServerById(@Param("id") int id);

    List<Map<String, Object>> findPlayerServers(@Param("userId") String userId);

    Map<String, Object> findPlayerServer(@Param("userId") String userId, @Param("serverId") int serverId);

    int isNameTaken(@Param("serverId") int serverId, @Param("lordName") String lordName);

    void insertPlayerServer(@Param("userId") String userId,
                            @Param("serverId") int serverId,
                            @Param("lordName") String lordName,
                            @Param("createTime") long createTime);

    void updatePlayerLogin(@Param("userId") String userId,
                           @Param("serverId") int serverId,
                           @Param("lastLogin") long lastLogin);

    void incrementServerPlayers(@Param("serverId") int serverId);

    void insertServer(@Param("serverName") String serverName,
                      @Param("status") String status,
                      @Param("openTime") long openTime,
                      @Param("maxPlayers") int maxPlayers);

    List<Map<String, Object>> findAllPlayerServers();

    void updateServerStatus(@Param("id") int id, @Param("status") String status);

    void updateServerName(@Param("id") int id, @Param("name") String name);

    List<Map<String, Object>> findPlayerServersByServerId(@Param("serverId") int serverId);

    void migratePlayerServer(@Param("sourceServerId") int sourceServerId,
                             @Param("targetServerId") int targetServerId);
}
