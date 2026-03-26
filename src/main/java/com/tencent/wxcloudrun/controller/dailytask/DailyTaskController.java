package com.tencent.wxcloudrun.controller.dailytask;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.dailytask.DailyTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/daily-task")
public class DailyTaskController {

    private static final Logger logger = LoggerFactory.getLogger(DailyTaskController.class);

    @Autowired
    private DailyTaskService dailyTaskService;

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        try {
            return ApiResponse.success(dailyTaskService.getInfo(getUserId(request)));
        } catch (Exception e) {
            logger.error("获取日常任务信息失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/claim")
    public ApiResponse<Map<String, Object>> claimTask(HttpServletRequest request,
                                                       @RequestBody Map<String, Object> body) {
        try {
            String taskType = String.valueOf(body.get("taskType"));
            return ApiResponse.success(dailyTaskService.claimDailyTask(getUserId(request), taskType));
        } catch (Exception e) {
            logger.error("领取日常任务奖励失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/claim-stage")
    public ApiResponse<Map<String, Object>> claimStage(HttpServletRequest request,
                                                        @RequestBody Map<String, Object> body) {
        try {
            int stage = ((Number) body.get("stage")).intValue();
            return ApiResponse.success(dailyTaskService.claimStageReward(getUserId(request), stage));
        } catch (Exception e) {
            logger.error("领取阶段奖励失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/claim-achievement")
    public ApiResponse<Map<String, Object>> claimAchievement(HttpServletRequest request,
                                                              @RequestBody Map<String, Object> body) {
        try {
            String achievementId = String.valueOf(body.get("achievementId"));
            return ApiResponse.success(dailyTaskService.claimAchievement(getUserId(request), achievementId));
        } catch (Exception e) {
            logger.error("领取成就奖励失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/use-stamina-item")
    public ApiResponse<Map<String, Object>> useStaminaItem(HttpServletRequest request) {
        try {
            return ApiResponse.success(dailyTaskService.useStaminaItem(getUserId(request)));
        } catch (Exception e) {
            logger.error("使用精力丹失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
