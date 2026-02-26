package com.tencent.wxcloudrun.controller.chat;

import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.chat.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private GameServerMapper gameServerMapper;

    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> send(HttpServletRequest request,
                                                  @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        String channel = (String) body.getOrDefault("channel", "world");
        String content = (String) body.get("content");

        String userName = getLordName(request, userId);
        return ApiResponse.success(chatService.sendMessage(userId, userName, channel, content));
    }

    @GetMapping("/recent")
    public ApiResponse<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "world") String channel,
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.success(chatService.getRecent(channel, limit));
    }

    @GetMapping("/poll")
    public ApiResponse<List<Map<String, Object>>> poll(
            @RequestParam(defaultValue = "world") String channel,
            @RequestParam(defaultValue = "0") long since) {
        return ApiResponse.success(chatService.poll(channel, since));
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Map<String, Object>>> announcements() {
        return ApiResponse.success(chatService.getAnnouncements());
    }

    private String getLordName(HttpServletRequest request, String compositeUserId) {
        // 用原始userId查询，不是复合ID
        Object rawId = request.getAttribute("rawUserId");
        String userId = rawId != null ? String.valueOf(rawId) : compositeUserId;
        
        Object sidObj = request.getAttribute("serverId");
        if (sidObj != null) {
            try {
                int serverId = Integer.parseInt(String.valueOf(sidObj));
                Map<String, Object> ps = gameServerMapper.findPlayerServer(userId, serverId);
                if (ps != null && ps.get("lordName") != null) {
                    return (String) ps.get("lordName");
                }
            } catch (Exception ignored) {}
        }
        // fallback: 查该用户最近的区服
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
