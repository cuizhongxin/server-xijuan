package com.tencent.wxcloudrun.service.chat;

import com.tencent.wxcloudrun.dao.ChatMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.PlayerNameResolver;
import com.tencent.wxcloudrun.service.alliance.AllianceService;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_MSG_LENGTH = 200;
    private static final long MIN_SEND_INTERVAL = 2000;

    private final Map<String, Long> lastSendTime = new HashMap<>();

    @Autowired private ChatMapper chatMapper;
    @Autowired private PlayerNameResolver playerNameResolver;
    @Autowired @Lazy private NationWarService nationWarService;
    @Autowired @Lazy private AllianceService allianceService;

    static int extractServerId(String compositeUserId) {
        if (compositeUserId == null) return 1;
        int idx = compositeUserId.lastIndexOf('_');
        if (idx > 0) {
            try { return Integer.parseInt(compositeUserId.substring(idx + 1)); }
            catch (NumberFormatException e) { return 1; }
        }
        return 1;
    }

    public Map<String, Object> sendMessage(String userId, String userName, String channel, String content) {
        return sendMessage(userId, userName, channel, content, null);
    }

    public Map<String, Object> sendMessage(String userId, String userName, String channel,
                                            String content, String targetId) {
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
        int serverId = extractServerId(userId);

        String nationId = null;
        String allianceId = null;

        switch (channel) {
            case "nation":
                nationId = getPlayerNation(userId);
                if (nationId == null || nationId.isEmpty()) throw new BusinessException(400, "尚未选择国家");
                break;
            case "alliance":
                allianceId = getPlayerAlliance(userId);
                if (allianceId == null || allianceId.isEmpty()) throw new BusinessException(400, "尚未加入联盟");
                break;
            case "private":
                if (targetId == null || targetId.isEmpty()) throw new BusinessException(400, "私聊目标不能为空");
                break;
        }

        chatMapper.insertMessageFull(userId, userName, channel, content.trim(),
                serverId, now, targetId, nationId, allianceId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("createTime", now);
        return result;
    }

    public List<Map<String, Object>> getRecent(String userId, String channel, int limit) {
        if (channel == null || channel.isEmpty()) channel = "world";
        if (limit <= 0 || limit > 100) limit = 30;
        int serverId = extractServerId(userId);

        List<Map<String, Object>> msgs;
        switch (channel) {
            case "nation": {
                String nationId = getPlayerNation(userId);
                msgs = chatMapper.findRecentByNation(serverId, nationId, limit);
                break;
            }
            case "alliance": {
                String allianceId = getPlayerAlliance(userId);
                msgs = chatMapper.findRecentByAlliance(serverId, allianceId, limit);
                break;
            }
            case "private": {
                msgs = chatMapper.findRecentPrivate(serverId, userId, limit);
                break;
            }
            default:
                msgs = chatMapper.findRecent(channel, serverId, limit);
                break;
        }
        normalizeSenderNames(msgs);
        Collections.reverse(msgs);
        return msgs;
    }

    public List<Map<String, Object>> poll(String userId, String channel, long sinceTime) {
        if (channel == null || channel.isEmpty()) channel = "world";
        int serverId = extractServerId(userId);

        switch (channel) {
            case "nation": {
                String nationId = getPlayerNation(userId);
                List<Map<String, Object>> result = chatMapper.pollByNation(serverId, nationId, sinceTime, 50);
                normalizeSenderNames(result);
                return result;
            }
            case "alliance": {
                String allianceId = getPlayerAlliance(userId);
                List<Map<String, Object>> result = chatMapper.pollByAlliance(serverId, allianceId, sinceTime, 50);
                normalizeSenderNames(result);
                return result;
            }
            case "private": {
                List<Map<String, Object>> result = chatMapper.pollPrivate(serverId, userId, sinceTime, 50);
                normalizeSenderNames(result);
                return result;
            }
            default: {
                List<Map<String, Object>> result = chatMapper.findSince(channel, serverId, sinceTime, 50);
                normalizeSenderNames(result);
                return result;
            }
        }
    }

    /**
     * 获取私聊对话（两个人之间）
     */
    public List<Map<String, Object>> getPrivateChat(String userId, String targetId, int limit) {
        int serverId = extractServerId(userId);
        List<Map<String, Object>> msgs = chatMapper.findPrivateConversation(serverId, userId, targetId, limit);
        normalizeSenderNames(msgs);
        Collections.reverse(msgs);
        return msgs;
    }

    /**
     * 获取私聊联系人列表（有过私聊的人）
     */
    public List<Map<String, Object>> getPrivateContacts(String userId) {
        int serverId = extractServerId(userId);
        List<Map<String, Object>> contacts = chatMapper.findPrivateContacts(serverId, userId, 20);
        if (contacts != null) {
            for (Map<String, Object> c : contacts) {
                String contactId = String.valueOf(c.get("contactId"));
                c.put("contactName", resolveDisplayName(contactId, String.valueOf(c.get("contactName"))));
            }
        }
        return contacts;
    }

    public void sendSystemMessage(String channel, String content) {
        sendSystemMessage(1, channel, content);
    }

    public void sendSystemMessage(int serverId, String channel, String content) {
        if (content == null || content.trim().isEmpty()) return;
        if (channel == null || channel.isEmpty()) channel = "world";
        chatMapper.insertMessageFull("SYSTEM", "系统公告", channel, content.trim(),
                serverId, System.currentTimeMillis(), null, null, null);
        logger.info("系统公告 serverId={}: {}", serverId, content);
    }

    public List<Map<String, Object>> getAnnouncements() {
        return chatMapper.findActiveAnnouncements(System.currentTimeMillis());
    }

    private String getPlayerNation(String userId) {
        try {
            return nationWarService.getPlayerNation(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private String getPlayerAlliance(String userId) {
        try {
            Object alliance = allianceService.getUserAlliance(userId);
            if (alliance != null) {
                java.lang.reflect.Method getId = alliance.getClass().getMethod("getId");
                Object id = getId.invoke(alliance);
                return id != null ? String.valueOf(id) : null;
            }
        } catch (Exception e) { }
        return null;
    }

    private void normalizeSenderNames(List<Map<String, Object>> msgs) {
        if (msgs == null) return;
        for (Map<String, Object> msg : msgs) {
            if (msg == null) continue;
            String senderId = String.valueOf(msg.get("senderId"));
            String senderName = String.valueOf(msg.get("senderName"));
            msg.put("senderName", resolveDisplayName(senderId, senderName));
        }
    }

    private String resolveDisplayName(String userId, String fallback) {
        if (userId == null || userId.isEmpty() || "SYSTEM".equals(userId) || userId.startsWith("NPC_")) {
            return fallback;
        }
        try {
            String name = playerNameResolver.resolve(userId);
            if (name != null && !name.isEmpty() && !"君主".equals(name)) return name;
        } catch (Exception ignore) { }
        return fallback;
    }
}
