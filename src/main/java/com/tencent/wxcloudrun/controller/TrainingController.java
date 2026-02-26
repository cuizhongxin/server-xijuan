package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.level.LevelService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 训练 - 三级训练体系（匹配资源产出）
 *
 * 每日粮食产出约16,000，银两约36,000
 * 训练是主要经验来源，免费玩家每日训练可获约6,000-8,000经验
 *
 * 初级训练: 消耗少，适合新手和资源紧张时
 * 中级训练: 消耗适中，性价比最优
 * 高级训练: 消耗大，经验最多，适合资源充裕/氪金玩家
 */
@RestController
@RequestMapping("/training")
public class TrainingController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingController.class);

    // [消耗粮食, 消耗银两, 主公修炼经验, 武将特训-武将经验, 武将特训-主公经验]
    private static final Map<String, long[]> GRADE_CONFIG = new LinkedHashMap<>();
    static {
        GRADE_CONFIG.put("junior",       new long[]{500,     0,    300,   300,   100});
        GRADE_CONFIG.put("intermediate", new long[]{1500,  3000,  1000,  1000,   300});
        GRADE_CONFIG.put("senior",       new long[]{4000,  8000,  3000,  3000,   800});
    }

    @Autowired
    private UserResourceService userResourceService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private GeneralService generalService;

    /** 获取训练配置 */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        List<Map<String, Object>> grades = new ArrayList<>();
        String[] ids = {"junior", "intermediate", "senior"};
        String[] names = {"初级训练", "中级训练", "高级训练"};
        String[] icons = {"🍚", "🍱", "🍖"};
        String[] colors = {"#55ff55", "#5599ff", "#ff9933"};

        for (int i = 0; i < ids.length; i++) {
            long[] cfg = GRADE_CONFIG.get(ids[i]);
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("id", ids[i]);
            g.put("name", names[i]);
            g.put("icon", icons[i]);
            g.put("color", colors[i]);
            g.put("foodCost", cfg[0]);
            g.put("silverCost", cfg[1]);
            g.put("lordExp", cfg[2]);
            g.put("generalExp", cfg[3]);
            g.put("generalLordExp", cfg[4]);
            grades.add(g);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("grades", grades);
        return ApiResponse.success(result);
    }

    /** 执行训练 */
    @PostMapping("/train")
    public ApiResponse<Map<String, Object>> train(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String mode = (String) body.getOrDefault("mode", "lord");
        String grade = (String) body.getOrDefault("grade", "junior");
        int count = body.get("count") != null ? Integer.parseInt(body.get("count").toString()) : 1;
        String generalId = (String) body.get("generalId");

        if (count < 1 || count > 99) return ApiResponse.error(400, "训练次数需在1-99之间");

        long[] cfg = GRADE_CONFIG.get(grade);
        if (cfg == null) return ApiResponse.error(400, "无效的训练等级");

        long totalFood = cfg[0] * count;
        long totalSilver = cfg[1] * count;

        UserResource resource = userResourceService.getUserResource(userId);
        long curFood = resource.getFood() != null ? resource.getFood() : 0;
        long curSilver = resource.getSilver() != null ? resource.getSilver() : 0;

        if (curFood < totalFood) return ApiResponse.error(400, "粮食不足，需要" + totalFood + "，当前" + curFood);
        if (curSilver < totalSilver) return ApiResponse.error(400, "银两不足，需要" + totalSilver + "，当前" + curSilver);

        resource.setFood(curFood - totalFood);
        if (totalSilver > 0) resource.setSilver(curSilver - totalSilver);
        userResourceService.saveResource(resource);

        Map<String, Object> result = new HashMap<>();
        boolean levelUp = false;

        if ("lord".equals(mode)) {
            long lordExp = cfg[2] * count;
            try {
                Map<String, Object> lvlResult = levelService.addExp(userId, lordExp, "训练");
                if (lvlResult != null) {
                    levelUp = Boolean.TRUE.equals(lvlResult.get("levelUp"));
                    result.put("newLevel", lvlResult.get("newLevel"));
                }
            } catch (Exception e) { logger.warn("增加主公经验失败: {}", e.getMessage()); }
            result.put("lordExpGained", lordExp);
            result.put("generalExpGained", 0);
        } else {
            long genExp = cfg[3] * count;
            long lordExp = cfg[4] * count;
            try {
                Map<String, Object> lvlResult = levelService.addExp(userId, lordExp, "训练");
                if (lvlResult != null) {
                    levelUp = Boolean.TRUE.equals(lvlResult.get("levelUp"));
                    result.put("newLevel", lvlResult.get("newLevel"));
                }
            } catch (Exception e) { logger.warn("增加主公经验失败: {}", e.getMessage()); }

            if (generalId != null) {
                try {
                    Map<String, Object> genResult = generalService.addGeneralExp(generalId, genExp);
                    if (genResult != null) {
                        result.put("generalLevel", genResult.get("newLevel"));
                        result.put("generalExp", genResult.get("currentExp"));
                        result.put("generalMaxExp", genResult.get("maxExp"));
                    }
                } catch (Exception e) { logger.warn("增加武将经验失败: {}", e.getMessage()); }
            }
            result.put("lordExpGained", lordExp);
            result.put("generalExpGained", genExp);
        }

        result.put("success", true);
        result.put("levelUp", levelUp);
        result.put("grade", grade);
        result.put("foodConsumed", totalFood);
        result.put("silverConsumed", totalSilver);
        result.put("remainingFood", resource.getFood());
        result.put("remainingSilver", resource.getSilver());

        logger.info("用户 {} {}训练({})x{}，消耗粮食{}银两{}", userId, mode, grade, count, totalFood, totalSilver);
        return ApiResponse.success(result);
    }
}
