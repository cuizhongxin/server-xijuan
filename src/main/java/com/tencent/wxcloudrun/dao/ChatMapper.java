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
                       @Param("createTime") long createTime);

    List<Map<String, Object>> findRecent(@Param("channel") String channel,
                                          @Param("limit") int limit);

    List<Map<String, Object>> findSince(@Param("channel") String channel,
                                         @Param("sinceTime") long sinceTime,
                                         @Param("limit") int limit);

    // 公告
    List<Map<String, Object>> findActiveAnnouncements(@Param("now") long now);

    Map<String, Object> findAnnouncementById(@Param("id") int id);
}
