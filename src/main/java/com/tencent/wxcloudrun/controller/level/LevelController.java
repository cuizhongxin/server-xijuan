package com.tencent.wxcloudrun.controller.level;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.UserLevel;
import com.tencent.wxcloudrun.service.level.LevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 等级接口
 */
@RestController
@RequestMapping("/level")
public class LevelController {
    
    private static final Logger logger = LoggerFactory.getLogger(LevelController.class);
    
    @Autowired
    private LevelService levelService;
    
    /**
     * 获取用户等级信息
     */
    @GetMapping
    public ApiResponse<UserLevel> getUserLevel(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("获取用户等级, userId: {}", userId);
        
        UserLevel userLevel = levelService.getUserLevel(userId);
        
        return ApiResponse.success(userLevel);
    }
    
    /**
     * 领取每日登录经验
     */
    @PostMapping("/daily-login")
    public ApiResponse<Map<String, Object>> claimDailyLogin(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("领取每日登录经验, userId: {}", userId);
        
        Map<String, Object> result = levelService.claimDailyLoginExp(userId);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取等级配置信息
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getLevelConfig() {
        Map<String, Object> config = levelService.getLevelConfigInfo();
        return ApiResponse.success(config);
    }
}


