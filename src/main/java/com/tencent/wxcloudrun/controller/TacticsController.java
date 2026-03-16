package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.tactics.TacticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/tactics")
public class TacticsController {

    private static final Logger logger = LoggerFactory.getLogger(TacticsController.class);

    @Autowired
    private TacticsService tacticsService;

    @GetMapping("/all")
    public ApiResponse<List<Map<String, Object>>> getAllTactics(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(tacticsService.getAllTacticsWithOwnership(userId));
    }

    @GetMapping("/owned")
    public ApiResponse<List<Map<String, Object>>> getOwnedTactics(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(tacticsService.getUserTactics(userId));
    }

    @PostMapping("/craft")
    public ApiResponse<Map<String, Object>> craftTactics(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String tacticsId = (String) body.get("tacticsId");
        logger.info("用户 {} 制造兵法 {}", userId, tacticsId);
        return ApiResponse.success(tacticsService.craftTactics(userId, tacticsId));
    }

    @PostMapping("/upgrade")
    public ApiResponse<Map<String, Object>> upgradeTactics(@RequestBody Map<String, Object> body,
                                                            HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String tacticsId = (String) body.get("tacticsId");
        logger.info("用户 {} 升级兵法 {}", userId, tacticsId);
        return ApiResponse.success(tacticsService.upgradeTactics(userId, tacticsId));
    }

    @PostMapping("/equip")
    public ApiResponse<Map<String, Object>> equipTactics(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String generalId = (String) body.get("generalId");
        String tacticsId = (String) body.get("tacticsId");
        logger.info("用户 {} 给武将 {} 装备兵法 {}", userId, generalId, tacticsId);
        return ApiResponse.success(tacticsService.equipTactics(userId, generalId, tacticsId));
    }

    @PostMapping("/unequip")
    public ApiResponse<Map<String, Object>> unequipTactics(@RequestBody Map<String, Object> body,
                                                            HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String generalId = (String) body.get("generalId");
        logger.info("用户 {} 给武将 {} 卸下兵法", userId, generalId);
        return ApiResponse.success(tacticsService.unequipTactics(userId, generalId));
    }

    @GetMapping("/equipped")
    public ApiResponse<Map<String, Object>> getEquippedTactics(@RequestParam String generalId,
                                                                HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(tacticsService.getEquippedTactics(userId, generalId));
    }

    @GetMapping("/learned")
    public ApiResponse<List<Map<String, Object>>> getLearnedTactics(@RequestParam String generalId,
                                                                      HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(tacticsService.getLearnedTactics(userId, generalId));
    }

    @PostMapping("/learn")
    public ApiResponse<Map<String, Object>> learnTactic(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String generalId = (String) body.get("generalId");
        String tacticsId = (String) body.get("tacticsId");
        logger.info("用户 {} 武将 {} 学习兵法 {}", userId, generalId, tacticsId);
        return ApiResponse.success(tacticsService.learnTactic(userId, generalId, tacticsId));
    }
}
