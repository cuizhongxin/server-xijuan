package com.tencent.wxcloudrun.service.refine;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.equipment.EquipmentService;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 精炼服务 - 装备强化、品质提升、套装融合、装备分解
 */
@Service
public class RefineService {

    private static final Logger logger = LoggerFactory.getLogger(RefineService.class);

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserResourceService userResourceService;

    // 强化等级上限
    private static final int MAX_ENHANCE_LEVEL = 10;

    // 强化成功率（按等级）
    private static final double[] ENHANCE_SUCCESS_RATE = {
        1.0,   // 0 -> 1: 100%
        0.95,  // 1 -> 2: 95%
        0.90,  // 2 -> 3: 90%
        0.80,  // 3 -> 4: 80%
        0.70,  // 4 -> 5: 70%
        0.60,  // 5 -> 6: 60%
        0.50,  // 6 -> 7: 50%
        0.40,  // 7 -> 8: 40%
        0.30,  // 8 -> 9: 30%
        0.20   // 9 -> 10: 20%
    };

    // 强化消耗银币（按等级）
    private static final int[] ENHANCE_SILVER_COST = {
        1000,   // 0 -> 1
        2000,   // 1 -> 2
        4000,   // 2 -> 3
        8000,   // 3 -> 4
        15000,  // 4 -> 5
        25000,  // 5 -> 6
        40000,  // 6 -> 7
        60000,  // 7 -> 8
        80000,  // 8 -> 9
        100000  // 9 -> 10
    };

    // 强化消耗强化石等级（按装备强化等级）
    private static final int[] ENHANCE_STONE_LEVEL = {
        1, 1, 2, 2, 3, 3, 4, 4, 5, 6
    };

    // 强化属性加成（每级百分比）
    private static final double ENHANCE_BONUS_PER_LEVEL = 0.05; // 每级5%

    // 品质提升消耗品质石
    private static final int QUALITY_STONE_COST = 5;

    // 品质提升消耗银币
    private static final int QUALITY_SILVER_COST = 5000;

    // 品质提升随机范围
    private static final int QUALITY_UPGRADE_MIN = 1;
    private static final int QUALITY_UPGRADE_MAX = 5;

    // 套装定义
    private static final Map<String, SetDefinition> SET_DEFINITIONS = new HashMap<>();

    static {
        // 折冲套装
        SET_DEFINITIONS.put("zhechong", new SetDefinition(
            "zhechong", "折冲", 20,
            "攻击+30", Equipment.Attributes.builder().attack(30).build(),
            "攻击+79 士兵生命+24", Equipment.Attributes.builder().attack(79).hp(24).build()
        ));
        
        // 破军套装
        SET_DEFINITIONS.put("pojun", new SetDefinition(
            "pojun", "破军", 40,
            "攻击+60", Equipment.Attributes.builder().attack(60).build(),
            "攻击+150 暴击率+10%", Equipment.Attributes.builder().attack(150).critRate(0.1).build()
        ));
        
        // 龙胆套装
        SET_DEFINITIONS.put("longdan", new SetDefinition(
            "longdan", "龙胆", 60,
            "武勇+10 统御+10", Equipment.Attributes.builder().valor(10).command(10).build(),
            "攻击+200 防御+150 暴击伤害+20%", Equipment.Attributes.builder().attack(200).defense(150).critDamage(0.2).build()
        ));
        
        // 霸王套装
        SET_DEFINITIONS.put("bawang", new SetDefinition(
            "bawang", "霸王", 80,
            "攻击+100 防御+50", Equipment.Attributes.builder().attack(100).defense(50).build(),
            "攻击+300 防御+200 生命+500", Equipment.Attributes.builder().attack(300).defense(200).hp(500).build()
        ));
        
        // 天命套装
        SET_DEFINITIONS.put("tianming", new SetDefinition(
            "tianming", "天命", 100,
            "全属性+15", Equipment.Attributes.builder().attack(80).defense(80).valor(15).command(15).build(),
            "攻击+400 防御+300 暴击率+15% 暴击伤害+30%", 
            Equipment.Attributes.builder().attack(400).defense(300).critRate(0.15).critDamage(0.3).build()
        ));
    }

    /**
     * 获取强化信息
     */
    public Map<String, Object> getEnhanceInfo(String odUserId, String equipmentId) {
        Equipment equipment = equipmentService.getEquipment(odUserId, equipmentId);
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }

        int currentLevel = equipment.getEnhanceLevel() != null ? equipment.getEnhanceLevel() : 0;
        
        Map<String, Object> info = new HashMap<>();
        info.put("equipmentId", equipmentId);
        info.put("equipmentName", equipment.getName());
        info.put("currentLevel", currentLevel);
        info.put("maxLevel", MAX_ENHANCE_LEVEL);
        
        // 当前强化属性
        info.put("currentBonus", calculateEnhanceBonus(equipment, currentLevel));
        
        if (currentLevel < MAX_ENHANCE_LEVEL) {
            info.put("canEnhance", true);
            info.put("successRate", (int)(ENHANCE_SUCCESS_RATE[currentLevel] * 100));
            info.put("silverCost", ENHANCE_SILVER_COST[currentLevel]);
            info.put("stoneLevel", ENHANCE_STONE_LEVEL[currentLevel]);
            info.put("stoneCost", 1);
            info.put("nextBonus", calculateEnhanceBonus(equipment, currentLevel + 1));
        } else {
            info.put("canEnhance", false);
            info.put("message", "已达最高强化等级");
        }

        return info;
    }

    /**
     * 执行强化
     */
    public Map<String, Object> enhance(String odUserId, String equipmentId, boolean useProtect) {
        Equipment equipment = equipmentService.getEquipment(odUserId, equipmentId);
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }

        int currentLevel = equipment.getEnhanceLevel() != null ? equipment.getEnhanceLevel() : 0;
        
        if (currentLevel >= MAX_ENHANCE_LEVEL) {
            throw new BusinessException(400, "已达最高强化等级");
        }

        // 检查资源
        UserResource resource = userResourceService.getUserResource(odUserId);
        int silverCost = ENHANCE_SILVER_COST[currentLevel];
        int stoneLevel = ENHANCE_STONE_LEVEL[currentLevel];
        
        if (resource.getSilver() < silverCost) {
            throw new BusinessException(400, "银币不足");
        }
        
        int stoneCount = getEnhanceStoneCount(resource, stoneLevel);
        if (stoneCount < 1) {
            throw new BusinessException(400, stoneLevel + "级强化石不足");
        }

        // 扣除资源
        resource.setSilver(resource.getSilver() - silverCost);
        deductEnhanceStone(resource, stoneLevel, 1);
        
        // 保护符消耗
        if (useProtect && currentLevel >= 5) {
            // 检查保护符
            int scrollLevel = currentLevel >= 8 ? 3 : (currentLevel >= 6 ? 2 : 1);
            int scrollCount = getEnhanceScrollCount(resource, scrollLevel);
            if (scrollCount < 1) {
                throw new BusinessException(400, "保护符不足");
            }
            deductEnhanceScroll(resource, scrollLevel, 1);
        }

        // 判断成功
        double successRate = ENHANCE_SUCCESS_RATE[currentLevel];
        boolean success = Math.random() < successRate;
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        
        if (success) {
            // 强化成功
            int newLevel = currentLevel + 1;
            equipment.setEnhanceLevel(newLevel);
            equipment.setEnhanceAttributes(calculateEnhanceBonus(equipment, newLevel));
            result.put("newLevel", newLevel);
            result.put("levelDown", false);
            logger.info("装备强化成功: {} -> +{}", equipment.getName(), newLevel);
        } else {
            // 强化失败
            if (useProtect && currentLevel >= 5) {
                // 使用保护符，等级不变
                result.put("newLevel", currentLevel);
                result.put("levelDown", false);
                result.put("protected", true);
            } else if (currentLevel >= 7 && Math.random() < 0.3) {
                // 高等级有概率掉级
                int newLevel = currentLevel - 1;
                equipment.setEnhanceLevel(newLevel);
                equipment.setEnhanceAttributes(calculateEnhanceBonus(equipment, newLevel));
                result.put("newLevel", newLevel);
                result.put("levelDown", true);
            } else {
                result.put("newLevel", currentLevel);
                result.put("levelDown", false);
            }
            logger.info("装备强化失败: {} +{}", equipment.getName(), currentLevel);
        }

        // 保存
        equipmentService.saveEquipment(odUserId, equipment);
        userResourceService.saveUserResource(resource);

        return result;
    }

    /**
     * 获取品质信息
     */
    public Map<String, Object> getQualityInfo(String odUserId, String equipmentId) {
        Equipment equipment = equipmentService.getEquipment(odUserId, equipmentId);
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }

        int currentQuality = equipment.getQualityValue() != null ? equipment.getQualityValue() : 0;
        
        Map<String, Object> info = new HashMap<>();
        info.put("equipmentId", equipmentId);
        info.put("equipmentName", equipment.getName());
        info.put("currentQuality", currentQuality);
        info.put("maxQuality", 100);
        info.put("qualityDesc", getQualityDesc(currentQuality));
        info.put("currentBonus", calculateQualityBonus(equipment, currentQuality));
        
        if (currentQuality < 100) {
            info.put("canUpgrade", true);
            info.put("qualityStoneCost", QUALITY_STONE_COST);
            info.put("silverCost", QUALITY_SILVER_COST);
            info.put("upgradeRange", QUALITY_UPGRADE_MIN + "-" + QUALITY_UPGRADE_MAX + "%");
        } else {
            info.put("canUpgrade", false);
            info.put("message", "已达完美品质");
        }

        return info;
    }

    /**
     * 提升品质
     */
    public Map<String, Object> upgradeQuality(String odUserId, String equipmentId) {
        Equipment equipment = equipmentService.getEquipment(odUserId, equipmentId);
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }

        int currentQuality = equipment.getQualityValue() != null ? equipment.getQualityValue() : 0;
        
        if (currentQuality >= 100) {
            throw new BusinessException(400, "已达完美品质");
        }

        // 检查资源
        UserResource resource = userResourceService.getUserResource(odUserId);
        
        if (resource.getSilver() < QUALITY_SILVER_COST) {
            throw new BusinessException(400, "银币不足");
        }
        
        int qualityStone = resource.getQualityStone() != null ? resource.getQualityStone() : 0;
        if (qualityStone < QUALITY_STONE_COST) {
            throw new BusinessException(400, "品质石不足");
        }

        // 扣除资源
        resource.setSilver(resource.getSilver() - QUALITY_SILVER_COST);
        resource.setQualityStone(qualityStone - QUALITY_STONE_COST);

        // 随机提升品质
        int upgrade = QUALITY_UPGRADE_MIN + (int)(Math.random() * (QUALITY_UPGRADE_MAX - QUALITY_UPGRADE_MIN + 1));
        int newQuality = Math.min(100, currentQuality + upgrade);
        
        equipment.setQualityValue(newQuality);
        equipment.setQualityAttributes(calculateQualityBonus(equipment, newQuality));

        // 保存
        equipmentService.saveEquipment(odUserId, equipment);
        userResourceService.saveUserResource(resource);

        Map<String, Object> result = new HashMap<>();
        result.put("oldQuality", currentQuality);
        result.put("newQuality", newQuality);
        result.put("upgrade", upgrade);
        result.put("qualityDesc", getQualityDesc(newQuality));
        result.put("isPerfect", newQuality >= 100);
        result.put("newBonus", equipment.getQualityAttributes());

        logger.info("装备品质提升: {} {}% -> {}%", equipment.getName(), currentQuality, newQuality);
        
        return result;
    }

    /**
     * 获取套装信息
     */
    public Map<String, Object> getSetInfo(String setId) {
        SetDefinition def = SET_DEFINITIONS.get(setId);
        if (def == null) {
            throw new BusinessException(400, "套装不存在");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("setId", def.setId);
        info.put("setName", def.setName);
        info.put("setLevel", def.setLevel);
        info.put("threeSetEffect", def.threeSetEffect);
        info.put("threeSetBonus", def.threeSetBonus);
        info.put("sixSetEffect", def.sixSetEffect);
        info.put("sixSetBonus", def.sixSetBonus);

        return info;
    }

    /**
     * 套装融合
     */
    public Map<String, Object> fuseEquipments(String odUserId, List<String> equipmentIds, int targetSlotId) {
        if (equipmentIds == null || equipmentIds.size() != 3) {
            throw new BusinessException(400, "需要选择3件装备进行融合");
        }

        // 获取装备
        List<Equipment> equipments = new ArrayList<>();
        String setId = null;
        
        for (String id : equipmentIds) {
            Equipment eq = equipmentService.getEquipment(odUserId, id);
            if (eq == null) {
                throw new BusinessException(400, "装备不存在: " + id);
            }
            if (eq.getEquipped() != null && eq.getEquipped()) {
                throw new BusinessException(400, "已装备的装备不能融合");
            }
            if (eq.getSetInfo() == null || eq.getSetInfo().getSetId() == null) {
                throw new BusinessException(400, "非套装装备不能融合: " + eq.getName());
            }
            
            if (setId == null) {
                setId = eq.getSetInfo().getSetId();
            } else if (!setId.equals(eq.getSetInfo().getSetId())) {
                throw new BusinessException(400, "必须是同一套装的装备");
            }
            
            equipments.add(eq);
        }

        // 检查目标槽位
        if (targetSlotId < 1 || targetSlotId > 6) {
            throw new BusinessException(400, "无效的目标槽位");
        }

        // 检查融合费用
        UserResource resource = userResourceService.getUserResource(odUserId);
        int fuseCost = 10000; // 融合费用
        if (resource.getSilver() < fuseCost) {
            throw new BusinessException(400, "银币不足");
        }

        // 删除原装备
        for (Equipment eq : equipments) {
            equipmentService.deleteEquipment(odUserId, eq.getId());
        }

        // 创建新装备
        SetDefinition setDef = SET_DEFINITIONS.get(setId);
        String slotName = getSlotName(targetSlotId);
        String qualityPrefix = getRandomQualityPrefix();
        
        Equipment newEquipment = Equipment.builder()
            .id(UUID.randomUUID().toString())
            .userId(odUserId)
            .name(qualityPrefix + "的" + setDef.setName + slotName)
            .slotType(Equipment.SlotType.builder()
                .id(targetSlotId)
                .name(slotName)
                .build())
            .level(setDef.setLevel)
            .quality(getRandomQuality())
            .setInfo(Equipment.SetInfo.builder()
                .setId(setId)
                .setName(setDef.setName)
                .setLevel(setDef.setLevel)
                .threeSetEffect(setDef.threeSetEffect)
                .threeSetBonus(setDef.threeSetBonus)
                .sixSetEffect(setDef.sixSetEffect)
                .sixSetBonus(setDef.sixSetBonus)
                .build())
            .baseAttributes(generateBaseAttributes(setDef.setLevel, targetSlotId))
            .enhanceLevel(0)
            .qualityValue(0)
            .equipped(false)
            .locked(false)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();

        // 保存新装备
        equipmentService.saveEquipment(odUserId, newEquipment);

        // 扣除费用
        resource.setSilver(resource.getSilver() - fuseCost);
        userResourceService.saveUserResource(resource);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newEquipment", newEquipment);

        logger.info("套装融合成功: {} -> {}", setDef.setName, newEquipment.getName());

        return result;
    }

    /**
     * 装备分解
     */
    public Map<String, Object> decomposeEquipments(String odUserId, List<String> equipmentIds) {
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            throw new BusinessException(400, "请选择要分解的装备");
        }

        int totalQualityStone = 0;
        int totalSilver = 0;

        for (String id : equipmentIds) {
            Equipment eq = equipmentService.getEquipment(odUserId, id);
            if (eq == null) {
                continue;
            }
            if (eq.getEquipped() != null && eq.getEquipped()) {
                throw new BusinessException(400, "已装备的装备不能分解: " + eq.getName());
            }
            if (eq.getLocked() != null && eq.getLocked()) {
                throw new BusinessException(400, "已锁定的装备不能分解: " + eq.getName());
            }

            // 计算分解产出
            int qualityId = eq.getQuality() != null && eq.getQuality().getId() != null ? eq.getQuality().getId() : 1;
            int level = eq.getLevel() != null ? eq.getLevel() : 20;
            
            // 品质石 = 品质等级 * 1-3
            totalQualityStone += qualityId * (1 + (int)(Math.random() * 3));
            // 银币 = 等级 * 50-100
            totalSilver += level * (50 + (int)(Math.random() * 50));

            // 删除装备
            equipmentService.deleteEquipment(odUserId, id);
        }

        // 发放资源
        UserResource resource = userResourceService.getUserResource(odUserId);
        resource.setSilver(resource.getSilver() + totalSilver);
        int currentQualityStone = resource.getQualityStone() != null ? resource.getQualityStone() : 0;
        resource.setQualityStone(currentQualityStone + totalQualityStone);
        userResourceService.saveUserResource(resource);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("qualityStoneGained", totalQualityStone);
        result.put("silverGained", totalSilver);
        result.put("decomposedCount", equipmentIds.size());

        logger.info("装备分解: {} 件, 获得品质石 {}, 银币 {}", equipmentIds.size(), totalQualityStone, totalSilver);

        return result;
    }

    // ==================== 辅助方法 ====================

    private Equipment.Attributes calculateEnhanceBonus(Equipment equipment, int level) {
        if (level <= 0) {
            return Equipment.Attributes.builder().build();
        }
        
        Equipment.Attributes base = equipment.getBaseAttributes();
        if (base == null) {
            base = Equipment.Attributes.builder().attack(50).defense(30).build();
        }
        
        double multiplier = level * ENHANCE_BONUS_PER_LEVEL;
        
        return Equipment.Attributes.builder()
            .attack((int)(base.getAttack() * multiplier))
            .defense((int)(base.getDefense() * multiplier))
            .valor((int)(base.getValor() * multiplier))
            .command((int)(base.getCommand() * multiplier))
            .hp((int)(base.getHp() * multiplier))
            .mobility((int)(base.getMobility() * multiplier))
            .build();
    }

    private Equipment.Attributes calculateQualityBonus(Equipment equipment, int qualityValue) {
        if (qualityValue <= 0) {
            return Equipment.Attributes.builder().build();
        }
        
        Equipment.Attributes base = equipment.getBaseAttributes();
        if (base == null) {
            base = Equipment.Attributes.builder().attack(50).defense(30).build();
        }
        
        // 品质值每点提供0.5%属性加成
        double multiplier = qualityValue * 0.005;
        
        return Equipment.Attributes.builder()
            .attack((int)(base.getAttack() * multiplier))
            .defense((int)(base.getDefense() * multiplier))
            .valor((int)(base.getValor() * multiplier))
            .command((int)(base.getCommand() * multiplier))
            .hp((int)(base.getHp() * multiplier))
            .build();
    }

    private String getQualityDesc(int qualityValue) {
        if (qualityValue >= 100) return "完美";
        if (qualityValue >= 80) return "精良";
        if (qualityValue >= 60) return "优秀";
        if (qualityValue >= 40) return "良好";
        if (qualityValue >= 20) return "普通";
        return "粗糙";
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

    private void deductEnhanceStone(UserResource resource, int level, int count) {
        switch (level) {
            case 1: resource.setEnhanceStone1(resource.getEnhanceStone1() - count); break;
            case 2: resource.setEnhanceStone2(resource.getEnhanceStone2() - count); break;
            case 3: resource.setEnhanceStone3(resource.getEnhanceStone3() - count); break;
            case 4: resource.setEnhanceStone4(resource.getEnhanceStone4() - count); break;
            case 5: resource.setEnhanceStone5(resource.getEnhanceStone5() - count); break;
            case 6: resource.setEnhanceStone6(resource.getEnhanceStone6() - count); break;
        }
    }

    private int getEnhanceScrollCount(UserResource resource, int level) {
        switch (level) {
            case 1: return resource.getEnhanceScrollBasic() != null ? resource.getEnhanceScrollBasic() : 0;
            case 2: return resource.getEnhanceScrollMedium() != null ? resource.getEnhanceScrollMedium() : 0;
            case 3: return resource.getEnhanceScrollAdvanced() != null ? resource.getEnhanceScrollAdvanced() : 0;
            default: return 0;
        }
    }

    private void deductEnhanceScroll(UserResource resource, int level, int count) {
        switch (level) {
            case 1: resource.setEnhanceScrollBasic(resource.getEnhanceScrollBasic() - count); break;
            case 2: resource.setEnhanceScrollMedium(resource.getEnhanceScrollMedium() - count); break;
            case 3: resource.setEnhanceScrollAdvanced(resource.getEnhanceScrollAdvanced() - count); break;
        }
    }

    private String getSlotName(int slotId) {
        switch (slotId) {
            case 1: return "剑";
            case 2: return "盔";
            case 3: return "铠";
            case 4: return "戒指";
            case 5: return "鞋";
            case 6: return "项链";
            default: return "装备";
        }
    }

    private String getRandomQualityPrefix() {
        String[] prefixes = {"粗糙", "普通", "精良", "优秀"};
        return prefixes[(int)(Math.random() * prefixes.length)];
    }

    private Equipment.Quality getRandomQuality() {
        double rand = Math.random();
        if (rand < 0.4) {
            return Equipment.Quality.builder().id(2).name("绿色").color("#00ff00").multiplier(1.0).build();
        } else if (rand < 0.7) {
            return Equipment.Quality.builder().id(3).name("蓝色").color("#0088ff").multiplier(1.2).build();
        } else if (rand < 0.9) {
            return Equipment.Quality.builder().id(4).name("紫色").color("#9900ff").multiplier(1.5).build();
        } else {
            return Equipment.Quality.builder().id(5).name("橙色").color("#ff8800").multiplier(2.0).build();
        }
    }

    private Equipment.Attributes generateBaseAttributes(int level, int slotId) {
        int baseValue = level * 5;
        
        Equipment.Attributes.AttributesBuilder builder = Equipment.Attributes.builder();
        
        switch (slotId) {
            case 1: // 武器 - 主攻击
                builder.attack(baseValue * 2).valor(baseValue / 2);
                break;
            case 2: // 头盔 - 主防御
                builder.defense(baseValue).hp(baseValue * 3);
                break;
            case 3: // 铠甲 - 主防御
                builder.defense(baseValue * 2).hp(baseValue * 2);
                break;
            case 4: // 戒指 - 主攻击
                builder.attack(baseValue).critRate(0.02);
                break;
            case 5: // 鞋子 - 主机动
                builder.mobility(baseValue / 3).dodge(0.02);
                break;
            case 6: // 项链 - 主统御
                builder.command(baseValue / 2).hp(baseValue * 2);
                break;
        }
        
        return builder.build();
    }

    /**
     * 套装定义内部类
     */
    private static class SetDefinition {
        String setId;
        String setName;
        int setLevel;
        String threeSetEffect;
        Equipment.Attributes threeSetBonus;
        String sixSetEffect;
        Equipment.Attributes sixSetBonus;

        SetDefinition(String setId, String setName, int setLevel,
                     String threeSetEffect, Equipment.Attributes threeSetBonus,
                     String sixSetEffect, Equipment.Attributes sixSetBonus) {
            this.setId = setId;
            this.setName = setName;
            this.setLevel = setLevel;
            this.threeSetEffect = threeSetEffect;
            this.threeSetBonus = threeSetBonus;
            this.sixSetEffect = sixSetEffect;
            this.sixSetBonus = sixSetBonus;
        }
    }
}
