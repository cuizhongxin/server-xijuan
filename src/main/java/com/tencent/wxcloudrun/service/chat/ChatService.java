package com.tencent.wxcloudrun.service.chat;

import com.tencent.wxcloudrun.dao.ChatMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_MSG_LENGTH = 200;
    private static final long MIN_SEND_INTERVAL = 2000;

    private final Map<String, Long> lastSendTime = new HashMap<>();

    @Autowired
    private ChatMapper chatMapper;

    /**
     * 发送聊天消息
     */
    public Map<String, Object> sendMessage(String userId, String userName, String channel, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "消息不能为空");
        }
        if (content.length() > MAX_MSG_LENGTH) {
            throw new BusinessException(400, "消息不能超过" + MAX_MSG_LENGTH + "字");
        }

        long now = System.currentTimeMillis();
        Long last = lastSendTime.get(userId);
        if (last != null && now - last < MIN_SEND_INTERVAL) {
            throw new BusinessException(400, "发言太频繁，请稍后再试");
        }
        lastSendTime.put(userId, now);

        if (channel == null || channel.isEmpty()) channel = "world";

        chatMapper.insertMessage(userId, userName, channel, content.trim(), now);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("createTime", now);
        return result;
    }

    /**
     * 获取最近消息
     */
    public List<Map<String, Object>> getRecent(String channel, int limit) {
        if (channel == null || channel.isEmpty()) channel = "world";
        if (limit <= 0 || limit > 100) limit = 30;
        List<Map<String, Object>> msgs = chatMapper.findRecent(channel, limit);
        Collections.reverse(msgs);
        return msgs;
    }

    /**
     * 轮询新消息（自某个时间戳之后的消息）
     */
    public List<Map<String, Object>> poll(String channel, long sinceTime) {
        if (channel == null || channel.isEmpty()) channel = "world";
        return chatMapper.findSince(channel, sinceTime, 50);
    }

    /**
     * 获取活跃公告
     */
    public List<Map<String, Object>> getAnnouncements() {
        return chatMapper.findActiveAnnouncements(System.currentTimeMillis());
    }
}
