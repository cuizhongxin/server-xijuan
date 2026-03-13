package com.tencent.wxcloudrun.controller.soldier;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserLevel;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.herorank.PeerageService;
import com.tencent.wxcloudrun.service.level.LevelService;
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

    @Autowired
    private LevelService levelService;

    // 兵种名称映射: troopType + soldierRank -> 名称
    private static final Map<String, String[]> SOLDIER_NAMES = new LinkedHashMap<>();
    static {
        SOLDIER_NAMES.put("步", new String[]{"民勇", "长枪兵", "重甲兵"});
        SOLDIER_NAMES.put("骑", new String[]{"轻骑", "铁骑", "近卫骑"});
        SOLDIER_NAMES.put("弓", new String[]{"弓手", "连弩兵", "神射手"});
    }

    // 编制扩编配置 (来自APK formation_cfg.json)
    private static final int[][] FORMATION_CFG = {
        // {maxPeople, needLv, needSilver, addAtt, addDef}
        {100,   1,    500,    230, 230},  // 队 lv1
        {200,   5,    800,    250, 250},  // 伙 lv2
        {300,  10,   2000,    270, 270},  // 哨 lv3
        {400,  20,   7000,    290, 290},  // 岗 lv4
        {500,  30,  15000,    310, 310},  // 都 lv5
        {600,  40,  30000,    330, 330},  // 营 lv6
        {700,  50, 100000,    350, 350},  // 团 lv7
        {800,  60, 200000,    370, 370},  // 师 lv8
        {900,  70, 300000,    390, 390},  // 旅 lv9
        {1000, 80, 500000,    410, 410},  // 军 lv10
    };
    private static final String[] FORMATION_NAMES = {"队","伙","哨","岗","都","营","团","师","旅","军"};

    // 兵种进阶白银消耗 (来自APK ArmyService.json, kingLevelNeed = tier)
    private static final long[] ARMY_SILVER = {
        500, 1000, 3000, 8000, 15000, 30000, 100000, 200000, 300000, 500000
    };

    // 兵种阶位对应爵位要求 (来自APK BanneretID.json + peerage_config)
    private static final String[] ARMY_PEERAGE = {
        "平民", "公士", "民爵", "勋爵", "男爵", "子爵", "伯爵", "侯爵", "公爵", "王"
    };

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
     * POST /soldier/rank/upgrade - 升级爵位 (保留兼容)
     */
    @PostMapping("/rank/upgrade")
    public ApiResponse<?> upgradeRank(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(peerageService.upgradeRank(userId));
    }

    /**
     * POST /soldier/upgrade-formation - 编制扩编 (基于君主等级, 参考APK formation_cfg)
     */
    @PostMapping("/upgrade-formation")
    public ApiResponse<?> upgradeFormation(@RequestBody Map<String, Object> body,
                                            HttpServletRequest request) {
        String userId = getUserId(request);
        String generalId = (String) body.get("generalId");

        General general = generalService.getGeneralById(generalId);
        if (general == null || !userId.equals(general.getUserId())) {
            return ApiResponse.error(400, "武将不存在");
        }

        int curMax = general.getSoldierMaxCount() != null ? general.getSoldierMaxCount() : 100;

        int curIdx = -1;
        for (int i = 0; i < FORMATION_CFG.length; i++) {
            if (FORMATION_CFG[i][0] == curMax) { curIdx = i; break; }
        }
        if (curIdx < 0) {
            for (int i = FORMATION_CFG.length - 1; i >= 0; i--) {
                if (curMax >= FORMATION_CFG[i][0]) { curIdx = i; break; }
            }
            if (curIdx < 0) curIdx = 0;
        }

        if (curIdx >= FORMATION_CFG.length - 1) {
            return ApiResponse.error(400, "已达最高编制（" + FORMATION_NAMES[FORMATION_CFG.length - 1] + "）");
        }

        int nextIdx = curIdx + 1;
        int needLv = FORMATION_CFG[nextIdx][1];
        long needSilver = FORMATION_CFG[nextIdx][2];
        int newMaxPeople = FORMATION_CFG[nextIdx][0];

        UserLevel userLevel = levelService.getUserLevel(userId);
        int kingLevel = userLevel != null && userLevel.getLevel() != null ? userLevel.getLevel() : 1;
        if (kingLevel < needLv) {
            return ApiResponse.error(400, "君主等级不足，需要" + needLv + "级（当前" + kingLevel + "级）");
        }

        if (!resourceService.consumeSilver(userId, needSilver)) {
            return ApiResponse.error(400, "白银不足，需要" + needSilver + "白银");
        }

        general.setSoldierMaxCount(newMaxPeople);
        general.setSoldierCount(newMaxPeople);
        general.setUpdateTime(System.currentTimeMillis());
        generalService.saveGeneral(general);

        logger.info("用户 {} 武将 {} 编制扩编: {} -> {}, 消耗白银 {}",
                userId, general.getName(), FORMATION_NAMES[curIdx], FORMATION_NAMES[nextIdx], needSilver);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("oldFormation", FORMATION_NAMES[curIdx]);
        result.put("newFormation", FORMATION_NAMES[nextIdx]);
        result.put("newMaxPeople", newMaxPeople);
        result.put("addAtt", FORMATION_CFG[nextIdx][3]);
        result.put("addDef", FORMATION_CFG[nextIdx][4]);
        result.put("cost", needSilver);
        return ApiResponse.success(result);
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
     * POST /soldier/upgrade-type - 升级兵种 (基于君主等级, 参考APK ArmyService.json)
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
        if (currentRank >= 10) {
            return ApiResponse.error(400, "已达最高兵种等级");
        }

        String troopType = general.getTroopType() != null ? general.getTroopType() : "步";
        int targetTier = currentRank + 1;

        int kingLvNeed = targetTier;
        long silverNeed = ARMY_SILVER[Math.min(targetTier - 1, ARMY_SILVER.length - 1)];
        String peerageNeed = ARMY_PEERAGE[Math.min(targetTier - 1, ARMY_PEERAGE.length - 1)];

        UserLevel userLevel = levelService.getUserLevel(userId);
        int kingLevel = userLevel != null && userLevel.getLevel() != null ? userLevel.getLevel() : 1;
        if (kingLevel < kingLvNeed) {
            return ApiResponse.error(400, "君主等级不足，需要" + kingLvNeed + "级（当前" + kingLevel + "级）");
        }

        UserResource res = resourceService.getUserResource(userId);
        String currentRankName = res != null && res.getRank() != null ? res.getRank() : "平民";
        int maxTier = peerageService.getMaxSoldierTier(currentRankName);
        if (targetTier > maxTier) {
            return ApiResponse.error(400, "爵位不足，需要爵位「" + peerageNeed + "」才能升级到" + targetTier + "阶兵种（当前爵位：" + currentRankName + "）");
        }

        if (!resourceService.consumeSilver(userId, silverNeed)) {
            return ApiResponse.error(400, "白银不足，需要" + silverNeed + "白银");
        }

        general.setSoldierRank(targetTier);
        general.setUpdateTime(System.currentTimeMillis());
        generalService.saveGeneral(general);

        logger.info("用户 {} 武将 {} 兵种进阶: {}阶 -> {}阶, 消耗白银 {}",
                userId, general.getName(), currentRank, targetTier, silverNeed);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("newTier", targetTier);
        result.put("troopType", troopType);
        result.put("cost", silverNeed);
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
