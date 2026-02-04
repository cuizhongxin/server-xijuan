package com.tencent.wxcloudrun.service.enhance;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 强化服务
 */
@Service
public class EnhanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhanceService.class);
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private UserResourceService userResourceService;
    
    // 强化等级对应的机动性加成
    private static final Map<Integer, Integer> MOBILITY_BONUS = new HashMap<>();
    static {
        MOBILITY_BONUS.put(4, 1);
        MOBILITY_BONUS.put(6, 2);
        MOBILITY_BONUS.put(8, 4);
        MOBILITY_BONUS.put(10, 8);
    }
    
    // 每级强化需要的强化石等级和数量
    private static final Map<Integer, Map<String, Integer>> ENHANCE_COST = new HashMap<>();
    static {
        // 1-3级用1级强化石
        for (int i = 1; i <= 3; i++) {
            Map<String, Integer> cost = new HashMap<>();
            cost.put("stoneLevel", 1);
            cost.put("stoneCount", i);
            cost.put("silver", 100 * i);
            ENHANCE_COST.put(i, cost);
        }
        // 4-5级用2级强化石
        for (int i = 4; i <= 5; i++) {
            Map<String, Integer> cost = new HashMap<>();
            cost.put("stoneLevel", 2);
            cost.put("stoneCount", i - 2);
            cost.put("silver", 200 * i);
            ENHANCE_COST.put(i, cost);
        }
        // 6-7级用3级强化石
        for (int i = 6; i <= 7; i++) {
            Map<String, Integer> cost = new HashMap<>();
            cost.put("stoneLevel", 3);
            cost.put("stoneCount", i - 4);
            cost.put("silver", 500 * i);
            ENHANCE_COST.put(i, cost);
        }
        // 8-9级用4级强化石
        for (int i = 8; i <= 9; i++) {
            Map<String, Integer> cost = new HashMap<>();
            cost.put("stoneLevel", 4);
            cost.put("stoneCount", i - 6);
            cost.put("silver", 1000 * i);
            ENHANCE_COST.put(i, cost);
        }
        // 10级用5级强化石
        Map<String, Integer> cost = new HashMap<>();
        cost.put("stoneLevel", 5);
        cost.put("stoneCount", 3);
        cost.put("silver", 5000);
        ENHANCE_COST.put(10, cost);
    }
    
    /**
     * 强化装备
     */
    public Map<String, Object> enhanceEquipment(String userId, String equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId);
        if (equipment == null || !equipment.getUserId().equals(userId)) {
            throw new BusinessException(400, "装备不存在或不属于该用户");
        }
        
        int currentLevel = equipment.getEnhanceLevel() != null ? equipment.getEnhanceLevel() : 0;
        if (currentLevel >= 10) {
            throw new BusinessException(400, "装备已达到最高强化等级");
        }
        
        int nextLevel = currentLevel + 1;
        Map<String, Integer> cost = ENHANCE_COST.get(nextLevel);
        if (cost == null) {
            throw new BusinessException(400, "无效的强化等级");
        }
        
        // 检查资源
        UserResource resource = userResourceService.getUserResource(userId);
        int stoneLevel = cost.get("stoneLevel");
        int stoneCount = cost.get("stoneCount");
        long silverCost = cost.get("silver");
        
        int currentStones = getEnhanceStoneCount(resource, stoneLevel);
        if (currentStones < stoneCount) {
            throw new BusinessException(400, "强化石不足，需要" + stoneCount + "个" + stoneLevel + "级强化石");
        }
        
        if (resource.getSilver() < silverCost) {
            throw new BusinessException(400, "银两不足，需要" + silverCost + "银两");
        }
        
        // 消耗资源
        consumeEnhanceStone(resource, stoneLevel, stoneCount);
        resource.setSilver(resource.getSilver() - silverCost);
        userResourceService.saveUserResource(resource);
        
        // 强化装备
        equipment.setEnhanceLevel(nextLevel);
        equipment.setEnhanceAttributes(calculateEnhanceAttributes(nextLevel, equipment));
        equipment.setUpdateTime(System.currentTimeMillis());
        equipmentRepository.update(equipment);
        
        logger.info("用户 {} 强化装备 {} 到 +{}", userId, equipment.getName(), nextLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("equipment", equipment);
        result.put("enhanceLevel", nextLevel);
        result.put("mobilityBonus", MOBILITY_BONUS.getOrDefault(nextLevel, 0));
        result.put("consumedStones", stoneCount);
        result.put("consumedSilver", silverCost);
        result.put("remainingSilver", resource.getSilver());
        
        return result;
    }
    
    /**
     * 转移强化（将强化等级转移到另一件装备）
     */
    public Map<String, Object> transferEnhance(String userId, String fromEquipmentId, String toEquipmentId) {
        Equipment fromEquipment = equipmentRepository.findById(fromEquipmentId);
        Equipment toEquipment = equipmentRepository.findById(toEquipmentId);
        
        if (fromEquipment == null || !fromEquipment.getUserId().equals(userId)) {
            throw new BusinessException(400, "源装备不存在或不属于该用户");
        }
        if (toEquipment == null || !toEquipment.getUserId().equals(userId)) {
            throw new BusinessException(400, "目标装备不存在或不属于该用户");
        }
        
        int fromLevel = fromEquipment.getEnhanceLevel() != null ? fromEquipment.getEnhanceLevel() : 0;
        if (fromLevel == 0) {
            throw new BusinessException(400, "源装备未强化");
        }
        
        int toLevel = toEquipment.getEnhanceLevel() != null ? toEquipment.getEnhanceLevel() : 0;
        if (toLevel > 0) {
            throw new BusinessException(400, "目标装备已有强化，请先清除");
        }
        
        // 确定需要的强化符
        String scrollType;
        if (fromLevel >= 8) {
            scrollType = "advanced";
        } else if (fromLevel >= 6) {
            scrollType = "medium";
        } else {
            scrollType = "basic";
        }
        
        // 检查并消耗强化符
        UserResource resource = userResourceService.getUserResource(userId);
        int scrollCount = getEnhanceScrollCount(resource, scrollType);
        if (scrollCount < 1) {
            throw new BusinessException(400, "强化符不足，需要" + getScrollName(scrollType));
        }
        
        consumeEnhanceScroll(resource, scrollType, 1);
        userResourceService.saveUserResource(resource);
        
        // 转移强化
        toEquipment.setEnhanceLevel(fromLevel);
        toEquipment.setEnhanceAttributes(calculateEnhanceAttributes(fromLevel, toEquipment));
        toEquipment.setUpdateTime(System.currentTimeMillis());
        
        fromEquipment.setEnhanceLevel(0);
        fromEquipment.setEnhanceAttributes(null);
        fromEquipment.setUpdateTime(System.currentTimeMillis());
        
        equipmentRepository.update(fromEquipment);
        equipmentRepository.update(toEquipment);
        
        logger.info("用户 {} 将装备 {} 的强化 +{} 转移到 {}", userId, fromEquipment.getName(), fromLevel, toEquipment.getName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("fromEquipment", fromEquipment);
        result.put("toEquipment", toEquipment);
        result.put("transferredLevel", fromLevel);
        result.put("consumedScroll", scrollType);
        
        return result;
    }
    
    /**
     * 合成强化石（3个同等级 + 合成符 = 1个更高级）
     */
    public Map<String, Object> mergeEnhanceStones(String userId, int stoneLevel, int count) {
        if (stoneLevel < 1 || stoneLevel >= 6) {
            throw new BusinessException(400, "只能合成1-5级强化石");
        }
        
        if (count < 3) {
            throw new BusinessException(400, "至少需要3个强化石才能合成");
        }
        
        int mergeCount = count / 3;  // 可以合成的次数
        int nextLevel = stoneLevel + 1;
        
        // 检查资源
        UserResource resource = userResourceService.getUserResource(userId);
        int currentStones = getEnhanceStoneCount(resource, stoneLevel);
        if (currentStones < count) {
            throw new BusinessException(400, "强化石不足，当前有" + currentStones + "个");
        }
        
        int mergeScrolls = resource.getMergeScroll() != null ? resource.getMergeScroll() : 0;
        if (mergeScrolls < mergeCount) {
            throw new BusinessException(400, "合成符不足，需要" + mergeCount + "个");
        }
        
        // 消耗资源
        consumeEnhanceStone(resource, stoneLevel, mergeCount * 3);
        resource.setMergeScroll(mergeScrolls - mergeCount);
        
        // 增加高级强化石
        addEnhanceStone(resource, nextLevel, mergeCount);
        userResourceService.saveUserResource(resource);
        
        logger.info("用户 {} 合成 {} 个 {} 级强化石为 {} 个 {} 级强化石", 
            userId, mergeCount * 3, stoneLevel, mergeCount, nextLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("consumedStones", mergeCount * 3);
        result.put("consumedScrolls", mergeCount);
        result.put("gainedStones", mergeCount);
        result.put("stoneLevel", nextLevel);
        result.put("remainingStones", getEnhanceStoneCount(resource, stoneLevel));
        result.put("remainingScrolls", resource.getMergeScroll());
        
        return result;
    }
    
    /**
     * 计算强化属性加成
     */
    private Equipment.Attributes calculateEnhanceAttributes(int enhanceLevel, Equipment equipment) {
        Equipment.Attributes attributes = Equipment.Attributes.builder().build();
        
        // 基础属性加成（每级+5%）
        double multiplier = 1.0 + enhanceLevel * 0.05;
        if (equipment.getBaseAttributes() != null) {
            int baseAttack = equipment.getBaseAttributes().getAttack() != null ? equipment.getBaseAttributes().getAttack() : 0;
            int baseDefense = equipment.getBaseAttributes().getDefense() != null ? equipment.getBaseAttributes().getDefense() : 0;
            int baseValor = equipment.getBaseAttributes().getValor() != null ? equipment.getBaseAttributes().getValor() : 0;
            int baseCommand = equipment.getBaseAttributes().getCommand() != null ? equipment.getBaseAttributes().getCommand() : 0;
            
            attributes.setAttack((int)(baseAttack * multiplier));
            attributes.setDefense((int)(baseDefense * multiplier));
            attributes.setValor((int)(baseValor * multiplier));
            attributes.setCommand((int)(baseCommand * multiplier));
        }
        
        // 机动性加成（特定等级）
        if (MOBILITY_BONUS.containsKey(enhanceLevel)) {
            attributes.setMobility(MOBILITY_BONUS.get(enhanceLevel));
        }
        
        // 套装奖励增强（每级+2%）
        if (equipment.getSetInfo() != null) {
            // 套装属性也会被强化影响
            double setMultiplier = 1.0 + enhanceLevel * 0.02;
            // 这里可以进一步处理套装属性
        }
        
        return attributes;
    }
    
    private int getEnhanceStoneCount(UserResource resource, int level) {
        switch (level) {
            case 1: return resource.getEnhanceStone1() != null ? resource.getEnhanceStone1() : 0;
            case 2: return resource.getEnhanceStone2() != null ? resource.getEnhanceStone2() : 0;
            case 3: return resource.getEnhanceStone3() != null ? resource.getEnhanceStone3() : 0;
            case 4: return resource.getEnhanceStone4() != null ? resource.getEnhanceStone4() : 0;
            case 5: return resource.getEnhanceStone5() != null ? resource.getEnhanceStone5() : 0;
            case 6: return resource.getEnhanceStone6() != null ? resource.getEnhanceStone6() : 0;
            default: return 0;
        }
    }
    
    private void consumeEnhanceStone(UserResource resource, int level, int count) {
        switch (level) {
            case 1: resource.setEnhanceStone1((resource.getEnhanceStone1() != null ? resource.getEnhanceStone1() : 0) - count); break;
            case 2: resource.setEnhanceStone2((resource.getEnhanceStone2() != null ? resource.getEnhanceStone2() : 0) - count); break;
            case 3: resource.setEnhanceStone3((resource.getEnhanceStone3() != null ? resource.getEnhanceStone3() : 0) - count); break;
            case 4: resource.setEnhanceStone4((resource.getEnhanceStone4() != null ? resource.getEnhanceStone4() : 0) - count); break;
            case 5: resource.setEnhanceStone5((resource.getEnhanceStone5() != null ? resource.getEnhanceStone5() : 0) - count); break;
            case 6: resource.setEnhanceStone6((resource.getEnhanceStone6() != null ? resource.getEnhanceStone6() : 0) - count); break;
        }
    }
    
    private void addEnhanceStone(UserResource resource, int level, int count) {
        switch (level) {
            case 1: resource.setEnhanceStone1((resource.getEnhanceStone1() != null ? resource.getEnhanceStone1() : 0) + count); break;
            case 2: resource.setEnhanceStone2((resource.getEnhanceStone2() != null ? resource.getEnhanceStone2() : 0) + count); break;
            case 3: resource.setEnhanceStone3((resource.getEnhanceStone3() != null ? resource.getEnhanceStone3() : 0) + count); break;
            case 4: resource.setEnhanceStone4((resource.getEnhanceStone4() != null ? resource.getEnhanceStone4() : 0) + count); break;
            case 5: resource.setEnhanceStone5((resource.getEnhanceStone5() != null ? resource.getEnhanceStone5() : 0) + count); break;
            case 6: resource.setEnhanceStone6((resource.getEnhanceStone6() != null ? resource.getEnhanceStone6() : 0) + count); break;
        }
    }
    
    private int getEnhanceScrollCount(UserResource resource, String type) {
        switch (type) {
            case "basic": return resource.getEnhanceScrollBasic() != null ? resource.getEnhanceScrollBasic() : 0;
            case "medium": return resource.getEnhanceScrollMedium() != null ? resource.getEnhanceScrollMedium() : 0;
            case "advanced": return resource.getEnhanceScrollAdvanced() != null ? resource.getEnhanceScrollAdvanced() : 0;
            default: return 0;
        }
    }
    
    private void consumeEnhanceScroll(UserResource resource, String type, int count) {
        switch (type) {
            case "basic": resource.setEnhanceScrollBasic((resource.getEnhanceScrollBasic() != null ? resource.getEnhanceScrollBasic() : 0) - count); break;
            case "medium": resource.setEnhanceScrollMedium((resource.getEnhanceScrollMedium() != null ? resource.getEnhanceScrollMedium() : 0) - count); break;
            case "advanced": resource.setEnhanceScrollAdvanced((resource.getEnhanceScrollAdvanced() != null ? resource.getEnhanceScrollAdvanced() : 0) - count); break;
        }
    }
    
    private String getScrollName(String type) {
        switch (type) {
            case "basic": return "初级强化符";
            case "medium": return "中级强化符";
            case "advanced": return "高级强化符";
            default: return "强化符";
        }
    }
}
