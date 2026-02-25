package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.level.LevelService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/training")
public class TrainingController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingController.class);

    @Autowired
    private UserResourceService userResourceService;
    
    @Autowired
    private LevelService levelService;
    
    @Autowired
    private GeneralService generalService;
    
    @Autowired
    private GeneralRepository generalRepository;

    /**
     * 执行训练
     */
    @PostMapping("/train")
    public ApiResponse<Map<String, Object>> train(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        String mode = (String) body.get("mode"); // lord 或 general
        String foodLevel = (String) body.get("foodLevel");
        Integer count = body.get("count") != null ? Integer.parseInt(body.get("count").toString()) : 1;
        String generalId = (String) body.get("generalId");
        Integer foodCost = body.get("foodCost") != null ? Integer.parseInt(body.get("foodCost").toString()) : 0;
        Integer lordExp = body.get("lordExp") != null ? Integer.parseInt(body.get("lordExp").toString()) : 0;
        Integer generalExp = body.get("generalExp") != null ? Integer.parseInt(body.get("generalExp").toString()) : 0;
        
        logger.info("用户 {} 进行{}训练, 等级={}, 次数={}, 消耗粮食={}", 
            userId, mode, foodLevel, count, foodCost);
        
        // 检查并扣除粮食
        UserResource resource = userResourceService.getUserResource(userId);
        if (resource.getFood() < foodCost) {
            return ApiResponse.error(400, "粮食不足");
        }
        
        resource.setFood(resource.getFood() - foodCost);
        userResourceService.saveResource(resource);
        
        Map<String, Object> result = new HashMap<>();
        boolean levelUp = false;
        
        // 增加主公经验
        if (lordExp > 0) {
            try {
                Map<String, Object> levelResult = levelService.addExp(userId, lordExp.longValue(), "训练");
                if (levelResult != null && levelResult.containsKey("levelUp")) {
                    levelUp = Boolean.TRUE.equals(levelResult.get("levelUp"));
                }
            } catch (Exception e) {
                logger.warn("增加主公经验失败: {}", e.getMessage());
            }
        }
        
        // 武将特训模式下增加武将经验
        if ("general".equals(mode) && generalId != null && generalExp > 0) {
            try {
                Map<String, Object> general = generalService.addGeneralExp(generalId, generalExp.longValue());
                if (general != null) {
                    result.put("generalLevel", general.get("newLevel").toString());
                    result.put("generalExp", general.get("currentExp").toString());
                    result.put("generalMaxExp", general.get("maxExp").toString());
                }
            } catch (Exception e) {
                logger.warn("增加武将经验失败: {}", e.getMessage());
            }
        }
        
        result.put("success", true);
        result.put("levelUp", levelUp);
        result.put("lordExpGained", lordExp);
        result.put("generalExpGained", generalExp);
        result.put("remainingFood", resource.getFood());
        
        return ApiResponse.success(result);
    }
}
