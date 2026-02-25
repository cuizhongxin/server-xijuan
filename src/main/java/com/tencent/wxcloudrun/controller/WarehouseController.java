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
    
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getWarehouseInfo(
            @RequestAttribute("userId") String userId) {
        Map<String, Object> info = warehouseService.getWarehouseInfo(userId);
        return ApiResponse.success(info);
    }
    
    @GetMapping("/equipments")
    public ApiResponse<Map<String, Object>> getEquipments(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Map<String, Object> result = warehouseService.getEquipments(userId, page, pageSize);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/items")
    public ApiResponse<Map<String, Object>> getItems(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "all") String itemType) {
        Map<String, Object> result = warehouseService.getItems(userId, page, pageSize, itemType);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/expand/equipment")
    public ApiResponse<Map<String, Object>> expandEquipmentStorage(
            @RequestAttribute("userId") String userId) {
        Map<String, Object> result = warehouseService.expandEquipmentStorage(userId);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/expand/item")
    public ApiResponse<Map<String, Object>> expandItemStorage(
            @RequestAttribute("userId") String userId) {
        Map<String, Object> result = warehouseService.expandItemStorage(userId);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/use")
    public ApiResponse<Map<String, Object>> useItem(
            @RequestAttribute("userId") String userId,
            @RequestBody Map<String, Object> request) {
        String itemId = (String) request.get("itemId");
        int count = request.containsKey("count") ? (int) request.get("count") : 1;
        Map<String, Object> result = warehouseService.useItem(userId, itemId, count);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/sell/equipment")
    public ApiResponse<Map<String, Object>> sellEquipment(
            @RequestAttribute("userId") String userId,
            @RequestBody Map<String, Object> request) {
        String equipmentId = (String) request.get("equipmentId");
        Map<String, Object> result = warehouseService.sellEquipment(userId, equipmentId);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/sell/batch")
    public ApiResponse<Map<String, Object>> batchSellEquipments(
            @RequestAttribute("userId") String userId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> equipmentIds = (List<String>) request.get("equipmentIds");
        Map<String, Object> result = warehouseService.batchSellEquipments(userId, equipmentIds);
        return ApiResponse.success(result);
    }
}
