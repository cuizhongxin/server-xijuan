package com.tencent.wxcloudrun.controller.server;

import com.tencent.wxcloudrun.dao.ChatMapper;
import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/server")
public class GameServerController {

    @Autowired
    private GameServerMapper serverMapper;

    @Autowired
    private ChatMapper chatMapper;

    /**
     * 获取区服列表 + 公告 + 玩家已有角色信息
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(HttpServletRequest request) {
        String userId = getUserId(request);

        List<Map<String, Object>> servers = serverMapper.findAllServers();
        List<Map<String, Object>> playerServers = userId != null
                ? serverMapper.findPlayerServers(userId) : Collections.emptyList();
        List<Map<String, Object>> announcements = chatMapper.findActiveAnnouncements(System.currentTimeMillis());

        Set<Integer> joinedIds = new HashSet<>();
        Map<Integer, String> nameMap = new HashMap<>();
        for (Map<String, Object> ps : playerServers) {
            int sid = ((Number) ps.get("serverId")).intValue();
            joinedIds.add(sid);
            nameMap.put(sid, (String) ps.get("lordName"));
        }
        for (Map<String, Object> s : servers) {
            int sid = ((Number) s.get("id")).intValue();
            s.put("hasRole", joinedIds.contains(sid));
            s.put("lordName", nameMap.getOrDefault(sid, ""));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("servers", servers);
        result.put("announcements", announcements);
        return ApiResponse.success(result);
    }

    /**
     * 进入区服 — 老玩家直接进入，新玩家返回 needCreate=true 让前端弹窗起名
     */
    @PostMapping("/enter")
    public ApiResponse<Map<String, Object>> enter(HttpServletRequest request,
                                                    @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        int serverId = body.get("serverId") instanceof Number ? ((Number) body.get("serverId")).intValue() : 0;

        Map<String, Object> server = serverMapper.findServerById(serverId);
        if (server == null) return ApiResponse.error(400, "区服不存在");
        if ("maintenance".equals(server.get("serverStatus")))
            return ApiResponse.error(400, "该区服正在维护中");

        Map<String, Object> playerServer = serverMapper.findPlayerServer(userId, serverId);

        Map<String, Object> result = new HashMap<>();
        result.put("serverId", serverId);
        result.put("serverName", server.get("serverName"));

        if (playerServer == null) {
            // 新玩家，告诉前端需要创建角色（弹窗起名）
            result.put("needCreate", true);
            return ApiResponse.success(result);
        }

        // 老玩家，直接进入
        serverMapper.updatePlayerLogin(userId, serverId, System.currentTimeMillis());
        result.put("needCreate", false);
        result.put("lordName", playerServer.get("lordName"));
        return ApiResponse.success(result);
    }

    /**
     * 创建角色（起名后调用）
     */
    @PostMapping("/create-role")
    public ApiResponse<Map<String, Object>> createRole(HttpServletRequest request,
                                                        @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        int serverId = body.get("serverId") instanceof Number ? ((Number) body.get("serverId")).intValue() : 0;
        String lordName = body.get("lordName") != null ? body.get("lordName").toString().trim() : null;

        // 校验区服
        Map<String, Object> server = serverMapper.findServerById(serverId);
        if (server == null) return ApiResponse.error(400, "区服不存在");

        // 校验是否已有角色
        if (serverMapper.findPlayerServer(userId, serverId) != null) {
            return ApiResponse.error(400, "您在该区服已有角色");
        }

        // 校验名字格式
        String validated = validateName(lordName);
        if (validated == null) {
            return ApiResponse.error(400, "名字需要2-8个字符，只能包含中文、字母、数字、下划线");
        }

        // 同区名字唯一性检查
        if (serverMapper.isNameTaken(serverId, validated) > 0) {
            return ApiResponse.error(400, "该名字已被使用，请换一个");
        }

        serverMapper.insertPlayerServer(userId, serverId, validated, System.currentTimeMillis());
        serverMapper.incrementServerPlayers(serverId);

        Map<String, Object> result = new HashMap<>();
        result.put("serverId", serverId);
        result.put("serverName", server.get("serverName"));
        result.put("lordName", validated);
        return ApiResponse.success(result);
    }

    /**
     * 检查名字是否可用
     */
    @GetMapping("/check-name")
    public ApiResponse<Map<String, Object>> checkName(
            @RequestParam int serverId,
            @RequestParam String lordName) {
        String validated = validateName(lordName);
        if (validated == null) {
            return ApiResponse.error(400, "名字需要2-8个字符，只能包含中文、字母、数字、下划线");
        }
        boolean taken = serverMapper.isNameTaken(serverId, validated) > 0;
        Map<String, Object> result = new HashMap<>();
        result.put("available", !taken);
        result.put("lordName", validated);
        return ApiResponse.success(result);
    }

    /**
     * 校验名字：2-8个字符，只允许中文、字母、数字、下划线
     */
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 8) return null;
        if (!trimmed.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$")) return null;
        return trimmed;
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("rawUserId"));
    }
}
