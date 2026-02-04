package com.tencent.wxcloudrun.controller.recruit;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.dto.RecruitRequest;
import com.tencent.wxcloudrun.dto.RecruitResult;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.recruit.RecruitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 招募控制器
 */
@RestController
@RequestMapping("/recruit")
public class RecruitController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecruitController.class);
    
    @Autowired
    private RecruitService recruitService;
    
    /**
     * 获取用户资源信息
     */
    @GetMapping("/resource")
    public ApiResponse<UserResource> getResource(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("获取用户资源, userId: {}", userId);
        
        UserResource resource = recruitService.getUserResource(userId);
        
        return ApiResponse.success(resource);
    }
    
    /**
     * 每日领取初级招贤令
     */
    @PostMapping("/claim-daily")
    public ApiResponse<UserResource> claimDaily(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("领取每日招贤令, userId: {}", userId);
        
        UserResource resource = recruitService.claimDailyTokens(userId);
        
        return ApiResponse.success(resource);
    }
    
    /**
     * 购买招贤令
     */
    @PostMapping("/buy")
    public ApiResponse<UserResource> buyToken(@RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        String userId = request.getAttribute("userId").toString();
        String tokenType = body.get("tokenType").toString();
        
        logger.info("购买招贤令, userId: {}, tokenType: {}", userId, tokenType);
        
        UserResource resource = recruitService.buyToken(userId, tokenType);
        
        return ApiResponse.success(resource);
    }
    
    /**
     * 合成高级招贤令
     */
    @PostMapping("/compose")
    public ApiResponse<UserResource> composeToken(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        String fromType = (String) body.get("fromType");
        
        logger.info("合成招贤令, userId: {}, fromType: {}", userId, fromType);
        
        UserResource resource = recruitService.composeToken(userId, fromType);
        
        return ApiResponse.success(resource);
    }
    
    /**
     * 招募武将
     */
    @PostMapping("/recruit")
    public ApiResponse<RecruitResult> recruit(@RequestBody RecruitRequest recruitRequest,
                                             HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("招募武将, userId: {}, tokenType: {}, count: {}", 
                   userId, recruitRequest.getTokenType(), recruitRequest.getCount());
        
        // 执行招募
        List<General> generals = recruitService.recruit(userId, 
                                                       recruitRequest.getTokenType(), 
                                                       recruitRequest.getCount());
        
        // 获取更新后的资源
        UserResource resource = recruitService.getUserResource(userId);
        
        // 构建结果
        int remainingTokens = 0;
        switch (recruitRequest.getTokenType().toUpperCase()) {
            case "JUNIOR":
                remainingTokens = resource.getJuniorToken();
                break;
            case "INTERMEDIATE":
                remainingTokens = resource.getIntermediateToken();
                break;
            case "SENIOR":
                remainingTokens = resource.getSeniorToken();
                break;
        }
        
        // 检查是否有橙色或紫色武将
        boolean hasOrange = false;
        boolean hasPurple = false;
        for (General general : generals) {
            if (general.getQuality().getId() == 6) {
                hasOrange = true;
            } else if (general.getQuality().getId() == 5) {
                hasPurple = true;
            }
        }
        
        RecruitResult result = RecruitResult.builder()
            .generals(generals)
            .remainingTokens(remainingTokens)
            .hasOrange(hasOrange)
            .hasPurple(hasPurple)
            .build();
        
        return ApiResponse.success(result);
    }
}


