package com.tencent.wxcloudrun.controller.soldier;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.herorank.PeerageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 士兵控制器 - 管理将领的士兵招募、兵种升级等
 */
@RestController
@RequestMapping("/soldier")
public class SoldierController {

    private static final Logger logger = LoggerFactory.getLogger(SoldierController.class);

    @Autowired
    private GeneralService generalService;

    @Autowired
    private UserResourceService resourceService;

    @Autowired
    private PeerageService peerageService;

    // 兵种名称映射: troopType + soldierRank -> 名称
    private static final Map<String, String[]> SOLDIER_NAMES = new LinkedHashMap<>();
    static {
        SOLDIER_NAMES.put("步", new String[]{"民勇", "长枪兵", "重甲兵"});
        SOLDIER_NAMES.put("骑", new String[]{"轻骑", "铁骑", "近卫骑"});
        SOLDIER_NAMES.put("弓", new String[]{"弓手", "连弩兵", "神射手"});
    }

    /**
     * GET /soldier/rank - 获取玩家爵位信息
     */
    @GetMapping("/rank")
    public ApiResponse<?> getPlayerRank(HttpServletRequest request) {
        String userId = getUserId(request);
        Map<String, Object> info = peerageService.getPeerageInfo(userId);
        return ApiResponse.success(info);
    }

    /**
     * POST /soldier/rank/upgrade - 升级爵位
     */
    @PostMapping("/rank/upgrade")
    public ApiResponse<?> upgradeRank(HttpServletRequest request) {
        String userId = getUserId(request);
        // 爵位升级逻辑委托给PeerageService
        // 暂返回当前信息
        return ApiResponse.success(peerageService.getPeerageInfo(userId));
    }

    /**
     * GET /soldier/general/{generalId} - 获取将领的士兵信息
     */
    @GetMapping("/general/{generalId}")
    public ApiResponse<?> getGeneralSoldier(@PathVariable String generalId,
                                             HttpServletRequest request) {
        String userId = getUserId(request);
        General general = generalService.getGeneralById(generalId);
        if (general == null || !userId.equals(general.getUserId())) {
            return ApiResponse.error(400, "武将不存在");
        }

        String troopType = general.getTroopType() != null ? general.getTroopType() : "步";
        int rank = general.getSoldierRank() != null ? general.getSoldierRank() : 1;
        int count = general.getSoldierCount() != null ? general.getSoldierCount() : 0;
        int maxCount = general.getSoldierMaxCount() != null ? general.getSoldierMaxCount() : 100;

        String[] names = SOLDIER_NAMES.getOrDefault(troopType, SOLDIER_NAMES.get("步"));
        String typeName = rank <= names.length ? names[rank - 1] : names[names.length - 1];
        String nextTypeName = rank < names.length ? names[rank] : null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generalId", generalId);
        result.put("troopType", troopType);
        result.put("soldierTypeName", typeName);
        result.put("soldierHp", 100 + (rank - 1) * 50);
        result.put("count", count);
        result.put("maxCount", maxCount);
        result.put("upgradeLevel", rank - 1);
        result.put("nextTypeName", nextTypeName);
        return ApiResponse.success(result);
    }

    /**
     * GET /soldier/types - 获取可用兵种列表
     */
    @GetMapping("/types")
    public ApiResponse<?> getSoldierTypes(@RequestParam String generalId,
                                           HttpServletRequest request) {
        String userId = getUserId(request);
        General general = generalService.getGeneralById(generalId);
        if (general == null || !userId.equals(general.getUserId())) {
            return ApiResponse.error(400, "武将不存在");
        }

        String troopType = general.getTroopType() != null ? general.getTroopType() : "步";
        String[] names = SOLDIER_NAMES.getOrDefault(troopType, SOLDIER_NAMES.get("步"));
        int currentRank = general.getSoldierRank() != null ? general.getSoldierRank() : 1;

        List<Map<String, Object>> types = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("tier", i + 1);
            t.put("name", names[i]);
            t.put("troopCategory", troopType);
            t.put("hp", 100 + i * 50);
            t.put("unlocked", i + 1 <= currentRank);
            t.put("current", i + 1 == currentRank);
            types.add(t);
        }
        return ApiResponse.success(types);
    }

    /**
     * POST /soldier/recruit - 招募士兵
     */
    @PostMapping("/recruit")
    public ApiResponse<?> recruitSoldier(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        String userId = getUserId(request);
        String generalId = (String) body.get("generalId");
        int count = body.get("count") instanceof Number ? ((Number) body.get("count")).intValue() : 10;

        General general = generalService.getGeneralById(generalId);
        if (general == null || !userId.equals(general.getUserId())) {
            return ApiResponse.error(400, "武将不存在");
        }

        int maxCount = general.getSoldierMaxCount() != null ? general.getSoldierMaxCount() : 100;
        int current = general.getSoldierCount() != null ? general.getSoldierCount() : 0;
        int canRecruit = Math.min(count, maxCount - current);
        if (canRecruit <= 0) {
            return ApiResponse.error(400, "士兵已满");
        }

        // 消耗银两: 每个士兵10银两
        long cost = canRecruit * 10L;
        if (!resourceService.consumeSilver(userId, cost)) {
            return ApiResponse.error(400, "银两不足，需要" + cost + "银两");
        }

        general.setSoldierCount(current + canRecruit);
        general.setUpdateTime(System.currentTimeMillis());
        generalService.saveGeneral(general);

        logger.info("用户 {} 武将 {} 招募 {} 名士兵, 消耗银两 {}", userId, general.getName(), canRecruit, cost);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recruited", canRecruit);
        result.put("currentCount", general.getSoldierCount());
        result.put("maxCount", maxCount);
        result.put("cost", cost);
        return ApiResponse.success(result);
    }

    /**
     * POST /soldier/upgrade-type - 升级兵种
     */
    @PostMapping("/upgrade-type")
    public ApiResponse<?> upgradeSoldierType(@RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        String userId = getUserId(request);
        String generalId = (String) body.get("generalId");

        General general = generalService.getGeneralById(generalId);
        if (general == null || !userId.equals(general.getUserId())) {
            return ApiResponse.error(400, "武将不存在");
        }

        int currentRank = general.getSoldierRank() != null ? general.getSoldierRank() : 1;
        if (currentRank >= 3) {
            return ApiResponse.error(400, "已达最高兵种等级");
        }

        String troopType = general.getTroopType() != null ? general.getTroopType() : "步";
        int targetTier = currentRank + 1;

        // 委托给PeerageService处理
        Map<String, Object> result = peerageService.upgradeSoldierTier(userId, generalId, targetTier, troopType);
        return ApiResponse.success(result);
    }

    /**
     * GET /soldier/overview - 士兵总览
     */
    @GetMapping("/overview")
    public ApiResponse<?> getSoldierOverview(HttpServletRequest request) {
        String userId = getUserId(request);
        List<General> generals = generalService.getUserGenerals(userId);

        int totalSoldiers = 0;
        List<Map<String, Object>> list = new ArrayList<>();
        for (General g : generals) {
            int count = g.getSoldierCount() != null ? g.getSoldierCount() : 0;
            totalSoldiers += count;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("generalId", g.getId());
            m.put("generalName", g.getName());
            m.put("troopType", g.getTroopType());
            m.put("soldierCount", count);
            m.put("soldierMax", g.getSoldierMaxCount());
            m.put("soldierRank", g.getSoldierRank());
            list.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSoldiers", totalSoldiers);
        result.put("generals", list);
        return ApiResponse.success(result);
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
}
