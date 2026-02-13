package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.Formation;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.service.formation.FormationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 阵型控制器
 */
@RestController
@RequestMapping("/formation")
public class FormationController {
    
    @Autowired
    private FormationService formationService;
    
    /**
     * 获取阵型
     */
    @GetMapping("")
    public ApiResponse<Map<String, Object>> getFormation(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        Map<String, Object> formation = formationService.getFormationDetail(userId);
        return ApiResponse.success(formation);
    }
    
    /**
     * 设置单个槽位
     */
    @PostMapping("/slot")
    public ApiResponse<Formation> setSlot(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        int slotIndex = Integer.parseInt(body.get("position").toString());
        String generalId = body.get("generalId") != null ? body.get("generalId").toString() : null;
        Formation formation = formationService.setSlot(userId, slotIndex, generalId);
        return ApiResponse.success(formation);
    }
    
    /**
     * 批量设置阵型
     */
    @PostMapping("/batch")
    public ApiResponse<Formation> setFormation(@RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        @SuppressWarnings("unchecked")
        List<String> generalIds = (List<String>) body.get("generalIds");
        Formation formation = formationService.setFormation(userId, generalIds);
        return ApiResponse.success(formation);
    }
    
    /**
     * 交换槽位
     */
    @PostMapping("/swap")
    public ApiResponse<Formation> swapSlots(@RequestBody Map<String, Object> body,
                                            HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        int slotIndex1 = Integer.parseInt(body.get("slotIndex1").toString());
        int slotIndex2 = Integer.parseInt(body.get("slotIndex2").toString());
        Formation formation = formationService.swapSlots(userId, slotIndex1, slotIndex2);
        return ApiResponse.success(formation);
    }
    
    /**
     * 清空槽位
     */
    @PostMapping("/clear")
    public ApiResponse<Formation> clearSlot(@RequestBody Map<String, Object> body,
                                            HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        int slotIndex = Integer.parseInt(body.get("slotIndex").toString());
        Formation formation = formationService.setSlot(userId, slotIndex, null);
        return ApiResponse.success(formation);
    }
    
    /**
     * 获取战斗顺序
     */
    @GetMapping("/battle-order")
    public ApiResponse<List<General>> getBattleOrder(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        List<General> order = formationService.getBattleOrder(userId);
        return ApiResponse.success(order);
    }
}

