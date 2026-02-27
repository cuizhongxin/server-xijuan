package com.tencent.wxcloudrun.controller.herorank;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.herorank.HeroRankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/herorank")
public class HeroRankController {

    @Autowired
    private HeroRankService heroRankService;

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request,
                                                     @RequestParam(defaultValue = "0") int page) {
        String userId = getUserId(request);
        return ApiResponse.success(heroRankService.getHeroRankInfo(userId, page));
    }

    @PostMapping("/challenge")
    public ApiResponse<Map<String, Object>> challenge(HttpServletRequest request,
                                                       @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        String targetId = (String) body.get("targetId");
        return ApiResponse.success(heroRankService.challenge(userId, targetId));
    }

    @PostMapping("/purchase")
    public ApiResponse<Map<String, Object>> purchaseChallenge(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(heroRankService.purchaseChallenge(userId));
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getBattleRecords(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(heroRankService.getBattleRecords(userId));
    }

    @GetMapping("/rewards")
    public ApiResponse<List<Map<String, Object>>> getRewardRecords(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(heroRankService.getRewardRecords(userId));
    }

    @PostMapping("/sync")
    public ApiResponse<String> syncPower(HttpServletRequest request) {
        String userId = getUserId(request);
        heroRankService.syncPower(userId);
        return ApiResponse.success("ok");
    }

    @PostMapping("/resetCooldown")
    public ApiResponse<Map<String, Object>> resetCooldown(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(heroRankService.resetCooldown(userId));
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
}
