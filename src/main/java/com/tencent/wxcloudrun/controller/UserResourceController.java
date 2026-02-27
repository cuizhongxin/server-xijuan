package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.plunder.PlunderService;
import com.tencent.wxcloudrun.service.herorank.HeroRankService;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户资源控制器
 */
@RestController
@RequestMapping("/resource")
public class UserResourceController {
    
    @Autowired
    private UserResourceService resourceService;
    
    @Autowired
    private PlunderService plunderService;

    @Autowired
    private HeroRankService heroRankService;

    @Autowired
    private NationWarService nationWarService;

    /**
     * 获取用户资源
     */
    @GetMapping("")
    public ApiResponse<UserResource> getUserResource(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        UserResource resource = resourceService.getUserResource(userId);
        return ApiResponse.success(resource);
    }
    
    /**
     * 获取简要资源信息（用于首页展示）
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getResourceSummary(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        UserResource resource = resourceService.getUserResource(userId);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("gold", resource.getGold());
        summary.put("silver", resource.getSilver());
        summary.put("diamond", resource.getDiamond());
        summary.put("stamina", resource.getStamina());
        summary.put("maxStamina", resource.getMaxStamina());
        summary.put("generalOrder", resource.getGeneralOrder());
        summary.put("rank", resource.getRank());
        summary.put("fame", resource.getFame());
        summary.put("generalCount", resource.getGeneralCount());
        summary.put("maxGeneral", resource.getMaxGeneral());
        summary.put("vipLevel", resource.getVipLevel());
        
        // 挑战剩余次数
        try {
            Map<String, Object> heroInfo = heroRankService.getHeroRankInfo(userId, 0);
            Map<String, Object> myRank = (Map<String, Object>) heroInfo.get("myRank");
            int maxChallenge = heroInfo.get("maxChallenge") != null ? ((Number) heroInfo.get("maxChallenge")).intValue() : 5;
            if (myRank != null) {
                int todayChal = myRank.get("todayChallenge") != null ? ((Number) myRank.get("todayChallenge")).intValue() : 0;
                int todayPur = myRank.get("todayPurchased") != null ? ((Number) myRank.get("todayPurchased")).intValue() : 0;
                int totalAllowed = maxChallenge + todayPur;
                summary.put("challengeRemain", totalAllowed - todayChal);
            } else {
                summary.put("challengeRemain", maxChallenge);
            }
        } catch (Exception e) {
            summary.put("challengeRemain", 10);
        }

        // 掠夺剩余次数
        try {
            Map<String, Object> plunderInfo = plunderService.getPlunderInfo(userId);
            summary.put("plunderAvailable", plunderInfo.get("availableCount"));
        } catch (Exception e) {
            summary.put("plunderAvailable", 0);
        }

        // 国籍
        try {
            String nation = nationWarService.getPlayerNation(userId);
            summary.put("nationality", nation);
        } catch (Exception e) {
            summary.put("nationality", null);
        }

        return ApiResponse.success(summary);
    }
    
    /**
     * 获取武将位信息
     */
    @GetMapping("/general-slots")
    public ApiResponse<Map<String, Object>> getGeneralSlotInfo(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        Map<String, Object> slotInfo = resourceService.getGeneralSlotInfo(userId);
        return ApiResponse.success(slotInfo);
    }
    
    /**
     * 购买武将位
     */
    @PostMapping("/general-slots/purchase")
    public ApiResponse<Map<String, Object>> purchaseGeneralSlot(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        Map<String, Object> result = resourceService.purchaseGeneralSlot(userId);
        return ApiResponse.success(result);
    }
}
