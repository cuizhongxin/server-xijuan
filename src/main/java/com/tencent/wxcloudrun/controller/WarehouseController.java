package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 仓库控制器
 */
@RestController
@RequestMapping("/warehouse")
public class WarehouseController {
    
    @Autowired
    private WarehouseService warehouseService;
    
    /**
     * 获取仓库信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getWarehouseInfo(
            @RequestAttribute("userId") Long userIdLong) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        Map<String, Object> info = warehouseService.getWarehouseInfo(userId);
        return ApiResponse.success(info);
    }
    
    /**
     * 获取装备列表
     */
    @GetMapping("/equipments")
    public ApiResponse<Map<String, Object>> getEquipments(
            @RequestAttribute("userId") Long userIdLong,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        Map<String, Object> result = warehouseService.getEquipments(userId, page, pageSize);
        return ApiResponse.success(result);
    }
    
    /**
     * 获取物品列表
     */
    @GetMapping("/items")
    public ApiResponse<Map<String, Object>> getItems(
            @RequestAttribute("userId") Long userIdLong,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "all") String itemType) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        Map<String, Object> result = warehouseService.getItems(userId, page, pageSize, itemType);
        return ApiResponse.success(result);
    }
    
    /**
     * 扩充装备仓库
     */
    @PostMapping("/expand/equipment")
    public ApiResponse<Map<String, Object>> expandEquipmentStorage(
            @RequestAttribute("userId") Long userIdLong) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        Map<String, Object> result = warehouseService.expandEquipmentStorage(userId);
        return ApiResponse.success(result);
    }
    
    /**
     * 扩充物品仓库
     */
    @PostMapping("/expand/item")
    public ApiResponse<Map<String, Object>> expandItemStorage(
            @RequestAttribute("userId") Long userIdLong) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        Map<String, Object> result = warehouseService.expandItemStorage(userId);
        return ApiResponse.success(result);
    }
    
    /**
     * 使用物品
     */
    @PostMapping("/use")
    public ApiResponse<Map<String, Object>> useItem(
            @RequestAttribute("userId") Long userIdLong,
            @RequestBody Map<String, Object> request) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        String itemId = (String) request.get("itemId");
        int count = request.containsKey("count") ? (int) request.get("count") : 1;
        Map<String, Object> result = warehouseService.useItem(userId, itemId, count);
        return ApiResponse.success(result);
    }
    
    /**
     * 出售装备
     */
    @PostMapping("/sell/equipment")
    public ApiResponse<Map<String, Object>> sellEquipment(
            @RequestAttribute("userId") Long userIdLong,
            @RequestBody Map<String, Object> request) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        String equipmentId = (String) request.get("equipmentId");
        Map<String, Object> result = warehouseService.sellEquipment(userId, equipmentId);
        return ApiResponse.success(result);
    }
    
    /**
     * 批量出售装备
     */
    @PostMapping("/sell/batch")
    public ApiResponse<Map<String, Object>> batchSellEquipments(
            @RequestAttribute("userId") Long userIdLong,
            @RequestBody Map<String, Object> request) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        @SuppressWarnings("unchecked")
        List<String> equipmentIds = (List<String>) request.get("equipmentIds");
        Map<String, Object> result = warehouseService.batchSellEquipments(userId, equipmentIds);
        return ApiResponse.success(result);
    }
}

