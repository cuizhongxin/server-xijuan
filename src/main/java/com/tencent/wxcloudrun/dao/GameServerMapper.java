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
}
