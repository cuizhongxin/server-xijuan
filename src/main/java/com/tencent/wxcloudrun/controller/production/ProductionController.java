package com.tencent.wxcloudrun.controller.production;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.Production;
import com.tencent.wxcloudrun.model.Production.Recipe;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.production.ProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
    
    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
    
    /**
     * 获取生产数据
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getProductionInfo(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            Production production = productionService.getProduction(userId);
            UserResource resource = userResourceService.getUserResource(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("production", production);
            data.put("resource", resource);

            // 前端兼容: facilities数组 + resources对象
            List<Map<String, Object>> facilities = new java.util.ArrayList<>();
            String[][] facilityDefs = {{"silver", "白银矿"}, {"metal", "金属矿"}, {"food", "农场"}, {"paper", "造纸坊"}};
            for (String[] fd : facilityDefs) {
                Production.Facility fac = getFacility(production, fd[0]);
                if (fac != null) {
                    Map<String, Object> fm = new HashMap<>();
                    fm.put("type", fd[0]);
                    fm.put("label", fd[1]);
                    fm.put("level", fac.getLevel());
                    fm.put("outputPerTime", fac.getOutputPerTime());
                    fm.put("dailyLimit", fac.getDailyLimit());
                    fm.put("usedToday", fac.getUsedToday());
                    facilities.add(fm);
                }
            }
            data.put("facilities", facilities);

            Map<String, Object> resources = new HashMap<>();
            resources.put("silver", resource.getSilver());
            resources.put("metal", resource.getMetal());
            resources.put("food", resource.getFood());
            resources.put("paper", resource.getPaper());
            data.put("resources", resources);

            data.put("playerLevel", productionService.getPlayerLevel(userId));

            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取生产信息异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    private Production.Facility getFacility(Production production, String type) {
        switch (type) {
            case "silver": return production.getSilverMine();
            case "metal": return production.getMetalMine();
            case "food": return production.getFarm();
            case "paper": return production.getPaperMill();
            default: return null;
        }
    }
    
    /**
     * 生产资源
     */
    @PostMapping("/produce")
    public ApiResponse<Map<String, Object>> produce(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String facilityType = (String) body.get("facilityType");
            Map<String, Object> produceResult = productionService.produce(userId, facilityType);
            
            produceResult.put("message", "生产成功，获得 " + produceResult.get("output") + " 资源");
            return ApiResponse.success(produceResult);
        } catch (Exception e) {
            log.error("生产异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 升级生产设施
     */
    @PostMapping("/upgrade-facility")
    public ApiResponse<Map<String, Object>> upgradeFacility(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String facilityType = (String) body.get("facilityType");
            Map<String, Object> upgradeResult = productionService.upgradeFacility(userId, facilityType);
            
            upgradeResult.put("message", "升级成功");
            return ApiResponse.success(upgradeResult);
        } catch (Exception e) {
            log.error("升级生产设施异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 升级制造设施
     */
    @PostMapping("/upgrade-manufacture")
    public ApiResponse<Map<String, Object>> upgradeManufactureFacility(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String facilityType = (String) body.get("facilityType");
            Map<String, Object> upgradeResult = productionService.upgradeManufactureFacility(userId, facilityType);
            
            upgradeResult.put("message", "升级成功");
            return ApiResponse.success(upgradeResult);
        } catch (Exception e) {
            log.error("升级制造设施异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取配方列表
     */
    @GetMapping("/recipes/{facilityType}")
    public ApiResponse<Map<String, Object>> getRecipes(
            HttpServletRequest request,
            @PathVariable String facilityType) {
        try {
            String userId = getUserId(request);
            Production production = productionService.getProduction(userId);
            Integer facilityLevel = getFacilityLevel(production, facilityType);
            List<Recipe> recipes = productionService.getRecipes(facilityType, facilityLevel);
            UserResource resource = userResourceService.getUserResource(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("recipes", recipes);
            data.put("playerLevel", productionService.getPlayerLevel(userId));
            data.put("resource", resource);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取配方列表异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 制造物品
     */
    @PostMapping("/manufacture")
    public ApiResponse<Map<String, Object>> manufacture(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String recipeId = (String) body.get("recipeId");
            Map<String, Object> manufactureResult = productionService.manufacture(userId, recipeId);
            
            manufactureResult.put("message", "制造成功");
            return ApiResponse.success(manufactureResult);
        } catch (Exception e) {
            log.error("制造物品异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/convert")
    public ApiResponse<Map<String, Object>> convertResource(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String source = (String) body.get("source");
            String target = (String) body.get("target");
            int amount = ((Number) body.get("amount")).intValue();
            boolean lossless = Boolean.TRUE.equals(body.get("lossless"));
            return ApiResponse.success(productionService.convertResource(userId, source, target, amount, lossless));
        } catch (Exception e) {
            log.error("资源转换异常", e);
            return ApiResponse.error(e.getMessage());
        }
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
