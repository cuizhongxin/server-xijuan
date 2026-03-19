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
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(heroRankService.getInfo(userId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/challenge")
    public ApiResponse<Map<String, Object>> challenge(HttpServletRequest request,
                                                       @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String targetId = (String) body.get("targetId");
            return ApiResponse.success(heroRankService.challenge(userId, targetId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/speedup")
    public ApiResponse<Map<String, Object>> speedUp(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(heroRankService.speedUp(userId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/claim-reward")
    public ApiResponse<Map<String, Object>> claimReward(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(heroRankService.claimReward(userId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getRecords(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(heroRankService.getRecords(userId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/battle-report")
    public ApiResponse<Map<String, Object>> getBattleReport(@RequestParam long id) {
        try {
            return ApiResponse.success(heroRankService.getBattleReport(id));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/sync")
    public ApiResponse<String> syncPower(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            heroRankService.syncPower(userId);
            return ApiResponse.success("ok");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
}
