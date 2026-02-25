package com.tencent.wxcloudrun.controller.general;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.service.general.GeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 武将控制器
 */
@RestController
@RequestMapping("/general")
public class GeneralController {
    
    private static final Logger logger = LoggerFactory.getLogger(GeneralController.class);
    
    @Autowired
    private GeneralService generalService;
    
    /**
     * 获取用户武将列表
     */
    @GetMapping("/list")
    public ApiResponse<List<General>> getGeneralList(HttpServletRequest request) {
        // 从request attribute获取userId（由拦截器设置）
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取武将列表, userId: {}", userId);
        
        List<General> generals = generalService.getUserGenerals(userId);
        
        return ApiResponse.success(generals);
    }
    
    /**
     * 获取单个武将详情
     */
    @GetMapping("/{generalId}")
    public ApiResponse<General> getGeneralDetail(@PathVariable String generalId, 
                                                  HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取武将详情, userId: {}, generalId: {}", userId, generalId);
        
        General general = generalService.getGeneralById(generalId);
        
        if (general == null) {
            return ApiResponse.error(404, "武将不存在");
        }
        
        // 验证武将是否属于当前用户
        if (!general.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权访问该武将");
        }
        
        return ApiResponse.success(general);
    }
    
    /**
     * 初始化用户武将（首次登录）
     */
    @PostMapping("/init")
    public ApiResponse<List<General>> initGenerals(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("初始化武将, userId: {}", userId);
        
        List<General> generals = generalService.initUserGenerals(userId);
        
        return ApiResponse.success(generals);
    }
    
    /**
     * 解雇武将
     */
    @PostMapping("/dismiss")
    public ApiResponse<?> dismissGeneral(@RequestBody java.util.Map<String, String> params,
                                         HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String generalId = params.get("generalId");
        
        logger.info("解雇武将, userId: {}, generalId: {}", userId, generalId);
        
        try {
            boolean result = generalService.dismissGeneral(userId, generalId);
            return ApiResponse.success(java.util.Collections.singletonMap("success", result));
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 将领传承
     */
    @PostMapping("/inherit")
    public ApiResponse<?> inheritGeneral(@RequestBody java.util.Map<String, String> params,
                                         HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String sourceGeneralId = params.get("sourceGeneralId");
        String targetGeneralId = params.get("targetGeneralId");
        String scrollType = params.get("scrollType");
        
        logger.info("将领传承, userId: {}, source: {}, target: {}, scroll: {}", 
                   userId, sourceGeneralId, targetGeneralId, scrollType);
        
        try {
            java.util.Map<String, Object> result = generalService.inheritGeneral(
                userId, sourceGeneralId, targetGeneralId, scrollType);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 军事演习
     */
    @PostMapping("/drill")
    public ApiResponse<?> drill(@RequestBody java.util.Map<String, Object> params,
                                HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String generalId = (String) params.get("generalId");
        String drillType = (String) params.get("drillType");
        Integer count = (Integer) params.get("count");
        
        if (count == null) count = 1;
        
        logger.info("军事演习, userId: {}, generalId: {}, type: {}, count: {}", 
                   userId, generalId, drillType, count);
        
        try {
            java.util.Map<String, Object> result = generalService.drill(
                userId, generalId, drillType, count);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}


