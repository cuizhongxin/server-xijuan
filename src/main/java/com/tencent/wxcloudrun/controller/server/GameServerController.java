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

        // 标记玩家已有角色的服务器
        Set<Integer> joinedIds = new HashSet<>();
        Map<Integer, Integer> levelMap = new HashMap<>();
        for (Map<String, Object> ps : playerServers) {
            int sid = ((Number) ps.get("serverId")).intValue();
            joinedIds.add(sid);
            levelMap.put(sid, ((Number) ps.getOrDefault("playerLevel", 1)).intValue());
        }
        for (Map<String, Object> s : servers) {
            int sid = ((Number) s.get("id")).intValue();
            s.put("hasRole", joinedIds.contains(sid));
            s.put("roleLevel", levelMap.getOrDefault(sid, 0));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("servers", servers);
        result.put("announcements", announcements);
        return ApiResponse.success(result);
    }

    /**
     * 选择/进入区服
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
        boolean isNew = playerServer == null;
        if (isNew) {
            serverMapper.insertPlayerServer(userId, serverId, System.currentTimeMillis());
            serverMapper.incrementServerPlayers(serverId);
        } else {
            int level = ((Number) playerServer.getOrDefault("playerLevel", 1)).intValue();
            serverMapper.updatePlayerLogin(userId, serverId, level, System.currentTimeMillis());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("serverId", serverId);
        result.put("serverName", server.get("serverName"));
        result.put("isNew", isNew);
        return ApiResponse.success(result);
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("rawUserId"));
    }
}
