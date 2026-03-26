package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMapper {

    void insertMessage(@Param("senderId") String senderId,
                       @Param("senderName") String senderName,
                       @Param("channel") String channel,
                       @Param("content") String content,
                       @Param("serverId") int serverId,
                       @Param("createTime") long createTime);

    void insertMessageFull(@Param("senderId") String senderId,
                           @Param("senderName") String senderName,
                           @Param("channel") String channel,
                           @Param("content") String content,
                           @Param("serverId") int serverId,
                           @Param("createTime") long createTime,
                           @Param("targetId") String targetId,
                           @Param("nationId") String nationId,
                           @Param("allianceId") String allianceId);

    List<Map<String, Object>> findRecent(@Param("channel") String channel,
                                          @Param("serverId") int serverId,
                                          @Param("limit") int limit);

    List<Map<String, Object>> findSince(@Param("channel") String channel,
                                         @Param("serverId") int serverId,
                                         @Param("sinceTime") long sinceTime,
                                         @Param("limit") int limit);

    List<Map<String, Object>> findRecentByNation(@Param("serverId") int serverId,
                                                  @Param("nationId") String nationId,
                                                  @Param("limit") int limit);

    List<Map<String, Object>> pollByNation(@Param("serverId") int serverId,
                                            @Param("nationId") String nationId,
                                            @Param("sinceTime") long sinceTime,
                                            @Param("limit") int limit);

    List<Map<String, Object>> findRecentByAlliance(@Param("serverId") int serverId,
                                                    @Param("allianceId") String allianceId,
                                                    @Param("limit") int limit);

    List<Map<String, Object>> pollByAlliance(@Param("serverId") int serverId,
                                              @Param("allianceId") String allianceId,
                                              @Param("sinceTime") long sinceTime,
                                              @Param("limit") int limit);

    List<Map<String, Object>> findRecentPrivate(@Param("serverId") int serverId,
                                                 @Param("userId") String userId,
                                                 @Param("limit") int limit);

    List<Map<String, Object>> pollPrivate(@Param("serverId") int serverId,
                                           @Param("userId") String userId,
                                           @Param("sinceTime") long sinceTime,
                                           @Param("limit") int limit);

    List<Map<String, Object>> findPrivateConversation(@Param("serverId") int serverId,
                                                       @Param("userId") String userId,
                                                       @Param("targetId") String targetId,
                                                       @Param("limit") int limit);

    List<Map<String, Object>> findPrivateContacts(@Param("serverId") int serverId,
                                                   @Param("userId") String userId,
                                                   @Param("limit") int limit);

    List<Map<String, Object>> findActiveAnnouncements(@Param("now") long now);

    Map<String, Object> findAnnouncementById(@Param("id") int id);
}
