package com.tencent.wxcloudrun.controller.server;

import com.tencent.wxcloudrun.dao.ChatMapper;
import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.alliance.AllianceBossService;
import com.tencent.wxcloudrun.service.herorank.HeroRankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/server")
public class GameServerController {

    private static final Logger logger = LoggerFactory.getLogger(GameServerController.class);

    @Autowired
    private GameServerMapper serverMapper;

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private GeneralService generalService;

    @Autowired
    private FormationService formationService;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private HeroRankService heroRankService;

    @Autowired
    private AllianceBossService allianceBossService;

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

        Map<Integer, Map<String, Object>> playerMap = new HashMap<>();
        for (Map<String, Object> ps : playerServers) {
            int sid = ((Number) ps.get("serverId")).intValue();
            playerMap.put(sid, ps);
        }
        for (Map<String, Object> s : servers) {
            int sid = ((Number) s.get("id")).intValue();
            Map<String, Object> ps = playerMap.get(sid);
            s.put("hasRole", ps != null);
            s.put("lordName", ps != null ? ps.getOrDefault("lordName", "") : "");
            s.put("roleLevel", ps != null ? ((Number) ps.getOrDefault("roleLevel", 1)).intValue() : 0);
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

        String gameUserId = userId + "_" + serverId;

        // 初始化该区服的用户资源（金币、银两等）
        try {
            userResourceService.getUserResource(gameUserId);
            logger.info("用户资源初始化完成, gameUserId={}", gameUserId);
        } catch (Exception e) {
            logger.error("初始化用户资源失败 gameUserId={}", gameUserId, e);
        }

        // 初始化该区服的英雄榜NPC（首个玩家进入时触发）
        try {
            heroRankService.ensureNpcExists(serverId);
        } catch (Exception e) {
            logger.error("初始化英雄榜NPC失败 serverId={}", serverId, e);
        }

        // 初始化该区服的联盟Boss（首个玩家进入时触发）
        try {
            allianceBossService.ensureBossExists(serverId);
        } catch (Exception e) {
            logger.error("初始化联盟Boss失败 serverId={}", serverId, e);
        }

        // 赠送初始武将并上阵
        String starterName = null;
        try {
            logger.info("开始为新玩家 {} 创建初始武将...", gameUserId);
            General starter = generalService.grantStarterGeneral(gameUserId);
            if (starter == null || starter.getId() == null) {
                logger.error("grantStarterGeneral 返回 null, userId={}", gameUserId);
            } else {
                starterName = starter.getName();
                logger.info("初始武将已创建: id={}, name={}, 准备设置阵型", starter.getId(), starterName);
                formationService.setFormation(gameUserId, Collections.singletonList(starter.getId()));
                logger.info("新玩家 {} 初始武将 {} 已创建并上阵", gameUserId, starterName);
            }
        } catch (Exception e) {
            logger.error("创建初始武将失败 userId={}", gameUserId, e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("serverId", serverId);
        result.put("serverName", server.get("serverName"));
        result.put("lordName", validated);
        result.put("starterGeneral", starterName);
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

    // ==================== 管理员接口（仅 userId=1） ====================

    /**
     * 创建新区服（仅管理员 userId=1 可操作）
     */
    @PostMapping("/admin/create")
    public ApiResponse<Map<String, Object>> adminCreateServer(HttpServletRequest request,
                                                               @RequestBody Map<String, Object> body) {
        String rawUserId = getUserId(request);
        if (!"1".equals(rawUserId)) {
            return ApiResponse.error(403, "无权操作");
        }

        String serverName = body.get("serverName") != null ? body.get("serverName").toString().trim() : null;
        if (serverName == null || serverName.isEmpty()) {
            return ApiResponse.error(400, "区服名称不能为空");
        }

        int maxPlayers = 10000;
        if (body.get("maxPlayers") instanceof Number) {
            maxPlayers = ((Number) body.get("maxPlayers")).intValue();
        }

        long now = System.currentTimeMillis();
        serverMapper.insertServer(serverName, "normal", now, maxPlayers);

        // 查询刚创建的区服ID
        List<Map<String, Object>> all = serverMapper.findAllServers();
        int newServerId = 0;
        for (Map<String, Object> s : all) {
            int sid = ((Number) s.get("id")).intValue();
            if (serverName.equals(s.get("serverName"))) newServerId = sid;
        }

        // 为新区服初始化英雄榜NPC + 联盟Boss
        if (newServerId > 0) {
            try {
                heroRankService.ensureNpcExists(newServerId);
                logger.info("新区服 {} (id={}) 英雄榜NPC初始化完成", serverName, newServerId);
            } catch (Exception e) {
                logger.error("新区服英雄榜NPC初始化失败 serverId={}", newServerId, e);
            }
            try {
                allianceBossService.ensureBossExists(newServerId);
                logger.info("新区服 {} (id={}) 联盟Boss初始化完成", serverName, newServerId);
            } catch (Exception e) {
                logger.error("新区服联盟Boss初始化失败 serverId={}", newServerId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("serverId", newServerId);
        result.put("serverName", serverName);
        result.put("status", "normal");
        return ApiResponse.success(result);
    }

    /**
     * 获取区服管理列表（仅管理员 userId=1）
     */
    @GetMapping("/admin/list")
    public ApiResponse<List<Map<String, Object>>> adminListServers(HttpServletRequest request) {
        String rawUserId = getUserId(request);
        if (!"1".equals(rawUserId)) {
            return ApiResponse.error(403, "无权操作");
        }
        return ApiResponse.success(serverMapper.findAllServers());
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("rawUserId"));
    }
}
