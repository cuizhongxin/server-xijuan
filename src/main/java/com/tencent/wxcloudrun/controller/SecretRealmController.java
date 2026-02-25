package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.SecretRealmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/secret-realm")
public class SecretRealmController {

    private static final Logger logger = LoggerFactory.getLogger(SecretRealmController.class);

    @Autowired
    private SecretRealmService secretRealmService;

    /**
     * 获取所有秘境列表
     */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> listRealms() {
        return ApiResponse.success(secretRealmService.listRealms());
    }

    /**
     * 获取秘境奖励预览(12件: 6装备+6道具)
     */
    @GetMapping("/rewards")
    public ApiResponse<List<Map<String, Object>>> getRewards(@RequestParam String realmId) {
        return ApiResponse.success(secretRealmService.getRealmRewards(realmId));
    }

    /**
     * 探索秘境
     */
    @PostMapping("/explore")
    public ApiResponse<SecretRealmService.ExploreResult> explore(@RequestBody Map<String, Object> body,
                                                                  HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));

        String realmId = (String) body.get("realmId");
        Integer count = body.get("count") instanceof Integer ? (Integer) body.get("count") : 1;

        logger.info("秘境探索, userId: {}, realmId: {}, count: {}", userId, realmId, count);

        SecretRealmService.ExploreResult result = secretRealmService.explore(userId, realmId, count);

        return ApiResponse.success(result);
    }
}
