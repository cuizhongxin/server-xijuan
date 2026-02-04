package com.tencent.wxcloudrun.controller.production;

import com.tencent.wxcloudrun.model.Production;
import com.tencent.wxcloudrun.model.Production.Recipe;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.production.ProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产制造控制器
 */
@Slf4j
@RestController
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {
    
    private final ProductionService productionService;
    private final UserResourceService userResourceService;
    
    /**
     * 获取生产数据
     */
    @GetMapping("/info")
    public Map<String, Object> getProductionInfo(@RequestHeader("X-User-ID") String odUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Production production = productionService.getProduction(odUserId);
            UserResource resource = userResourceService.getUserResource(odUserId);
            
            result.put("success", true);
            result.put("production", production);
            result.put("resource", resource);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 生产资源
     */
    @PostMapping("/produce")
    public Map<String, Object> produce(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String facilityType = (String) body.get("facilityType");
            Map<String, Object> produceResult = productionService.produce(odUserId, facilityType);
            
            result.put("success", true);
            result.putAll(produceResult);
            result.put("message", "生产成功，获得 " + produceResult.get("output") + " 资源");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 升级生产设施
     */
    @PostMapping("/upgrade-facility")
    public Map<String, Object> upgradeFacility(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String facilityType = (String) body.get("facilityType");
            Map<String, Object> upgradeResult = productionService.upgradeFacility(odUserId, facilityType);
            
            result.put("success", true);
            result.putAll(upgradeResult);
            result.put("message", "升级成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 升级制造设施
     */
    @PostMapping("/upgrade-manufacture")
    public Map<String, Object> upgradeManufactureFacility(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String facilityType = (String) body.get("facilityType");
            Map<String, Object> upgradeResult = productionService.upgradeManufactureFacility(odUserId, facilityType);
            
            result.put("success", true);
            result.putAll(upgradeResult);
            result.put("message", "升级成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取配方列表
     */
    @GetMapping("/recipes/{facilityType}")
    public Map<String, Object> getRecipes(
            @RequestHeader("X-User-ID") String odUserId,
            @PathVariable String facilityType) {
        Map<String, Object> result = new HashMap<>();
        try {
            Production production = productionService.getProduction(odUserId);
            Integer facilityLevel = getFacilityLevel(production, facilityType);
            List<Recipe> recipes = productionService.getRecipes(facilityType, facilityLevel);
            UserResource resource = userResourceService.getUserResource(odUserId);
            
            result.put("success", true);
            result.put("recipes", recipes);
            result.put("facilityLevel", facilityLevel);
            result.put("resource", resource);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 制造物品
     */
    @PostMapping("/manufacture")
    public Map<String, Object> manufacture(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String recipeId = (String) body.get("recipeId");
            Map<String, Object> manufactureResult = productionService.manufacture(odUserId, recipeId);
            
            result.put("success", true);
            result.putAll(manufactureResult);
            result.put("message", "制造成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    private Integer getFacilityLevel(Production production, String type) {
        switch (type) {
            case "arsenal": return production.getArsenal().getLevel();
            case "workshop": return production.getWorkshop().getLevel();
            case "academy": return production.getAcademy().getLevel();
            default: return 1;
        }
    }
}
