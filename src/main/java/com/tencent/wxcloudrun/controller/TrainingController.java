package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.UserLevel;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.level.LevelService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 训练系统 — 基于APK ExpPill_cfg.json 的四级军事演习
 *
 * 日常演练: playerExp 1000, genExp  5000, 白银200,  粮食 500,  需等级5
 * 战术演练: playerExp 2000, genExp 15000, 白银300,  粮食1000,  需等级20
 * 兵棋推演: playerExp 4000, genExp 35000, 白银550,  粮食2000,  需等级40
 * 实战演习: playerExp 8000, genExp 60000, 白银850,  粮食3000,  需等级60
 */
@RestController
@RequestMapping("/training")
public class TrainingController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingController.class);

    // [白银消耗, 粮食消耗, 君主经验, 将领经验, 需要等级]
    private static final Map<String, long[]> EXERCISE_CONFIG = new LinkedHashMap<>();
    static {
        EXERCISE_CONFIG.put("1", new long[]{200,   500,   1000,   5000,  5});
        EXERCISE_CONFIG.put("2", new long[]{300,  1000,   2000,  15000, 20});
        EXERCISE_CONFIG.put("3", new long[]{550,  2000,   4000,  35000, 40});
        EXERCISE_CONFIG.put("4", new long[]{850,  3000,   8000,  60000, 60});
    }

    private static final String[] EXERCISE_NAMES = {"日常演练", "战术演练", "兵棋推演", "实战演习"};
    private static final String[] EXERCISE_PICS  = {"exercisePic1.jpg", "exercisePic2.jpg", "exercisePic3.jpg", "exercisePic4.jpg"};
    private static final String[] EXERCISE_COLORS = {"#55ff55", "#5599ff", "#ff9933", "#ff4444"};

    private static final String SPECIAL_TRAIN_ITEM_ID = "15042";
    private static final long SPECIAL_TRAIN_EXP = 10000;

    @Autowired
    private UserResourceService userResourceService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private GeneralService generalService;
    @Autowired
    private WarehouseService warehouseService;

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));

        int playerLevel = 1;
        try {
            UserLevel ul = levelService.getUserLevel(userId);
            if (ul != null) playerLevel = ul.getLevel();
        } catch (Exception ignored) {}

        List<Map<String, Object>> exercises = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String id = String.valueOf(i + 1);
            long[] cfg = EXERCISE_CONFIG.get(id);
            Map<String, Object> ex = new LinkedHashMap<>();
            ex.put("id", id);
            ex.put("name", EXERCISE_NAMES[i]);
            ex.put("pic", EXERCISE_PICS[i]);
            ex.put("color", EXERCISE_COLORS[i]);
            ex.put("silverCost", cfg[0]);
            ex.put("foodCost", cfg[1]);
            ex.put("playerExp", cfg[2]);
            ex.put("genExp", cfg[3]);
            ex.put("needLevel", cfg[4]);
            ex.put("unlocked", playerLevel >= cfg[4]);
            exercises.add(ex);
        }

        UserResource resource = userResourceService.getUserResource(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("exercises", exercises);
        result.put("food", resource.getFood() != null ? resource.getFood() : 0);
        result.put("silver", resource.getSilver() != null ? resource.getSilver() : 0);
        result.put("playerLevel", playerLevel);
        return ApiResponse.success(result);
    }

    @PostMapping("/exercise")
    public ApiResponse<Map<String, Object>> exercise(@RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String exerciseId = String.valueOf(body.getOrDefault("exerciseId", "1"));
        String generalId = body.get("generalId") != null ? body.get("generalId").toString() : null;

        long[] cfg = EXERCISE_CONFIG.get(exerciseId);
        if (cfg == null) return ApiResponse.error(400, "无效的演习等级");

        int playerLevel = 1;
        try {
            UserLevel ul = levelService.getUserLevel(userId);
            if (ul != null) playerLevel = ul.getLevel();
        } catch (Exception ignored) {}

        if (playerLevel < cfg[4]) {
            return ApiResponse.error(400, "需要君主等级" + cfg[4] + "，当前等级" + playerLevel);
        }

        long needSilver = cfg[0];
        long needFood = cfg[1];

        UserResource resource = userResourceService.getUserResource(userId);
        long curSilver = resource.getSilver() != null ? resource.getSilver() : 0;
        long curFood = resource.getFood() != null ? resource.getFood() : 0;

        if (curSilver < needSilver) return ApiResponse.error(400, "白银不足，需要" + needSilver);
        if (curFood < needFood) return ApiResponse.error(400, "粮食不足，需要" + needFood);

        resource.setSilver(curSilver - needSilver);
        resource.setFood(curFood - needFood);
        userResourceService.saveResource(resource);

        long playerExp = cfg[2];
        long genExp = cfg[3];
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> lvlResult = levelService.addExp(userId, playerExp, "军事演习");
            if (lvlResult != null) {
                result.put("levelUp", Boolean.TRUE.equals(lvlResult.get("levelUp")));
                result.put("newLevel", lvlResult.get("newLevel"));
            }
        } catch (Exception e) {
            logger.warn("增加君主经验失败: {}", e.getMessage());
        }

        if (generalId != null) {
            try {
                Map<String, Object> genResult = generalService.addGeneralExp(generalId, genExp);
                if (genResult != null) {
                    result.put("generalLevel", genResult.get("newLevel"));
                    result.put("generalExp", genResult.get("currentExp"));
                    result.put("generalMaxExp", genResult.get("maxExp"));
                }
            } catch (Exception e) {
                logger.warn("增加将领经验失败: {}", e.getMessage());
            }
        }

        result.put("playerExpGained", playerExp);
        result.put("genExpGained", genExp);
        result.put("silverConsumed", needSilver);
        result.put("foodConsumed", needFood);
        result.put("remainingSilver", resource.getSilver());
        result.put("remainingFood", resource.getFood());

        int idx = Integer.parseInt(exerciseId) - 1;
        logger.info("用户 {} 执行{}，消耗白银{}粮食{}，将领经验+{}，君主经验+{}",
                userId, EXERCISE_NAMES[idx], needSilver, needFood, genExp, playerExp);
        return ApiResponse.success(result);
    }

    /** 保留旧接口兼容 */
    @PostMapping("/train")
    public ApiResponse<Map<String, Object>> train(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        body.putIfAbsent("exerciseId", "1");
        return exercise(body, request);
    }

    // ==================== 将领训练 (genTrain.json: 1h/8h/24h 挂机) ====================

    private static final long[][] TRAIN_TIERS = {
        {1, 0, 0},     // 1h, 免费
        {8, 0, 500},   // 8h, 500白银
        {24, 5, 0}     // 24h, 5黄金
    };
    private static final long TRAIN_EXP_PER_HOUR = 800;

    private static final Map<String, long[]> trainingSlots = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, String> trainingGeneralIds = new java.util.concurrent.ConcurrentHashMap<>();

    @GetMapping("/train-status")
    public ApiResponse<Map<String, Object>> trainStatus(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String key = userId + "_train";
        long[] slot = trainingSlots.get(key);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (int i = 0; i < TRAIN_TIERS.length; i++) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", i + 1);
            t.put("hours", TRAIN_TIERS[i][0]);
            t.put("gold", TRAIN_TIERS[i][1]);
            t.put("silver", TRAIN_TIERS[i][2]);
            t.put("label", TRAIN_TIERS[i][0] + "小时" +
                (TRAIN_TIERS[i][1] > 0 ? " " + TRAIN_TIERS[i][1] + "金" :
                 TRAIN_TIERS[i][2] > 0 ? " " + TRAIN_TIERS[i][2] + "银" : " 免费"));
            tiers.add(t);
        }
        result.put("tiers", tiers);
        result.put("expPerHour", TRAIN_EXP_PER_HOUR);
        result.put("specialTrainTokens", getSpecialTrainTokenCount(userId));
        result.put("specialTrainExp", SPECIAL_TRAIN_EXP);

        if (slot != null) {
            long startMs = slot[0];
            long durationMs = slot[1];
            long elapsed = System.currentTimeMillis() - startMs;
            boolean finished = elapsed >= durationMs;
            long expGain = (long)(TRAIN_EXP_PER_HOUR * (slot[1] / 3600000.0));
            result.put("training", true);
            result.put("generalId", trainingGeneralIds.getOrDefault(key, ""));
            result.put("generalName", "");
            result.put("startTime", startMs);
            result.put("duration", durationMs);
            result.put("elapsed", Math.min(elapsed, durationMs));
            result.put("finished", finished);
            result.put("expGain", expGain);
        } else {
            result.put("training", false);
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/start-train")
    public ApiResponse<Map<String, Object>> startTrain(@RequestBody Map<String, Object> body,
                                                        HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String generalId = body.get("generalId") != null ? body.get("generalId").toString() : null;
        int tier = body.get("tier") != null ? Integer.parseInt(body.get("tier").toString()) : 1;

        if (generalId == null) return ApiResponse.error(400, "请选择要训练的武将");
        if (tier < 1 || tier > 3) return ApiResponse.error(400, "无效的训练档位");

        String key = userId + "_train";
        if (trainingSlots.containsKey(key)) return ApiResponse.error(400, "已有将领在训练中");

        long[] t = TRAIN_TIERS[tier - 1];
        long goldCost = t[1], silverCost = t[2];

        UserResource resource = userResourceService.getUserResource(userId);
        if (goldCost > 0) {
            long cur = resource.getGold() != null ? resource.getGold() : 0;
            if (cur < goldCost) return ApiResponse.error(400, "黄金不足");
            resource.setGold(cur - goldCost);
        }
        if (silverCost > 0) {
            long cur = resource.getSilver() != null ? resource.getSilver() : 0;
            if (cur < silverCost) return ApiResponse.error(400, "白银不足");
            resource.setSilver(cur - silverCost);
        }
        userResourceService.saveResource(resource);

        long durationMs = t[0] * 3600000L;
        trainingSlots.put(key, new long[]{System.currentTimeMillis(), durationMs});
        trainingGeneralIds.put(key, generalId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("duration", durationMs);
        result.put("expGain", (long)(TRAIN_EXP_PER_HOUR * t[0]));
        logger.info("用户 {} 开始训练武将 {}，时长 {}h", userId, generalId, t[0]);
        return ApiResponse.success(result);
    }

    @PostMapping("/end-train")
    public ApiResponse<Map<String, Object>> endTrain(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String key = userId + "_train";
        long[] slot = trainingSlots.get(key);

        if (slot == null) return ApiResponse.error(400, "没有将领在训练中");

        long elapsed = System.currentTimeMillis() - slot[0];
        long durationMs = slot[1];
        if (elapsed < durationMs) return ApiResponse.error(400, "训练尚未完成");

        long expGain = (long)(TRAIN_EXP_PER_HOUR * (durationMs / 3600000.0));
        String generalId = trainingGeneralIds.getOrDefault(key, "");

        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> genResult = generalService.addGeneralExp(generalId, expGain);
            if (genResult != null) {
                result.put("generalLevel", genResult.get("newLevel"));
                result.put("generalExp", genResult.get("currentExp"));
            }
        } catch (Exception e) {
            logger.warn("训练结算经验失败: {}", e.getMessage());
        }

        trainingSlots.remove(key);
        trainingGeneralIds.remove(key);
        result.put("success", true);
        result.put("expGained", expGain);
        logger.info("用户 {} 完成训练武将 {}，获得经验 {}", userId, generalId, expGain);
        return ApiResponse.success(result);
    }

    @PostMapping("/use-special-train")
    public ApiResponse<Map<String, Object>> useSpecialTrain(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String key = userId + "_train";
        long[] slot = trainingSlots.get(key);

        if (slot == null) return ApiResponse.error(400, "没有将领在训练中，无法使用特训符");

        String generalId = trainingGeneralIds.getOrDefault(key, "");
        if (generalId.isEmpty()) return ApiResponse.error(400, "训练数据异常");

        int tokenCount = getSpecialTrainTokenCount(userId);
        if (tokenCount <= 0) return ApiResponse.error(400, "特训符不足");

        try {
            warehouseService.removeItem(userId, SPECIAL_TRAIN_ITEM_ID, 1);
        } catch (Exception e) {
            return ApiResponse.error(400, "消耗特训符失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> genResult = generalService.addGeneralExp(generalId, SPECIAL_TRAIN_EXP);
            if (genResult != null) {
                result.put("generalLevel", genResult.get("newLevel"));
                result.put("generalExp", genResult.get("currentExp"));
            }
        } catch (Exception e) {
            logger.warn("特训符增加经验失败: {}", e.getMessage());
        }

        result.put("success", true);
        result.put("expGained", SPECIAL_TRAIN_EXP);
        result.put("remainingTokens", getSpecialTrainTokenCount(userId));
        logger.info("用户 {} 使用特训符，武将 {} 获得经验 {}", userId, generalId, SPECIAL_TRAIN_EXP);
        return ApiResponse.success(result);
    }

    private int getSpecialTrainTokenCount(String userId) {
        try {
            Map<String, Object> items = warehouseService.getItems(userId, 0, 999, "all");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemList = (List<Map<String, Object>>) items.get("items");
            if (itemList != null) {
                for (Object obj : itemList) {
                    if (obj instanceof com.tencent.wxcloudrun.model.Warehouse.WarehouseItem) {
                        com.tencent.wxcloudrun.model.Warehouse.WarehouseItem wi =
                            (com.tencent.wxcloudrun.model.Warehouse.WarehouseItem) obj;
                        if (SPECIAL_TRAIN_ITEM_ID.equals(wi.getItemId())) {
                            return wi.getCount() != null ? wi.getCount() : 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("查询特训符数量失败: {}", e.getMessage());
        }
        return 0;
    }
}
