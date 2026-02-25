package com.tencent.wxcloudrun.controller.equipment;

import com.tencent.wxcloudrun.config.EquipmentConfig;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.SecretRealm;
import com.tencent.wxcloudrun.model.CraftMaterial;
import com.tencent.wxcloudrun.service.equipment.EquipmentService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 装备控制器
 */
@RestController
@RequestMapping("/equipment")
public class EquipmentController {
    
    private static final Logger logger = LoggerFactory.getLogger(EquipmentController.class);
    
    @Autowired
    private EquipmentService equipmentService;
    
    @Autowired
    private EquipmentConfig equipmentConfig;
    
    @Autowired
    private GeneralService generalService;
    
    // ==================== 装备查询 ====================
    
    /**
     * 获取用户所有装备
     */
    @GetMapping("/list")
    public ApiResponse<List<Equipment>> getUserEquipments(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取用户装备列表, userId: {}", userId);
        
        List<Equipment> equipments = equipmentService.getUserEquipments(userId);
        
        return ApiResponse.success(equipments);
    }
    
    /**
     * 获取用户背包中的装备（未装备）
     */
    @GetMapping("/bag")
    public ApiResponse<List<Equipment>> getBagEquipments(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取用户背包装备, userId: {}", userId);
        
        List<Equipment> equipments = equipmentService.getUserBagEquipments(userId);
        
        return ApiResponse.success(equipments);
    }
    
    /**
     * 获取武将已装备的装备
     */
    @GetMapping("/general/{generalId}")
    public ApiResponse<Map<String, Object>> getGeneralEquipments(@PathVariable String generalId,
                                                                 HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取武将装备, userId: {}, generalId: {}", userId, generalId);
        
        List<Equipment> equipments = equipmentService.getGeneralEquipments(generalId);
        Map<String, Object> setBonus = equipmentService.calculateSetBonus(generalId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("equipments", equipments);
        result.put("setBonus", setBonus);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取装备详情
     */
    @GetMapping("/{equipmentId}")
    public ApiResponse<Equipment> getEquipment(@PathVariable String equipmentId,
                                               HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取装备详情, userId: {}, equipmentId: {}", userId, equipmentId);
        
        Equipment equipment = equipmentService.getEquipmentById(equipmentId);
        
        if (equipment == null) {
            return ApiResponse.error(404, "装备不存在");
        }
        
        return ApiResponse.success(equipment);
    }
    
    // ==================== 装备操作 ====================
    
    /**
     * 装备到武将（带等级检查）
     */
    @PostMapping("/equip")
    public ApiResponse<Equipment> equipToGeneral(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String equipmentId = (String) body.get("equipmentId");
        String generalId = (String) body.get("generalId");
        
        logger.info("装备穿戴, userId: {}, equipmentId: {}, generalId: {}", 
                   userId, equipmentId, generalId);
        
        // 获取武将等级
        General general = generalService.getGeneralById(generalId);
        if (general == null) {
            return ApiResponse.error(400, "武将不存在");
        }
        int generalLevel = general.getLevel() != null ? general.getLevel() : 1;
        
        Equipment equipment = equipmentService.equipToGeneral(userId, equipmentId, generalId, generalLevel);
        
        return ApiResponse.success(equipment);
    }
    
    /**
     * 卸下装备
     */
    @PostMapping("/unequip")
    public ApiResponse<Equipment> unequip(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String equipmentId = (String) body.get("equipmentId");
        
        logger.info("卸下装备, userId: {}, equipmentId: {}", userId, equipmentId);
        
        Equipment equipment = equipmentService.unequip(userId, equipmentId);
        
        return ApiResponse.success(equipment);
    }
    
    // ==================== 装备制作 ====================
    
    /**
     * 获取制作所需材料（旧接口）
     */
    @GetMapping("/craft/materials")
    public ApiResponse<Map<String, Integer>> getCraftMaterials(@RequestParam Integer slotTypeId,
                                                               @RequestParam Integer level,
                                                               HttpServletRequest request) {
        logger.info("获取制作材料需求, slotTypeId: {}, level: {}", slotTypeId, level);
        Map<String, Integer> materials = equipmentService.getCraftMaterials(slotTypeId, level);
        return ApiResponse.success(materials);
    }
    
    /**
     * 制作装备（旧接口）
     */
    @PostMapping("/craft")
    public ApiResponse<Equipment> craftEquipment(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        Integer slotTypeId = (Integer) body.get("slotTypeId");
        Integer level = (Integer) body.get("level");
        
        logger.info("制作装备, userId: {}, slotTypeId: {}, level: {}", userId, slotTypeId, level);
        Equipment equipment = equipmentService.craftEquipment(userId, slotTypeId, level);
        return ApiResponse.success(equipment);
    }

    // ==================== 军械局制作（基于 equipment_pre）====================

    /**
     * 获取军械局可制作的装备列表
     */
    @GetMapping("/arsenal/list")
    public ApiResponse<List<Map<String, Object>>> getArsenalList(HttpServletRequest request) {
        logger.info("获取军械局可制作装备列表");
        List<Map<String, Object>> list = equipmentService.getCraftableEquipmentList();
        return ApiResponse.success(list);
    }

    /**
     * 获取军械局装备制作消耗
     */
    @GetMapping("/arsenal/cost")
    public ApiResponse<Map<String, Integer>> getArsenalCost(@RequestParam Integer level,
                                                             HttpServletRequest request) {
        logger.info("获取军械局制作消耗, level: {}", level);
        Map<String, Integer> cost = equipmentService.getArsenalCraftCost(level);
        return ApiResponse.success(cost);
    }

    /**
     * 军械局制作装备
     */
    @PostMapping("/arsenal/craft")
    public ApiResponse<Equipment> arsenalCraft(@RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        Integer preId = (Integer) body.get("preId");

        logger.info("军械局制作装备, userId: {}, preId: {}", userId, preId);
        Equipment equipment = equipmentService.craftByPreId(userId, preId);
        return ApiResponse.success(equipment);
    }
    
    // ==================== 秘境寻宝 ====================
    
    /**
     * 获取所有秘境
     */
    @GetMapping("/secret-realm/list")
    public ApiResponse<Map<String, SecretRealm>> getAllSecretRealms(HttpServletRequest request) {
        logger.info("获取所有秘境");
        
        Map<String, SecretRealm> realms = equipmentService.getAllSecretRealms();
        
        return ApiResponse.success(realms);
    }
    
    /**
     * 获取已解锁的秘境
     */
    @GetMapping("/secret-realm/unlocked")
    public ApiResponse<List<SecretRealm>> getUnlockedSecretRealms(@RequestParam Integer playerLevel,
                                                                  HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取已解锁秘境, userId: {}, playerLevel: {}", userId, playerLevel);
        
        List<SecretRealm> realms = equipmentService.getUnlockedSecretRealms(playerLevel);
        
        return ApiResponse.success(realms);
    }
    
    /**
     * 秘境寻宝
     */
    @PostMapping("/secret-realm/explore")
    public ApiResponse<Equipment> exploreSecretRealm(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String realmId = (String) body.get("realmId");
        Integer playerLevel = (Integer) body.get("playerLevel");
        
        logger.info("秘境寻宝, userId: {}, realmId: {}, playerLevel: {}", userId, realmId, playerLevel);
        
        Equipment equipment = equipmentService.secretRealmExplore(userId, realmId, playerLevel);
        
        return ApiResponse.success(equipment);
    }
    
    // ==================== 材料管理 ====================
    
    /**
     * 获取用户材料
     */
    @GetMapping("/materials")
    public ApiResponse<Map<String, Integer>> getUserMaterials(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        logger.info("获取用户材料, userId: {}", userId);
        
        Map<String, Integer> materials = equipmentService.getUserMaterials(userId);
        
        return ApiResponse.success(materials);
    }
    
    /**
     * 获取材料配置
     */
    @GetMapping("/materials/config")
    public ApiResponse<Map<String, CraftMaterial>> getMaterialConfig(HttpServletRequest request) {
        logger.info("获取材料配置");
        
        Map<String, CraftMaterial> materials = equipmentConfig.getAllMaterials();
        
        return ApiResponse.success(materials);
    }
    
    // ==================== 配置查询 ====================
    
    /**
     * 获取装备槽位配置
     */
    @GetMapping("/config/slots")
    public ApiResponse<Map<Integer, Equipment.SlotType>> getSlotConfig(HttpServletRequest request) {
        logger.info("获取装备槽位配置");
        
        Map<Integer, Equipment.SlotType> slots = equipmentConfig.getAllSlotTypes();
        
        return ApiResponse.success(slots);
    }
    
    /**
     * 获取装备品质配置
     */
    @GetMapping("/config/qualities")
    public ApiResponse<Map<Integer, Equipment.Quality>> getQualityConfig(HttpServletRequest request) {
        logger.info("获取装备品质配置");
        
        Map<Integer, Equipment.Quality> qualities = equipmentConfig.getAllQualities();
        
        return ApiResponse.success(qualities);
    }
    
    /**
     * 获取套装配置
     */
    @GetMapping("/config/sets")
    public ApiResponse<Map<String, Equipment.SetInfo>> getSetConfig(HttpServletRequest request) {
        logger.info("获取套装配置");
        
        Map<String, Equipment.SetInfo> sets = equipmentConfig.getAllEquipmentSets();
        
        return ApiResponse.success(sets);
    }
}


