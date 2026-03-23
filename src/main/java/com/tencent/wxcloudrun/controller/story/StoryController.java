package com.tencent.wxcloudrun.controller.story;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.story.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/story")
public class StoryController {

    @Autowired
    private StoryService storyService;

    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> getProgress(HttpServletRequest request) {
        String userId = getUserId(request);
        int serverId = getServerId(request);
        return ApiResponse.success(storyService.getProgress(userId, serverId));
    }

    @PostMapping("/advance")
    public ApiResponse<Map<String, Object>> advance(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        String userId = getUserId(request);
        int serverId = getServerId(request);
        int nodeId = 0;
        Object nodeObj = body.get("nodeId");
        if (nodeObj instanceof Number) {
            nodeId = ((Number) nodeObj).intValue();
        } else if (nodeObj != null) {
            nodeId = Integer.parseInt(String.valueOf(nodeObj));
        }
        return ApiResponse.success(storyService.advance(userId, serverId, nodeId));
    }

    @PostMapping("/complete")
    public ApiResponse<Map<String, Object>> complete(HttpServletRequest request) {
        String userId = getUserId(request);
        int serverId = getServerId(request);
        return ApiResponse.success(storyService.complete(userId, serverId));
    }

    @PostMapping("/skip")
    public ApiResponse<Map<String, Object>> skip(HttpServletRequest request) {
        String userId = getUserId(request);
        int serverId = getServerId(request);
        return ApiResponse.success(storyService.skip(userId, serverId));
    }

    private String getUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("rawUserId");
        return raw != null ? String.valueOf(raw) : String.valueOf(request.getAttribute("userId"));
    }

    private int getServerId(HttpServletRequest request) {
        Object sidObj = request.getAttribute("serverId");
        if (sidObj instanceof Number) return ((Number) sidObj).intValue();
        try { return Integer.parseInt(String.valueOf(sidObj)); } catch (Exception e) { return 1; }
    }
}
