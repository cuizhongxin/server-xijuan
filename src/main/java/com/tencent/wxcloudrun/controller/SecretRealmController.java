package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.SecretRealmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 秘境探险控制器
 */
@RestController
@RequestMapping("/secret-realm")
public class SecretRealmController {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretRealmController.class);
    
    @Autowired
    private SecretRealmService secretRealmService;
    
    /**
     * 探索秘境
     */
    @PostMapping("/explore")
    public ApiResponse<SecretRealmService.ExploreResult> explore(@RequestBody Map<String, Object> body,
                                                                  HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        String realmId = (String) body.get("realmId");
        Integer count = (Integer) body.get("count");
        
        if (count == null) count = 1;
        
        logger.info("秘境探索, userId: {}, realmId: {}, count: {}", userId, realmId, count);
        
        SecretRealmService.ExploreResult result = secretRealmService.explore(userId, realmId, count);
        
        return ApiResponse.success(result);
    }
}
