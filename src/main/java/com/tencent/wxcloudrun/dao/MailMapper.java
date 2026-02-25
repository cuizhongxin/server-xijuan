package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface MailMapper {

    @Insert("INSERT INTO mail (sender_id, sender_name, receiver_id, mail_type, title, content, " +
            "has_attachment, create_time, expire_time) VALUES " +
            "(#{senderId}, #{senderName}, #{receiverId}, #{mailType}, #{title}, #{content}, " +
            "#{hasAttachment}, #{createTime}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertMail(Map<String, Object> mail);

    @Insert("INSERT INTO mail_attachment (mail_id, item_type, item_id, item_name, item_quality, count) " +
            "VALUES (#{mailId}, #{itemType}, #{itemId}, #{itemName}, #{itemQuality}, #{count})")
    int insertAttachment(Map<String, Object> attachment);

    @Select("SELECT * FROM mail WHERE receiver_id = #{receiverId} AND deleted = 0 " +
            "ORDER BY is_read ASC, create_time DESC LIMIT 50")
    List<Map<String, Object>> findByReceiver(@Param("receiverId") String receiverId);

    @Select("SELECT * FROM mail WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") long id);

    @Select("SELECT * FROM mail_attachment WHERE mail_id = #{mailId}")
    List<Map<String, Object>> findAttachments(@Param("mailId") long mailId);

    @Update("UPDATE mail SET is_read = 1 WHERE id = #{id}")
    int markRead(@Param("id") long id);

    @Update("UPDATE mail SET attachment_claimed = 1 WHERE id = #{id}")
    int markAttachmentClaimed(@Param("id") long id);

    @Update("UPDATE mail SET deleted = 1 WHERE id = #{id} AND receiver_id = #{receiverId}")
    int deleteMail(@Param("id") long id, @Param("receiverId") String receiverId);

    @Select("SELECT COUNT(*) FROM mail WHERE receiver_id = #{receiverId} AND deleted = 0 AND is_read = 0")
    int countUnread(@Param("receiverId") String receiverId);
}
