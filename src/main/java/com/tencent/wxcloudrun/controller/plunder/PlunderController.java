package com.tencent.wxcloudrun.controller.plunder;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.plunder.PlunderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/plunder")
public class PlunderController {

    private static final Logger logger = LoggerFactory.getLogger(PlunderController.class);

    @Autowired
    private PlunderService plunderService;

    /**
     * 获取掠夺主页信息（次数、购买信息）
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        return ApiResponse.success(plunderService.getPlunderInfo(userId));
    }

    /**
     * 获取掠夺目标列表
     */
    @GetMapping("/targets")
    public ApiResponse<Map<String, Object>> getTargets(
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        return ApiResponse.success(plunderService.getTargetList(userId, page));
    }

    /**
     * 执行掠夺
     */
    @PostMapping("/attack")
    public ApiResponse<Map<String, Object>> attack(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        String targetId = (String) body.get("targetId");
        String generalId = (String) body.get("generalId");

        logger.info("掠夺攻击, userId={}, targetId={}, generalId={}", userId, targetId, generalId);
        return ApiResponse.success(plunderService.doPlunder(userId, targetId, generalId));
    }

    /**
     * 购买掠夺次数
     */
    @PostMapping("/purchase")
    public ApiResponse<Map<String, Object>> purchase(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        return ApiResponse.success(plunderService.purchaseCount(userId));
    }

    /**
     * 获取掠夺记录
     */
    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> getRecords(
            @RequestParam(defaultValue = "attack") String type,
            HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        return ApiResponse.success(plunderService.getRecords(userId, type));
    }
}
