package com.tencent.wxcloudrun.controller.bosswar;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.bosswar.BossWarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/boss-war")
public class BossWarController {

    private static final Logger logger = LoggerFactory.getLogger(BossWarController.class);

    @Autowired
    private BossWarService bossWarService;

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(bossWarService.getInfo(userId));
        } catch (Exception e) {
            logger.error("获取Boss战信息失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/attack")
    public ApiResponse<Map<String, Object>> attack(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            int bossId = Integer.parseInt(String.valueOf(body.getOrDefault("bossId", "1001")));
            return ApiResponse.success(bossWarService.attack(userId, bossId));
        } catch (Exception e) {
            logger.error("Boss战攻击失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/rankings/{bossId}")
    public ApiResponse<Map<String, Object>> getRankings(@PathVariable int bossId) {
        try {
            return ApiResponse.success(bossWarService.getRankings(bossId));
        } catch (Exception e) {
            logger.error("获取Boss战排名失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/open-chest")
    public ApiResponse<Map<String, Object>> openChest(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String chestItemId = String.valueOf(body.get("chestItemId"));
            return ApiResponse.success(bossWarService.openBossChest(userId, chestItemId));
        } catch (Exception e) {
            logger.error("开启Boss宝箱失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/open-tiandi")
    public ApiResponse<Map<String, Object>> openTiandi(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(bossWarService.openTiandiChest(userId));
        } catch (Exception e) {
            logger.error("开启天地宝盒失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
