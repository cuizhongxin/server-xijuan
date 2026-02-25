package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.enhance.EnhanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 强化控制器
 */
@RestController
@RequestMapping("/enhance")
public class EnhanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhanceController.class);
    
    @Autowired
    private EnhanceService enhanceService;
    
    /**
     * 强化装备
     */
    @PostMapping("/equipment")
    public ApiResponse<Map<String, Object>> enhanceEquipment(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        String equipmentId = (String) body.get("equipmentId");
        
        logger.info("强化装备, userId: {}, equipmentId: {}", userId, equipmentId);
        
        Map<String, Object> result = enhanceService.enhanceEquipment(userId, equipmentId);
        return ApiResponse.success(result);
    }
    
    /**
     * 转移强化
     */
    @PostMapping("/transfer")
    public ApiResponse<Map<String, Object>> transferEnhance(@RequestBody Map<String, Object> body,
                                                           HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        String fromEquipmentId = (String) body.get("fromEquipmentId");
        String toEquipmentId = (String) body.get("toEquipmentId");
        
        logger.info("转移强化, userId: {}, from: {}, to: {}", userId, fromEquipmentId, toEquipmentId);
        
        Map<String, Object> result = enhanceService.transferEnhance(userId, fromEquipmentId, toEquipmentId);
        return ApiResponse.success(result);
    }
    
    /**
     * 合成强化石
     */
    @PostMapping("/merge")
    public ApiResponse<Map<String, Object>> mergeEnhanceStones(@RequestBody Map<String, Object> body,
                                                               HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        Integer stoneLevel = body.get("stoneLevel") != null ? 
            Integer.parseInt(body.get("stoneLevel").toString()) : 1;
        Integer count = body.get("count") != null ? 
            Integer.parseInt(body.get("count").toString()) : 3;
        
        logger.info("合成强化石, userId: {}, level: {}, count: {}", userId, stoneLevel, count);
        
        Map<String, Object> result = enhanceService.mergeEnhanceStones(userId, stoneLevel, count);
        return ApiResponse.success(result);
    }
}
