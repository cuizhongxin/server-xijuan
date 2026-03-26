package com.tencent.wxcloudrun.controller.chat;

import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired private ChatService chatService;
    @Autowired private GameServerMapper gameServerMapper;

    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> send(HttpServletRequest request,
                                                  @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String channel = (String) body.getOrDefault("channel", "world");
            String content = (String) body.get("content");
            String targetId = (String) body.get("targetId");
            String userName = getLordName(request, userId);
            return ApiResponse.success(chatService.sendMessage(userId, userName, channel, content, targetId));
        } catch (Exception e) {
            logger.error("发送消息失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/recent")
    public ApiResponse<List<Map<String, Object>>> recent(
            HttpServletRequest request,
            @RequestParam(defaultValue = "world") String channel,
            @RequestParam(defaultValue = "30") int limit) {
        try {
            return ApiResponse.success(chatService.getRecent(getUserId(request), channel, limit));
        } catch (Exception e) {
            logger.error("获取聊天记录失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/poll")
    public ApiResponse<List<Map<String, Object>>> poll(
            HttpServletRequest request,
            @RequestParam(defaultValue = "world") String channel,
            @RequestParam(defaultValue = "0") long since) {
        try {
            return ApiResponse.success(chatService.poll(getUserId(request), channel, since));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/private-chat")
    public ApiResponse<List<Map<String, Object>>> privateChat(
            HttpServletRequest request,
            @RequestParam String targetId,
            @RequestParam(defaultValue = "30") int limit) {
        try {
            return ApiResponse.success(chatService.getPrivateChat(getUserId(request), targetId, limit));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/private-contacts")
    public ApiResponse<List<Map<String, Object>>> privateContacts(HttpServletRequest request) {
        try {
            return ApiResponse.success(chatService.getPrivateContacts(getUserId(request)));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Map<String, Object>>> announcements() {
        return ApiResponse.success(chatService.getAnnouncements());
    }

    private String getLordName(HttpServletRequest request, String compositeUserId) {
        Object rawId = request.getAttribute("rawUserId");
        String userId = rawId != null ? String.valueOf(rawId) : compositeUserId;
        Object sidObj = request.getAttribute("serverId");
        if (sidObj != null) {
            try {
                int serverId = Integer.parseInt(String.valueOf(sidObj));
                Map<String, Object> ps = gameServerMapper.findPlayerServer(userId, serverId);
                if (ps != null && ps.get("lordName") != null) return (String) ps.get("lordName");
            } catch (Exception e) { logger.error("获取主公名称异常", e); }
        }
        List<Map<String, Object>> servers = gameServerMapper.findPlayerServers(userId);
        if (servers != null && !servers.isEmpty()) {
            Object name = servers.get(0).get("lordName");
            if (name != null) return (String) name;
        }
        return "玩家";
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
}
