package com.tencent.wxcloudrun.service.refine;

import com.tencent.wxcloudrun.config.EquipmentConfig;
import com.tencent.wxcloudrun.dao.EquipmentPreMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.EquipmentPre;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.service.equipment.EquipmentService;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Random;

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

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private EquipmentConfig equipmentConfig;

    @Autowired
    private EquipmentPreMapper equipmentPreMapper;

    private static final int MAX_ENHANCE_LEVEL = 20;

    // 强化石 item_id 基础值: 14000 + 等级 = item_id
    private static final int ENHANCE_STONE_BASE_ID = 14000;
    // 品质石 item_id 基础值: 14030 + 阶数 = item_id
    private static final int QUALITY_STONE_BASE_ID = 14030;

    // APK EquipDecompose_cfg.json
    private static final int DECOMPOSE_SCROLL_ID = 15041;
    private static final Map<Integer, int[]> DECOMPOSE_TIERS = new LinkedHashMap<>();
    static {
        // [qualityStoneID, needSilver]
        DECOMPOSE_TIERS.put(1, new int[]{14031, 300});
        DECOMPOSE_TIERS.put(2, new int[]{14032, 1000});
        DECOMPOSE_TIERS.put(3, new int[]{14033, 2000});
        DECOMPOSE_TIERS.put(4, new int[]{14034, 3000});
        DECOMPOSE_TIERS.put(5, new int[]{14035, 5000});
        DECOMPOSE_TIERS.put(6, new int[]{14036, 10000});
        DECOMPOSE_TIERS.put(7, new int[]{14037, 20000});
        DECOMPOSE_TIERS.put(8, new int[]{14038, 30000});
        DECOMPOSE_TIERS.put(9, new int[]{14039, 40000});
        DECOMPOSE_TIERS.put(10, new int[]{14040, 50000});
    }

    // APK equipStrength_cfg: needSilver per target level (index = targetLevel - 1)
    private static final int[] ENHANCE_SILVER_COST = {
        300, 600, 1000, 2000, 4000, 8000, 10000, 15000, 20000, 30000,
        40000, 60000, 80000, 100000, 120000, 140000, 160000, 180000, 200000, 220000
    };

    // APK addPro 千分比: [targetLevel][slotId] (slot 1-6)
    private static final int[][] ENHANCE_ADD_PRO = {
        {0, 13,9,7,13,9,7},     // +1
        {0, 22,15,12,22,15,12}, // +2
        {0, 31,21,17,31,21,17}, // +3
        {0, 45,30,25,45,30,25}, // +4
        {0, 67,45,37,67,45,37}, // +5
        {0, 90,60,50,90,60,50}, // +6
        {0, 117,78,65,117,78,65}, // +7
        {0, 144,96,80,144,96,80}, // +8
        {0, 171,114,95,171,114,95}, // +9
        {0, 202,135,112,202,135,112}, // +10
        {0, 234,156,130,234,156,130}, // +11
        {0, 265,177,147,265,177,147}, // +12
        {0, 297,198,165,297,198,165}, // +13
        {0, 328,219,182,328,219,182}, // +14
        {0, 360,240,200,360,240,200}, // +15
        {0, 391,261,217,391,261,217}, // +16
        {0, 423,282,235,423,282,235}, // +17
        {0, 454,303,252,454,303,252}, // +18
        {0, 486,324,270,486,324,270}, // +19
        {0, 562,375,312,562,375,312}  // +20
    };

    // APK spAdd (机动加成)
    private static final int[] ENHANCE_SP_ADD = {
        0,0,0,1,1,2,2,3,4,5,6,7,8,9,10,11,12,13,14,15
    };

    private static int getNeededStoneLevel(int currentEnhanceLevel) {
        return currentEnhanceLevel + 1;
    }

    private static int getQualityStoneTier(int equipLevel) {
        if (equipLevel < 20) return 1;
        if (equipLevel >= 100) return 10;
        return (equipLevel / 10);
    }


    // 套装定义已迁移至 EquipmentConfig 中的 APK suit_config 数据

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
        
        info.put("currentBonus", calculateEnhanceBonus(equipment, currentLevel));

        int curSpAdd = currentLevel > 0 ? ENHANCE_SP_ADD[Math.min(currentLevel, MAX_ENHANCE_LEVEL) - 1] : 0;
        info.put("currentMobilityBonus", curSpAdd);

        if (currentLevel < MAX_ENHANCE_LEVEL) {
            int stoneLevel = getNeededStoneLevel(currentLevel);
            int nextIdx = currentLevel; // target level - 1
            info.put("canEnhance", true);
            info.put("silverCost", ENHANCE_SILVER_COST[nextIdx]);
            info.put("stoneLevel", stoneLevel);
            info.put("stoneItemId", String.valueOf(ENHANCE_STONE_BASE_ID + stoneLevel));
            info.put("stoneCost", 1);
            info.put("nextBonus", calculateEnhanceBonus(equipment, currentLevel + 1));
            info.put("nextMobilityBonus", ENHANCE_SP_ADD[nextIdx]);
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

        UserResource resource = userResourceService.getUserResource(odUserId);
        int silverCost = ENHANCE_SILVER_COST[currentLevel];
        int stoneLevel = getNeededStoneLevel(currentLevel);
        String stoneItemId = String.valueOf(ENHANCE_STONE_BASE_ID + stoneLevel);

        if (resource.getSilver() < silverCost) {
            throw new BusinessException(400, "银币不足");
        }

        int stoneCount = warehouseService.getItemCount(odUserId, stoneItemId);
        if (stoneCount < 1) {
            throw new BusinessException(400, stoneLevel + "级强化石不足");
        }

        resource.setSilver(resource.getSilver() - silverCost);
        warehouseService.consumeItem(odUserId, stoneItemId, 1);

        int newLevel = currentLevel + 1;
        equipment.setEnhanceLevel(newLevel);
        equipment.setEnhanceAttributes(calculateEnhanceBonus(equipment, newLevel));

        equipmentService.saveEquipment(odUserId, equipment);
        userResourceService.saveUserResource(resource);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newLevel", newLevel);
        result.put("levelDown", false);
        result.put("silverCost", silverCost);
        result.put("stoneLevel", stoneLevel);
        logger.info("装备强化成功: {} -> +{}", equipment.getName(), newLevel);

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

        int currentQualityId = equipment.getQualityValue() != null ? equipment.getQualityValue() : 1;
        if (currentQualityId < 1) currentQualityId = 1;
        EquipmentConfig.EquipQualityLevel ql = EquipmentConfig.getEquipQualityLevel(currentQualityId);

        Map<String, Object> info = new HashMap<>();
        info.put("equipmentId", equipmentId);
        info.put("equipmentName", equipment.getName());
        info.put("currentQuality", currentQualityId);
        info.put("maxQuality", 5);
        info.put("qualityName", ql.name);
        info.put("attrRate", ql.attrRate);
        info.put("qualityDesc", ql.name + " (" + (ql.attrRate / 100) + "%)");
        info.put("baseAttributes", equipment.getQualityAttributes());
        info.put("currentAttributes", equipment.getBaseAttributes());

        if (currentQualityId < 5) {
            EquipmentConfig.EquipQualityLevel next = EquipmentConfig.getEquipQualityLevel(currentQualityId + 1);
            int equipLv = equipment.getLevel() != null ? equipment.getLevel() : 1;
            int qsTier = getQualityStoneTier(equipLv);
            info.put("canUpgrade", true);
            info.put("silverCost", ql.needSilver);
            info.put("successRate", ql.increaseRate);
            info.put("successRateDesc", String.format("%.2f%%", ql.increaseRate / 100.0));
            info.put("nextQualityName", next.name);
            info.put("nextAttrRate", next.attrRate);
            info.put("qualityStoneTier", qsTier);
            info.put("qualityStoneItemId", String.valueOf(QUALITY_STONE_BASE_ID + qsTier));
            info.put("equipLevel", equipLv);
        } else {
            info.put("canUpgrade", false);
            info.put("message", "已达完美品质");
        }

        return info;
    }

    /**
     * 提升品质（洗练）
     */
    public Map<String, Object> upgradeQuality(String odUserId, String equipmentId) {
        Equipment equipment = equipmentService.getEquipment(odUserId, equipmentId);
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }

        int currentQualityId = equipment.getQualityValue() != null ? equipment.getQualityValue() : 1;
        if (currentQualityId < 1) currentQualityId = 1;

        if (currentQualityId >= 5) {
            throw new BusinessException(400, "已达完美品质");
        }

        EquipmentConfig.EquipQualityLevel ql = EquipmentConfig.getEquipQualityLevel(currentQualityId);

        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < ql.needSilver) {
            throw new BusinessException(400, "银币不足，需要" + ql.needSilver);
        }

        int equipLv = equipment.getLevel() != null ? equipment.getLevel() : 1;
        int qsTier = getQualityStoneTier(equipLv);
        String qsItemId = String.valueOf(QUALITY_STONE_BASE_ID + qsTier);
        int qsCount = warehouseService.getItemCount(odUserId, qsItemId);
        if (qsCount < 1) {
            throw new BusinessException(400, qsTier + "阶品质石不足");
        }

        resource.setSilver(resource.getSilver() - ql.needSilver);
        warehouseService.consumeItem(odUserId, qsItemId, 1);

        boolean success = new Random().nextInt(10000) < ql.increaseRate;

        Map<String, Object> result = new HashMap<>();
        result.put("oldQuality", currentQualityId);
        result.put("oldQualityName", ql.name);

        if (success) {
            int newQualityId = currentQualityId + 1;
            EquipmentConfig.EquipQualityLevel newQl = EquipmentConfig.getEquipQualityLevel(newQualityId);
            equipment.setQualityValue(newQualityId);

            Equipment.Attributes raw = equipment.getQualityAttributes();
            if (raw != null) {
                double newRate = newQl.attrRate / 10000.0;
                equipment.setBaseAttributes(Equipment.Attributes.builder()
                    .attack((int)(safeVal(raw.getAttack()) * newRate))
                    .defense((int)(safeVal(raw.getDefense()) * newRate))
                    .valor((int)(safeVal(raw.getValor()) * newRate))
                    .command((int)(safeVal(raw.getCommand()) * newRate))
                    .hp((int)(safeVal(raw.getHp()) * newRate))
                    .mobility((int)(safeVal(raw.getMobility()) * newRate))
                    .build());
            }

            String oldName = equipment.getName() != null ? equipment.getName() : "";
            String baseName = oldName.replaceFirst("^(粗糙|普通|优良|无暇|完美)的", "");
            equipment.setName(newQl.name + "的" + baseName);

            result.put("success", true);
            result.put("newQuality", newQualityId);
            result.put("newQualityName", newQl.name);
            result.put("newAttrRate", newQl.attrRate);
            result.put("newAttributes", equipment.getBaseAttributes());
            logger.info("品质提升成功: {} {} -> {}", equipment.getName(), ql.name, newQl.name);
        } else {
            result.put("success", false);
            result.put("newQuality", currentQualityId);
            result.put("newQualityName", ql.name);
            logger.info("品质提升失败: {} {}", equipment.getName(), ql.name);
        }

        result.put("silverCost", ql.needSilver);
        result.put("isPerfect", equipment.getQualityValue() >= 5);

        equipmentService.saveEquipment(odUserId, equipment);
        userResourceService.saveUserResource(resource);

        return result;
    }

    /**
     * 获取套装信息
     */
    public Map<String, Object> getSetInfo(String setId) {
        Equipment.SetInfo si = equipmentConfig.getEquipmentSet(setId);
        if (si == null) {
            throw new BusinessException(400, "套装不存在");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("setId", si.getSetId());
        info.put("setName", si.getSetName());
        info.put("setLevel", si.getSetLevel());
        info.put("threeSetEffect", si.getThreeSetEffect());
        info.put("threeSetBonus", si.getThreeSetBonus());
        info.put("sixSetEffect", si.getSixSetEffect());
        info.put("sixSetBonus", si.getSixSetBonus());

        return info;
    }

    // setName → equipment_pre APK id 数组 [武器, 戒指, 项链, 铠甲, 头盔, 靴子]
    private static final Map<String, int[]> SUIT_PATH = new LinkedHashMap<>();
    private static final Map<String, Integer> SUIT_COLOR = new LinkedHashMap<>();
    static {
        SUIT_PATH.put("宣武", new int[]{23021,23022,23023,23024,23025,23026}); SUIT_COLOR.put("宣武", 3);
        SUIT_PATH.put("折冲", new int[]{23031,23032,23033,23034,23035,23036}); SUIT_COLOR.put("折冲", 3);
        SUIT_PATH.put("骁勇", new int[]{23041,23042,23043,23044,23045,23046}); SUIT_COLOR.put("骁勇", 3);
        SUIT_PATH.put("破俘", new int[]{23051,23052,23053,23054,23055,23056}); SUIT_COLOR.put("破俘", 3);
        SUIT_PATH.put("陷阵", new int[]{23061,23062,23063,23064,23065,23066}); SUIT_COLOR.put("陷阵", 3);
        SUIT_PATH.put("狂战", new int[]{23071,23072,23073,23074,23075,23076}); SUIT_COLOR.put("狂战", 3);
        SUIT_PATH.put("天狼", new int[]{23081,23082,23083,23084,23085,23086}); SUIT_COLOR.put("天狼", 3);
        SUIT_PATH.put("征戎", new int[]{23091,23092,23093,23094,23095,23096}); SUIT_COLOR.put("征戎", 3);
        SUIT_PATH.put("破军", new int[]{24091,24092,24093,24094,24095,24096}); SUIT_COLOR.put("破军", 4);
        SUIT_PATH.put("龙威", new int[]{24101,24102,24103,24104,24105,24106}); SUIT_COLOR.put("龙威", 4);
        SUIT_PATH.put("战神", new int[]{25111,25112,25113,25114,25115,25116}); SUIT_COLOR.put("战神", 5);
        SUIT_PATH.put("鹰扬", new int[]{23121,23122,23123,23124,23125,23126}); SUIT_COLOR.put("鹰扬", 3);
        SUIT_PATH.put("虎啸", new int[]{24131,24132,24133,24134,24135,24136}); SUIT_COLOR.put("虎啸", 4);
        SUIT_PATH.put("地煞", new int[]{24141,24142,24143,24144,24145,24146}); SUIT_COLOR.put("地煞", 4);
        SUIT_PATH.put("天诛", new int[]{24151,24152,24153,24154,24155,24156}); SUIT_COLOR.put("天诛", 4);
        SUIT_PATH.put("幽冥", new int[]{24161,24162,24163,24164,24165,24166}); SUIT_COLOR.put("幽冥", 4);
        SUIT_PATH.put("诛邪", new int[]{25181,25182,25183,25184,25185,25186}); SUIT_COLOR.put("诛邪", 5);
    }
    // APK suitFuse: color → silver cost
    private static final Map<Integer, Integer> SUIT_FUSE_COST = new HashMap<>();
    static { SUIT_FUSE_COST.put(2, 5000); SUIT_FUSE_COST.put(3, 10000); SUIT_FUSE_COST.put(4, 30000); SUIT_FUSE_COST.put(5, 100000); }

    /**
     * 套装融合: 3件同套装 → 用户指定部位的1件装备, 品质继承自投入装备之一
     */
    public Map<String, Object> fuseEquipments(String odUserId, List<String> equipmentIds, int targetSlotId) {
        if (equipmentIds == null || equipmentIds.size() != 3) {
            throw new BusinessException(400, "需要选择3件装备进行融合");
        }
        if (targetSlotId < 1 || targetSlotId > 6) {
            throw new BusinessException(400, "请选择目标装备部位");
        }

        List<Equipment> equipments = new ArrayList<>();
        String setId = null;

        for (String id : equipmentIds) {
            Equipment eq = equipmentService.getEquipment(odUserId, id);
            if (eq == null) throw new BusinessException(400, "装备不存在: " + id);
            if (eq.getEquipped() != null && eq.getEquipped()) throw new BusinessException(400, "已装备的装备不能融合");
            if (eq.getSetInfo() == null || eq.getSetInfo().getSetId() == null) throw new BusinessException(400, "非套装装备不能融合: " + eq.getName());

            if (setId == null) {
                setId = eq.getSetInfo().getSetId();
            } else if (!setId.equals(eq.getSetInfo().getSetId())) {
                throw new BusinessException(400, "所选装备不是同一套装!");
            }
            equipments.add(eq);
        }

        int[] path = SUIT_PATH.get(setId);
        Integer color = SUIT_COLOR.get(setId);
        if (path == null || color == null) {
            throw new BusinessException(400, "未知套装: " + setId);
        }

        int fuseCost = SUIT_FUSE_COST.getOrDefault(color, 10000);
        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < fuseCost) {
            throw new BusinessException(400, "白银不足，需要" + fuseCost);
        }

        int preId = path[targetSlotId - 1];
        EquipmentPre pre = equipmentPreMapper.findById(preId);
        if (pre == null) {
            throw new BusinessException(500, "装备模板不存在: " + preId);
        }

        Random rng = new Random();
        Equipment donor = equipments.get(rng.nextInt(3));
        // 虎啸套装融合结果强制完美品质
        int qualityValueId;
        if ("虎啸".equals(setId)) {
            qualityValueId = 5;
        } else {
            qualityValueId = donor.getQualityValue() != null && donor.getQualityValue() > 0
                    ? donor.getQualityValue() : EquipmentConfig.rollEquipQuality();
        }
        EquipmentConfig.EquipQualityLevel ql = EquipmentConfig.getEquipQualityLevel(qualityValueId);
        double attrRate = ql.attrRate / 10000.0;

        for (Equipment eq : equipments) {
            equipmentService.deleteEquipment(odUserId, eq.getId());
        }

        Equipment.SetInfo si = equipmentConfig.getEquipmentSet(setId);

        Equipment.SlotType slotType = new Equipment.SlotType();
        slotType.setId(pre.getSlotTypeId());
        slotType.setName(pre.getPosition());

        int qId = color;
        Equipment.Quality quality = new Equipment.Quality();
        quality.setId(qId);
        quality.setName(qualityName(qId));
        quality.setColor(qualityColor(qId));
        quality.setMultiplier(qualityMultiplier(qId));

        int rawAtk = val(pre.getGenAtt()), rawDef = val(pre.getGenDef());
        int rawValor = val(pre.getGenFor()), rawCmd = val(pre.getGenLeader());
        int rawHp = val(pre.getArmyLife()), rawMob = val(pre.getArmySp());

        Equipment.Attributes rawAttrs = Equipment.Attributes.builder()
                .attack(rawAtk).defense(rawDef).valor(rawValor).command(rawCmd).hp(rawHp).mobility(rawMob).build();
        Equipment.Attributes baseAttrs = Equipment.Attributes.builder()
                .attack((int)(rawAtk * attrRate)).defense((int)(rawDef * attrRate))
                .valor((int)(rawValor * attrRate)).command((int)(rawCmd * attrRate))
                .hp((int)(rawHp * attrRate)).mobility((int)(rawMob * attrRate)).build();

        Equipment newEquipment = Equipment.builder()
                .id(UUID.randomUUID().toString())
                .userId(odUserId)
                .name(ql.name + "的" + pre.getName())
                .icon(pre.getIconUrl())
                .slotType(slotType)
                .level(pre.getNeedLevel())
                .quality(quality)
                .setInfo(si)
                .qualityAttributes(rawAttrs)
                .baseAttributes(baseAttrs)
                .enhanceLevel(0)
                .qualityValue(qualityValueId)
                .equipped(false)
                .locked(false)
                .bound(true)
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .source(Equipment.Source.builder().type("FUSE").name("装备融合").detail(setId + "套装融合").build())
                .description(ql.name + "的" + pre.getName() + " [" + (si != null ? si.getSetName() : setId + "套装") + "]")
                .build();

        equipmentService.createEquipment(odUserId, newEquipment);

        resource.setSilver(resource.getSilver() - fuseCost);
        userResourceService.saveUserResource(resource);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newEquipment", newEquipment);
        result.put("fuseCost", fuseCost);

        logger.info("套装融合: {} x3 → {} (品质{}), 消耗白银{}", setId, newEquipment.getName(), ql.name, fuseCost);

        return result;
    }

    /**
     * 装备分解 (APK EquipDecompose_cfg: 消耗分解符+银两, 产出品质石)
     */
    public Map<String, Object> decomposeEquipments(String odUserId, List<String> equipmentIds) {
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            throw new BusinessException(400, "请选择要分解的装备");
        }

        int scrollCount = warehouseService.getItemCount(odUserId, String.valueOf(DECOMPOSE_SCROLL_ID));
        if (scrollCount < equipmentIds.size()) {
            throw new BusinessException(400, "分解符不足，需要" + equipmentIds.size() + "个，当前" + scrollCount + "个");
        }

        List<Equipment> toDecompose = new ArrayList<>();
        long totalSilverCost = 0;

        for (String id : equipmentIds) {
            Equipment eq = equipmentService.getEquipment(odUserId, id);
            if (eq == null) continue;
            if (eq.getEquipped() != null && eq.getEquipped()) {
                throw new BusinessException(400, "已装备的装备不能分解: " + eq.getName());
            }
            if (eq.getLocked() != null && eq.getLocked()) {
                throw new BusinessException(400, "已锁定的装备不能分解: " + eq.getName());
            }
            int tier = getDecomposeTier(eq);
            int[] cfg = DECOMPOSE_TIERS.getOrDefault(tier, DECOMPOSE_TIERS.get(1));
            totalSilverCost += cfg[1];
            toDecompose.add(eq);
        }

        if (toDecompose.isEmpty()) {
            throw new BusinessException(400, "没有可分解的装备");
        }

        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < totalSilverCost) {
            throw new BusinessException(400, "银两不足，需要" + totalSilverCost);
        }

        warehouseService.consumeItem(odUserId, String.valueOf(DECOMPOSE_SCROLL_ID), toDecompose.size());
        resource.setSilver(resource.getSilver() - totalSilverCost);

        Map<Integer, Integer> stoneGained = new LinkedHashMap<>();
        for (Equipment eq : toDecompose) {
            int tier = getDecomposeTier(eq);
            int[] cfg = DECOMPOSE_TIERS.getOrDefault(tier, DECOMPOSE_TIERS.get(1));
            int stoneId = cfg[0];
            stoneGained.merge(stoneId, 1, Integer::sum);
            equipmentService.deleteEquipment(odUserId, eq.getId());
        }

        Map<String, String> stoneNameMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : stoneGained.entrySet()) {
            String stoneItemId = String.valueOf(entry.getKey());
            String stoneName = ITEM_NAMES.getOrDefault(entry.getKey(), stoneItemId + "品质石");
            stoneNameMap.put(stoneItemId, stoneName);
            int tier = 1;
            for (Map.Entry<Integer, int[]> te : DECOMPOSE_TIERS.entrySet()) {
                if (te.getValue()[0] == entry.getKey()) { tier = te.getKey(); break; }
            }
            Warehouse.WarehouseItem stoneItem = Warehouse.WarehouseItem.builder()
                    .itemId(stoneItemId)
                    .itemType("material")
                    .name(stoneName)
                    .icon(stoneItemId + ".jpg")
                    .quality(String.valueOf(Math.min(tier, 5)))
                    .count(entry.getValue())
                    .maxStack(999)
                    .description(stoneName + " - 用于提升装备品质")
                    .usable(false)
                    .bound(false)
                    .build();
            try {
                warehouseService.addItem(odUserId, stoneItem);
                logger.info("品质石已加入仓库: userId={}, item={}, count={}", odUserId, stoneName, entry.getValue());
            } catch (Exception e) {
                logger.error("品质石加入仓库失败: userId={}, item={}, error={}", odUserId, stoneName, e.getMessage(), e);
            }
        }

        userResourceService.saveUserResource(resource);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("decomposedCount", toDecompose.size());
        result.put("silverCost", totalSilverCost);
        result.put("stoneGained", stoneGained);
        result.put("stoneNames", stoneNameMap);
        result.put("scrollsConsumed", toDecompose.size());

        logger.info("装备分解: {} 件, 消耗银两 {}, 分解符 {}, 获得品质石 {}",
                toDecompose.size(), totalSilverCost, toDecompose.size(), stoneGained);

        return result;
    }

    private int getDecomposeTier(Equipment eq) {
        int qualityId = 1;
        if (eq.getQuality() != null && eq.getQuality().getId() != null) {
            qualityId = eq.getQuality().getId();
        }
        return Math.max(1, Math.min(10, qualityId));
    }

    private static final Map<Integer, String> ITEM_NAMES = new HashMap<>();
    static {
        ITEM_NAMES.put(14031, "1阶品质石"); ITEM_NAMES.put(14032, "2阶品质石");
        ITEM_NAMES.put(14033, "3阶品质石"); ITEM_NAMES.put(14034, "4阶品质石");
        ITEM_NAMES.put(14035, "5阶品质石"); ITEM_NAMES.put(14036, "6阶品质石");
        ITEM_NAMES.put(14037, "7阶品质石"); ITEM_NAMES.put(14038, "8阶品质石");
        ITEM_NAMES.put(14039, "9阶品质石"); ITEM_NAMES.put(14040, "10阶品质石");
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

        int slotId = 1;
        if (equipment.getSlotType() != null && equipment.getSlotType().getId() != null) {
            slotId = equipment.getSlotType().getId();
        }
        if (slotId < 1 || slotId > 6) slotId = 1;

        int idx = Math.min(level, MAX_ENHANCE_LEVEL) - 1;
        int promille = ENHANCE_ADD_PRO[idx][slotId];
        int spAdd = ENHANCE_SP_ADD[idx];

        return Equipment.Attributes.builder()
            .attack(base.getAttack() * promille / 1000)
            .defense(base.getDefense() * promille / 1000)
            .valor(base.getValor() * promille / 1000)
            .command(base.getCommand() * promille / 1000)
            .hp(base.getHp() * promille / 1000)
            .mobility(base.getMobility() * promille / 1000 + spAdd)
            .build();
    }

    private static int val(Integer v) { return v != null ? v : 0; }
    private int safeVal(Integer v) { return v != null ? v : 0; }

    private String qualityColor(int q) {
        switch (q) {
            case 2: return "#55ff55";
            case 3: return "#5599ff";
            case 4: return "#9370DB";
            case 5: return "#ff9933";
            case 6: return "#ff4444";
            default: return "#aaaaaa";
        }
    }
    private String qualityName(int q) {
        switch (q) {
            case 2: return "绿色"; case 3: return "蓝色"; case 4: return "紫色";
            case 5: return "橙色"; case 6: return "红色"; default: return "白色";
        }
    }
    private double qualityMultiplier(int q) {
        switch (q) {
            case 2: return 1.0; case 3: return 1.2; case 4: return 1.5;
            case 5: return 2.0; case 6: return 2.5; default: return 0.8;
        }
    }

    // ==================== 材料合成 (APK PropClip_cfg) ====================

    private static final Map<Integer, String> ITEM_NAME_MAP = new HashMap<>();
    static {
        ITEM_NAME_MAP.put(14001,"1级强化石");ITEM_NAME_MAP.put(14002,"2级强化石");ITEM_NAME_MAP.put(14003,"3级强化石");
        ITEM_NAME_MAP.put(14004,"4级强化石");ITEM_NAME_MAP.put(14005,"5级强化石");ITEM_NAME_MAP.put(14006,"6级强化石");
        ITEM_NAME_MAP.put(14007,"7级强化石");ITEM_NAME_MAP.put(14008,"8级强化石");ITEM_NAME_MAP.put(14009,"9级强化石");
        ITEM_NAME_MAP.put(14010,"10级强化石");ITEM_NAME_MAP.put(14011,"11级强化石");ITEM_NAME_MAP.put(14012,"12级强化石");
        ITEM_NAME_MAP.put(14013,"13级强化石");ITEM_NAME_MAP.put(14014,"14级强化石");ITEM_NAME_MAP.put(14015,"15级强化石");
        ITEM_NAME_MAP.put(14016,"16级强化石");ITEM_NAME_MAP.put(14017,"17级强化石");ITEM_NAME_MAP.put(14018,"18级强化石");
        ITEM_NAME_MAP.put(14019,"19级强化石");ITEM_NAME_MAP.put(14020,"20级强化石");
        ITEM_NAME_MAP.put(14031,"1阶品质石");ITEM_NAME_MAP.put(14032,"2阶品质石");ITEM_NAME_MAP.put(14033,"3阶品质石");
        ITEM_NAME_MAP.put(14034,"4阶品质石");ITEM_NAME_MAP.put(14035,"5阶品质石");ITEM_NAME_MAP.put(14036,"6阶品质石");
        ITEM_NAME_MAP.put(14037,"7阶品质石");ITEM_NAME_MAP.put(14038,"8阶品质石");ITEM_NAME_MAP.put(14039,"9阶品质石");
        ITEM_NAME_MAP.put(14040,"10阶品质石");
        ITEM_NAME_MAP.put(15001,"初级合成符");ITEM_NAME_MAP.put(15002,"中级合成符");
        ITEM_NAME_MAP.put(15003,"高级合成符");ITEM_NAME_MAP.put(15004,"特级合成符");
        ITEM_NAME_MAP.put(15011,"初级招贤令");ITEM_NAME_MAP.put(15012,"中级招贤令");ITEM_NAME_MAP.put(15013,"高级招贤令");
        ITEM_NAME_MAP.put(15021,"三十六计");ITEM_NAME_MAP.put(15022,"鬼谷兵法");
        ITEM_NAME_MAP.put(15023,"太公六韬");ITEM_NAME_MAP.put(15024,"孙子兵法");
    }

    private static final Map<Integer, int[]> COMPOSE_RECIPES = new LinkedHashMap<>();
    static {
        // {sourceId -> [aimID, needID, needNum, silver]}
        COMPOSE_RECIPES.put(14001, new int[]{14002, 15001, 3, 500});
        COMPOSE_RECIPES.put(14002, new int[]{14003, 15001, 3, 1000});
        COMPOSE_RECIPES.put(14003, new int[]{14004, 15002, 3, 1500});
        COMPOSE_RECIPES.put(14004, new int[]{14005, 15002, 3, 2000});
        COMPOSE_RECIPES.put(14005, new int[]{14006, 15003, 3, 3000});
        COMPOSE_RECIPES.put(14006, new int[]{14007, 15003, 3, 4000});
        COMPOSE_RECIPES.put(14007, new int[]{14008, 15003, 3, 5000});
        COMPOSE_RECIPES.put(14008, new int[]{14009, 15004, 3, 7000});
        COMPOSE_RECIPES.put(14009, new int[]{14010, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14010, new int[]{14011, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14011, new int[]{14012, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14013, new int[]{14014, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14014, new int[]{14015, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14015, new int[]{14016, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14016, new int[]{14017, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14017, new int[]{14018, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14018, new int[]{14019, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14019, new int[]{14020, 15004, 3, 10000});
        COMPOSE_RECIPES.put(14031, new int[]{14032, 15001, 3, 300});
        COMPOSE_RECIPES.put(14032, new int[]{14033, 15001, 3, 600});
        COMPOSE_RECIPES.put(14033, new int[]{14034, 15002, 3, 1000});
        COMPOSE_RECIPES.put(14034, new int[]{14035, 15002, 3, 2000});
        COMPOSE_RECIPES.put(14035, new int[]{14036, 15002, 3, 4000});
        COMPOSE_RECIPES.put(14036, new int[]{14037, 15003, 3, 7000});
        COMPOSE_RECIPES.put(14037, new int[]{14038, 15003, 3, 10000});
        COMPOSE_RECIPES.put(14038, new int[]{14039, 15004, 3, 15000});
        COMPOSE_RECIPES.put(14039, new int[]{14040, 15004, 3, 20000});
        COMPOSE_RECIPES.put(15011, new int[]{15012, 15002, 15, 5000});
        COMPOSE_RECIPES.put(15012, new int[]{15013, 15003, 15, 10000});
        COMPOSE_RECIPES.put(15021, new int[]{15022, 15001, 3, 300});
        COMPOSE_RECIPES.put(15022, new int[]{15023, 15002, 3, 700});
        COMPOSE_RECIPES.put(15023, new int[]{15024, 15003, 3, 1200});
    }

    public Map<String, Object> composeMaterial(String userId, int sourceItemId) {
        int[] recipe = COMPOSE_RECIPES.get(sourceItemId);
        if (recipe == null) {
            throw new BusinessException(400, "无效的合成配方");
        }

        int aimID = recipe[0];
        int needID = recipe[1];
        int needNum = recipe[2];
        int silverCost = recipe[3];

        int srcCount = warehouseService.getItemCount(userId, String.valueOf(sourceItemId));
        if (srcCount < needNum) {
            throw new BusinessException(400, "源材料不足，需要" + needNum + "个");
        }

        int symbolCount = warehouseService.getItemCount(userId, String.valueOf(needID));
        if (symbolCount < 1) {
            throw new BusinessException(400, "合成符不足");
        }

        UserResource resource = userResourceService.getUserResource(userId);
        long silver = resource.getSilver() != null ? resource.getSilver() : 0;
        if (silver < silverCost) {
            throw new BusinessException(400, "银两不足，需要" + silverCost);
        }

        warehouseService.removeItem(userId, String.valueOf(sourceItemId), needNum);
        warehouseService.removeItem(userId, String.valueOf(needID), 1);
        userResourceService.consumeSilver(userId, silverCost);

        Warehouse.WarehouseItem resultItem = Warehouse.WarehouseItem.builder()
                .itemId(String.valueOf(aimID))
                .itemType("material")
                .name(ITEM_NAME_MAP.getOrDefault(aimID, "未知道具"))
                .icon("images/item/" + aimID + ".jpg")
                .count(1)
                .maxStack(9999)
                .usable(false)
                .bound(false)
                .build();
        warehouseService.addItem(userId, resultItem);

        logger.info("材料合成: {} x{} + 合成符{} → {}, 银两-{}", sourceItemId, needNum, needID, aimID, silverCost);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("resultItemId", aimID);
        result.put("silverCost", silverCost);
        return result;
    }

}
