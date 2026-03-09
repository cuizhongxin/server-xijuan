package com.tencent.wxcloudrun.controller.recruit;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.dto.RecruitResult;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.recruit.RecruitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 招募控制器
 * 仅支持单抽：初级/中级/高级
 * 高级招募额外产出将魂，200将魂可召唤橙色武将
 */
@RestController
@RequestMapping("/recruit")
public class RecruitController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecruitController.class);
    
    @Autowired
    private RecruitService recruitService;
    
    /**
     * 获取用户资源信息（包含将魂）
     */
    @GetMapping("/resource")
    public ApiResponse<UserResource> getResource(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        logger.info("获取用户资源, userId: {}", userId);
        UserResource resource = recruitService.getUserResource(userId);
        return ApiResponse.success(resource);
    }
    
    /**
     * 每日领取初级招贤令
     */
    @PostMapping("/claim-daily")
    public ApiResponse<UserResource> claimDaily(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
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
        String userId = String.valueOf(request.getAttribute("userId"));
        String fromType = (String) body.get("fromType");
        logger.info("合成招贤令, userId: {}, fromType: {}", userId, fromType);
        UserResource resource = recruitService.composeToken(userId, fromType);
        return ApiResponse.success(resource);
    }
    
    /**
     * 单抽招募武将（不再支持十连抽）
     * 请求体: { "tokenType": "JUNIOR" | "INTERMEDIATE" | "SENIOR" }
     */
    @PostMapping("/recruit")
    public ApiResponse<RecruitResult> recruit(@RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String tokenType = body.get("tokenType").toString();
        Object sid = request.getAttribute("serverId");
        String serverId = sid != null ? sid.toString() : null;
        
        logger.info("单抽招募武将, userId: {}, tokenType: {}, serverId: {}", userId, tokenType, serverId);
        
        RecruitResult result = recruitService.recruit(userId, tokenType, serverId);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 将魂召唤 - 消耗200将魂直接召唤一个橙色武将
     */
    @PostMapping("/soul-summon")
    public ApiResponse<RecruitResult> soulSummon(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        Object sid = request.getAttribute("serverId");
        String serverId = sid != null ? sid.toString() : null;
        
        logger.info("将魂召唤, userId: {}, serverId: {}", userId, serverId);
        
        RecruitResult result = recruitService.soulSummon(userId, serverId);
        
        return ApiResponse.success(result);
    }
}
