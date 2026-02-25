package com.tencent.wxcloudrun.controller.chat;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
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
    private UserResourceService resourceService;

    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> send(HttpServletRequest request,
                                                  @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        String channel = (String) body.getOrDefault("channel", "world");
        String content = (String) body.get("content");

        UserResource res = resourceService.getUserResource(userId);
        String userName = "Lv." + (res != null && res.getLevel() != null ? res.getLevel() : 1) + "主公";

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

    private String getUserId(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        return userIdLong != null ? String.valueOf(userIdLong) : null;
    }
}
