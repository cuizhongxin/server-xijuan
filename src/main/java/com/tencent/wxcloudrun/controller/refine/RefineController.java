package com.tencent.wxcloudrun.controller.refine;

import com.tencent.wxcloudrun.service.refine.RefineService;
import com.tencent.wxcloudrun.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 精炼控制器 - 装备强化、品质提升、套装融合、装备分解
 */
@RestController
@RequestMapping("/refine")
public class RefineController {

    @Autowired
    private RefineService refineService;

    /**
     * 获取强化信息
     */
    @GetMapping("/enhance/info")
    public ApiResponse getEnhanceInfo(
            @RequestParam String equipmentId,
            HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        Map<String, Object> info = refineService.getEnhanceInfo(odUserId, equipmentId);
        return ApiResponse.success(info);
    }

    /**
     * 执行强化
     */
    @PostMapping("/enhance")
    public ApiResponse enhance(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String equipmentId = (String) params.get("equipmentId");
        Boolean useProtect = (Boolean) params.getOrDefault("useProtect", false);
        
        Map<String, Object> result = refineService.enhance(odUserId, equipmentId, useProtect);
        return ApiResponse.success(result);
    }

    /**
     * 获取品质信息
     */
    @GetMapping("/quality/info")
    public ApiResponse getQualityInfo(
            @RequestParam String equipmentId,
            HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        Map<String, Object> info = refineService.getQualityInfo(odUserId, equipmentId);
        return ApiResponse.success(info);
    }

    /**
     * 提升品质
     */
    @PostMapping("/quality/upgrade")
    public ApiResponse upgradeQuality(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String equipmentId = (String) params.get("equipmentId");
        
        Map<String, Object> result = refineService.upgradeQuality(odUserId, equipmentId);
        return ApiResponse.success(result);
    }

    /**
     * 获取套装信息
     */
    @GetMapping("/set/info")
    public ApiResponse getSetInfo(@RequestParam String setId) {
        Map<String, Object> info = refineService.getSetInfo(setId);
        return ApiResponse.success(info);
    }

    /**
     * 套装融合
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/fuse")
    public ApiResponse fuse(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        List<String> equipmentIds = (List<String>) params.get("equipmentIds");
        Integer targetSlotId = (Integer) params.get("targetSlotId");
        
        Map<String, Object> result = refineService.fuseEquipments(odUserId, equipmentIds, targetSlotId);
        return ApiResponse.success(result);
    }

    /**
     * 装备分解
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/decompose")
    public ApiResponse decompose(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        List<String> equipmentIds = (List<String>) params.get("equipmentIds");
        
        Map<String, Object> result = refineService.decomposeEquipments(odUserId, equipmentIds);
        return ApiResponse.success(result);
    }
}
