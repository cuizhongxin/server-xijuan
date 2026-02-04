package com.tencent.wxcloudrun.service.equipment;

import com.tencent.wxcloudrun.config.EquipmentConfig;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.SecretRealm;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.repository.UserMaterialRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import com.tencent.wxcloudrun.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 装备服务
 */
@Service
public class EquipmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(EquipmentService.class);
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private UserMaterialRepository materialRepository;
    
    @Autowired
    private UserResourceRepository resourceRepository;
    
    @Autowired
    private EquipmentConfig equipmentConfig;
    
    private final Random random = new Random();
    
    // ==================== 装备名称配置 ====================
    
    private static final Map<Integer, List<String>> WEAPON_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> HELMET_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> ARMOR_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> RING_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> SHOES_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> NECKLACE_NAMES = new HashMap<>();
    
    static {
        // 武器名称（按等级）
        WEAPON_NAMES.put(20, Arrays.asList("青铜剑", "铁刀", "木枪", "短戟"));
        WEAPON_NAMES.put(40, Arrays.asList("精钢剑", "百炼刀", "银枪", "双戟"));
        WEAPON_NAMES.put(60, Arrays.asList("玄铁剑", "偃月刀", "亮银枪", "方天戟"));
        WEAPON_NAMES.put(80, Arrays.asList("青龙剑", "青龙刀", "龙胆枪", "画戟"));
        WEAPON_NAMES.put(100, Arrays.asList("倚天剑", "屠龙刀", "丈八蛇矛", "方天画戟"));
        
        // 头盔名称
        HELMET_NAMES.put(20, Arrays.asList("皮盔", "铁盔", "布冠"));
        HELMET_NAMES.put(40, Arrays.asList("钢盔", "银盔", "武将盔"));
        HELMET_NAMES.put(60, Arrays.asList("玄铁盔", "紫金盔", "麒麟盔"));
        HELMET_NAMES.put(80, Arrays.asList("龙鳞盔", "凤翎冠", "虎头盔"));
        HELMET_NAMES.put(100, Arrays.asList("天王盔", "战神冠", "霸王盔"));
        
        // 铠甲名称
        ARMOR_NAMES.put(20, Arrays.asList("皮甲", "布甲", "链甲"));
        ARMOR_NAMES.put(40, Arrays.asList("钢甲", "银鳞甲", "锁子甲"));
        ARMOR_NAMES.put(60, Arrays.asList("玄铁甲", "紫金甲", "龙纹甲"));
        ARMOR_NAMES.put(80, Arrays.asList("龙鳞甲", "麒麟甲", "凤羽甲"));
        ARMOR_NAMES.put(100, Arrays.asList("天神甲", "战神铠", "霸王甲"));
        
        // 戒指名称
        RING_NAMES.put(20, Arrays.asList("铜戒", "银戒", "玉戒"));
        RING_NAMES.put(40, Arrays.asList("金戒", "翡翠戒", "宝石戒"));
        RING_NAMES.put(60, Arrays.asList("灵石戒", "紫晶戒", "血玉戒"));
        RING_NAMES.put(80, Arrays.asList("龙魂戒", "凤血戒", "圣光戒"));
        RING_NAMES.put(100, Arrays.asList("天命戒", "造化戒", "混沌戒"));
        
        // 鞋子名称
        SHOES_NAMES.put(20, Arrays.asList("布靴", "皮靴", "草鞋"));
        SHOES_NAMES.put(40, Arrays.asList("千里靴", "疾风靴", "轻云靴"));
        SHOES_NAMES.put(60, Arrays.asList("追风靴", "飞云靴", "踏雪靴"));
        SHOES_NAMES.put(80, Arrays.asList("龙行靴", "腾云靴", "御风靴"));
        SHOES_NAMES.put(100, Arrays.asList("神行靴", "凌霄靴", "破空靴"));
        
        // 项链名称
        NECKLACE_NAMES.put(20, Arrays.asList("铜链", "银链", "玉坠"));
        NECKLACE_NAMES.put(40, Arrays.asList("金链", "翡翠坠", "珍珠链"));
        NECKLACE_NAMES.put(60, Arrays.asList("灵石链", "紫晶坠", "琥珀链"));
        NECKLACE_NAMES.put(80, Arrays.asList("龙魂坠", "凤凰坠", "圣光链"));
        NECKLACE_NAMES.put(100, Arrays.asList("天命坠", "九天链", "造化坠"));
    }
    
    // ==================== 装备获取 ====================
    
    /**
     * 获取用户所有装备
     */
    public List<Equipment> getUserEquipments(String userId) {
        return equipmentRepository.findByUserId(userId);
    }
    
    /**
     * 获取用户背包中的装备（未装备）
     */
    public List<Equipment> getUserBagEquipments(String userId) {
        return equipmentRepository.findUnequippedByUserId(userId);
    }
    
    /**
     * 获取武将已装备的装备
     */
    public List<Equipment> getGeneralEquipments(String generalId) {
        return equipmentRepository.findEquippedByGeneralId(generalId);
    }
    
    /**
     * 根据ID获取装备
     */
    public Equipment getEquipmentById(String equipmentId) {
        return equipmentRepository.findById(equipmentId);
    }
    
    /**
     * 获取指定用户的装备（带权限检查）
     */
    public Equipment getEquipment(String userId, String equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId);
        if (equipment == null) {
            return null;
        }
        if (!userId.equals(equipment.getUserId())) {
            return null;
        }
        return equipment;
    }
    
    /**
     * 保存装备
     */
    public Equipment saveEquipment(String userId, Equipment equipment) {
        if (equipment == null) {
            throw new BusinessException(400, "装备不能为空");
        }
        if (!userId.equals(equipment.getUserId())) {
            throw new BusinessException(403, "无权操作此装备");
        }
        equipment.setUpdateTime(System.currentTimeMillis());
        return equipmentRepository.update(equipment);
    }
    
    /**
     * 删除装备
     */
    public void deleteEquipment(String userId, String equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId);
        if (equipment == null) {
            return;
        }
        if (!userId.equals(equipment.getUserId())) {
            throw new BusinessException(403, "无权操作此装备");
        }
        if (equipment.getEquipped() != null && equipment.getEquipped()) {
            throw new BusinessException(400, "请先卸下装备");
        }
        equipmentRepository.delete(equipmentId);
    }
    
    // ==================== 装备穿戴 ====================
    
    /**
     * 装备到武将（带等级检查）
     */
    public Equipment equipToGeneral(String userId, String equipmentId, String generalId, int generalLevel) {
        Equipment equipment = equipmentRepository.findById(equipmentId);
        
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }
        
        if (!userId.equals(equipment.getUserId())) {
            throw new BusinessException(403, "无权操作此装备");
        }
        
        if (equipment.getEquipped()) {
            throw new BusinessException(400, "装备已被穿戴");
        }
        
        // 检查装备等级限制：武将等级必须 >= 装备等级
        if (equipment.getLevel() != null && generalLevel < equipment.getLevel()) {
            throw new BusinessException(400, "武将等级不足，需要达到" + equipment.getLevel() + "级才能穿戴此装备");
        }
        
        // 检查武将该槽位是否已有装备
        List<Equipment> generalEquipments = equipmentRepository.findEquippedByGeneralId(generalId);
        for (Equipment existingEquip : generalEquipments) {
            if (existingEquip.getSlotType().getId().equals(equipment.getSlotType().getId())) {
                // 卸下旧装备
                existingEquip.setEquipped(false);
                existingEquip.setEquippedGeneralId(null);
                equipmentRepository.update(existingEquip);
            }
        }
        
        // 装备新装备
        equipment.setEquipped(true);
        equipment.setEquippedGeneralId(generalId);
        
        return equipmentRepository.update(equipment);
    }
    
    /**
     * 装备到武将（旧接口兼容，不检查等级）
     * @deprecated 使用 equipToGeneral(userId, equipmentId, generalId, generalLevel) 代替
     */
    @Deprecated
    public Equipment equipToGeneral(String userId, String equipmentId, String generalId) {
        // 兼容旧调用，假设武将等级为100（允许穿戴所有装备）
        return equipToGeneral(userId, equipmentId, generalId, 100);
    }
    
    /**
     * 卸下装备
     */
    public Equipment unequip(String userId, String equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId);
        
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }
        
        if (!userId.equals(equipment.getUserId())) {
            throw new BusinessException(403, "无权操作此装备");
        }
        
        if (!equipment.getEquipped()) {
            throw new BusinessException(400, "装备未被穿戴");
        }
        
        equipment.setEquipped(false);
        equipment.setEquippedGeneralId(null);
        
        return equipmentRepository.update(equipment);
    }
    
    // ==================== 装备生成 ====================
    
    /**
     * 手工制作装备
     */
    public Equipment craftEquipment(String userId, Integer slotTypeId, Integer level) {
        // 检查等级是否有效
        if (level < 20 || level > 100 || level % 20 != 0) {
            throw new BusinessException(400, "无效的装备等级");
        }
        
        // 获取材料需求
        Map<String, Integer> requiredMaterials = getCraftMaterials(slotTypeId, level);
        
        // 检查并消耗材料
        if (!materialRepository.consumeMaterials(userId, requiredMaterials)) {
            throw new BusinessException(400, "材料不足");
        }
        
        // 生成装备（手工制作品质最低为白色，最高为绿色）
        int qualityId = random.nextInt(100) < 70 ? 1 : 2;
        
        Equipment equipment = generateEquipment(userId, slotTypeId, level, qualityId, 
            Equipment.Source.builder()
                .type("CRAFT")
                .name("手工制作")
                .detail("工坊打造")
                .build(),
            null);  // 手工制作不属于套装
        
        return equipmentRepository.save(equipment);
    }
    
    /**
     * 获取制作所需材料
     */
    public Map<String, Integer> getCraftMaterials(Integer slotTypeId, Integer level) {
        Map<String, Integer> materials = new HashMap<>();
        
        int baseCost = level / 20 * 10;  // 20级10个，40级20个...
        
        switch (slotTypeId) {
            case 1: // 武器
                materials.put("METAL_IRON", baseCost * 2);
                materials.put("WOOD_COMMON", baseCost);
                materials.put("LEATHER_COMMON", baseCost / 2);
                break;
            case 2: // 头盔
                materials.put("METAL_IRON", baseCost);
                materials.put("LEATHER_COMMON", baseCost);
                materials.put("CLOTH_COTTON", baseCost / 2);
                break;
            case 3: // 铠甲
                materials.put("METAL_IRON", baseCost * 2);
                materials.put("LEATHER_COMMON", baseCost);
                materials.put("CLOTH_COTTON", baseCost);
                break;
            case 4: // 戒指
                materials.put("METAL_IRON", baseCost / 2);
                materials.put("GEM_JADE", baseCost / 2);
                break;
            case 5: // 鞋子
                materials.put("LEATHER_COMMON", baseCost * 2);
                materials.put("CLOTH_COTTON", baseCost);
                break;
            case 6: // 项链
                materials.put("METAL_IRON", baseCost / 2);
                materials.put("GEM_JADE", baseCost);
                materials.put("PAPER_COMMON", baseCost / 2);
                break;
        }
        
        return materials;
    }
    
    /**
     * 副本掉落装备
     */
    public Equipment dungeonDropEquipment(String userId, Integer level, String dungeonName) {
        // 副本品质：蓝色50%，紫色40%，橙色10%
        int roll = random.nextInt(100);
        int qualityId;
        if (roll < 50) {
            qualityId = 3;  // 蓝色
        } else if (roll < 90) {
            qualityId = 4;  // 紫色
        } else {
            qualityId = 5;  // 橙色
        }
        
        // 随机槽位
        int slotTypeId = random.nextInt(6) + 1;
        
        // 随机套装
        List<Equipment.SetInfo> sets = equipmentConfig.getEquipmentSetsByLevel(level);
        String setId = null;
        if (!sets.isEmpty() && random.nextInt(100) < 30) {  // 30%概率掉落套装
            setId = sets.get(random.nextInt(sets.size())).getSetId();
        }
        
        Equipment equipment = generateEquipment(userId, slotTypeId, level, qualityId,
            Equipment.Source.builder()
                .type("DUNGEON")
                .name("副本掉落")
                .detail(dungeonName)
                .build(),
            setId);
        
        return equipmentRepository.save(equipment);
    }
    
    /**
     * 秘境寻宝获取装备
     */
    public Equipment secretRealmExplore(String userId, String realmId, Integer playerLevel) {
        SecretRealm realm = equipmentConfig.getSecretRealm(realmId);
        
        if (realm == null) {
            throw new BusinessException(400, "秘境不存在");
        }
        
        if (playerLevel < realm.getUnlockLevel()) {
            throw new BusinessException(400, "等级不足，无法进入该秘境");
        }
        
        // 检查并消耗黄金
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null || resource.getGold() < realm.getGoldCost()) {
            throw new BusinessException(400, "黄金不足");
        }
        
        resource.setGold(resource.getGold() - realm.getGoldCost());
        resourceRepository.save(resource);
        
        // 秘境品质根据配置
        int qualityId = randomQuality(realm.getMinQuality(), realm.getMaxQuality());
        
        // 随机槽位
        int slotTypeId = random.nextInt(6) + 1;
        
        // 随机套装（秘境必定掉落套装）
        List<String> dropSets = realm.getDropSets();
        String setId = dropSets.get(random.nextInt(dropSets.size()));
        
        Equipment equipment = generateEquipment(userId, slotTypeId, realm.getEquipmentLevel(), qualityId,
            Equipment.Source.builder()
                .type("SECRET_REALM")
                .name("秘境寻宝")
                .detail(realm.getName())
                .build(),
            setId);
        
        logger.info("用户 {} 在 {} 获得装备: {} ({})", userId, realm.getName(), 
                   equipment.getName(), equipment.getQuality().getName());
        
        return equipmentRepository.save(equipment);
    }
    
    // ==================== 套装效果 ====================
    
    /**
     * 计算武将的套装效果
     */
    public Map<String, Object> calculateSetBonus(String generalId) {
        Map<String, Object> result = new HashMap<>();
        List<Equipment> equippedList = equipmentRepository.findEquippedByGeneralId(generalId);
        
        // 统计每个套装的装备数量
        Map<String, Integer> setCount = new HashMap<>();
        for (Equipment equipment : equippedList) {
            if (equipment.getSetInfo() != null) {
                String setId = equipment.getSetInfo().getSetId();
                setCount.merge(setId, 1, Integer::sum);
            }
        }
        
        // 计算套装加成
        Equipment.Attributes totalBonus = Equipment.Attributes.builder().build();
        List<Map<String, Object>> activeSetEffects = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : setCount.entrySet()) {
            String setId = entry.getKey();
            int count = entry.getValue();
            Equipment.SetInfo setInfo = equipmentConfig.getEquipmentSet(setId);
            
            if (setInfo == null) continue;
            
            Map<String, Object> setEffect = new HashMap<>();
            setEffect.put("setId", setId);
            setEffect.put("setName", setInfo.getSetName());
            setEffect.put("count", count);
            
            if (count >= 3) {
                // 3件套效果
                addAttributes(totalBonus, setInfo.getThreeSetBonus());
                setEffect.put("threeSetActive", true);
                setEffect.put("threeSetEffect", setInfo.getThreeSetEffect());
            }
            
            if (count >= 6) {
                // 6件套效果
                addAttributes(totalBonus, setInfo.getSixSetBonus());
                setEffect.put("sixSetActive", true);
                setEffect.put("sixSetEffect", setInfo.getSixSetEffect());
            }
            
            activeSetEffects.add(setEffect);
        }
        
        result.put("activeSetEffects", activeSetEffects);
        result.put("totalBonus", totalBonus);
        
        return result;
    }
    
    // ==================== 材料管理 ====================
    
    /**
     * 获取用户材料
     */
    public Map<String, Integer> getUserMaterials(String userId) {
        Map<String, Integer> materials = materialRepository.getUserMaterials(userId);
        if (materials.isEmpty()) {
            materialRepository.initUserMaterials(userId);
            materials = materialRepository.getUserMaterials(userId);
        }
        return materials;
    }
    
    /**
     * 添加材料
     */
    public void addMaterial(String userId, String materialId, int count) {
        materialRepository.addMaterial(userId, materialId, count);
    }
    
    // ==================== 秘境相关 ====================
    
    /**
     * 获取所有秘境
     */
    public Map<String, SecretRealm> getAllSecretRealms() {
        return equipmentConfig.getAllSecretRealms();
    }
    
    /**
     * 获取已解锁的秘境
     */
    public List<SecretRealm> getUnlockedSecretRealms(Integer playerLevel) {
        return equipmentConfig.getUnlockedSecretRealms(playerLevel);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 生成装备
     */
    private Equipment generateEquipment(String userId, Integer slotTypeId, Integer level, 
                                        Integer qualityId, Equipment.Source source, String setId) {
        String equipmentId = "equip_" + System.currentTimeMillis() + "_" + 
                            UUID.randomUUID().toString().substring(0, 8);
        
        Equipment.SlotType slotType = equipmentConfig.getSlotType(slotTypeId);
        Equipment.Quality quality = equipmentConfig.getQuality(qualityId);
        Equipment.SetInfo setInfo = setId != null ? equipmentConfig.getEquipmentSet(setId) : null;
        
        // 生成装备名称
        String name = generateEquipmentName(slotTypeId, level, quality);
        
        // 计算基础属性
        Equipment.Attributes baseAttributes = calculateBaseAttributes(slotType, level, quality);
        
        // 计算附加属性（随机）
        Equipment.Attributes bonusAttributes = calculateBonusAttributes(level, quality);
        
        // 装备图标
        String icon = slotType.getIcon();
        
        // 装备描述
        String description = generateDescription(slotType, level, quality, source);
        
        return Equipment.builder()
            .id(equipmentId)
            .userId(userId)
            .name(name)
            .slotType(slotType)
            .level(level)
            .quality(quality)
            .setInfo(setInfo)
            .baseAttributes(baseAttributes)
            .bonusAttributes(bonusAttributes)
            .source(source)
            .equipped(false)
            .equippedGeneralId(null)
            .icon(icon)
            .description(description)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 生成装备名称
     */
    private String generateEquipmentName(Integer slotTypeId, Integer level, Equipment.Quality quality) {
        List<String> names;
        
        switch (slotTypeId) {
            case 1: names = WEAPON_NAMES.getOrDefault(level, Arrays.asList("武器")); break;
            case 2: names = HELMET_NAMES.getOrDefault(level, Arrays.asList("头盔")); break;
            case 3: names = ARMOR_NAMES.getOrDefault(level, Arrays.asList("铠甲")); break;
            case 4: names = RING_NAMES.getOrDefault(level, Arrays.asList("戒指")); break;
            case 5: names = SHOES_NAMES.getOrDefault(level, Arrays.asList("鞋子")); break;
            case 6: names = NECKLACE_NAMES.getOrDefault(level, Arrays.asList("项链")); break;
            default: names = Arrays.asList("装备");
        }
        
        String baseName = names.get(random.nextInt(names.size()));
        
        // 高品质装备添加前缀
        if (quality.getId() >= 4) {
            String[] prefixes = {"精良的", "优秀的", "卓越的", "传说的", "神圣的"};
            baseName = prefixes[Math.min(quality.getId() - 4, prefixes.length - 1)] + baseName;
        }
        
        return baseName;
    }
    
    /**
     * 计算基础属性
     */
    private Equipment.Attributes calculateBaseAttributes(Equipment.SlotType slotType, 
                                                         Integer level, Equipment.Quality quality) {
        int baseValue = level * 5;  // 基础值
        double multiplier = quality.getMultiplier();  // 品质倍率
        
        Equipment.Attributes.AttributesBuilder builder = Equipment.Attributes.builder();
        
        switch (slotType.getId()) {
            case 1: // 武器 - 主攻击
                builder.attack((int)(baseValue * 2 * multiplier));
                builder.valor((int)(baseValue * 0.5 * multiplier));
                builder.critRate(quality.getId() * 1.0);
                break;
            case 2: // 头盔 - 主防御
                builder.defense((int)(baseValue * 1.5 * multiplier));
                builder.hp((int)(baseValue * 10 * multiplier));
                break;
            case 3: // 铠甲 - 主防御
                builder.defense((int)(baseValue * 2 * multiplier));
                builder.hp((int)(baseValue * 15 * multiplier));
                builder.dodge(quality.getId() * 0.5);
                break;
            case 4: // 戒指 - 主攻击
                builder.attack((int)(baseValue * 1.5 * multiplier));
                builder.critDamage(quality.getId() * 5.0);
                break;
            case 5: // 鞋子 - 主机动性
                builder.mobility((int)(baseValue * 1.5 * multiplier));
                builder.dodge(quality.getId() * 1.0);
                break;
            case 6: // 项链 - 主统御
                builder.command((int)(baseValue * 1.5 * multiplier));
                builder.valor((int)(baseValue * 0.5 * multiplier));
                break;
        }
        
        return builder.build();
    }
    
    /**
     * 计算附加属性（随机）
     */
    private Equipment.Attributes calculateBonusAttributes(Integer level, Equipment.Quality quality) {
        Equipment.Attributes.AttributesBuilder builder = Equipment.Attributes.builder();
        
        int bonusCount = Math.min(quality.getId(), 4);  // 品质越高，附加属性越多
        int baseBonus = (int)(level * quality.getMultiplier());
        
        List<String> availableStats = Arrays.asList("attack", "defense", "valor", "command", "mobility", "hp");
        Collections.shuffle(availableStats);
        
        for (int i = 0; i < bonusCount; i++) {
            String stat = availableStats.get(i);
            int value = baseBonus / 2 + random.nextInt(baseBonus);
            
            switch (stat) {
                case "attack": builder.attack(value); break;
                case "defense": builder.defense(value); break;
                case "valor": builder.valor(value / 2); break;
                case "command": builder.command(value / 2); break;
                case "mobility": builder.mobility(value / 3); break;
                case "hp": builder.hp(value * 5); break;
            }
        }
        
        return builder.build();
    }
    
    /**
     * 生成装备描述
     */
    private String generateDescription(Equipment.SlotType slotType, Integer level, 
                                       Equipment.Quality quality, Equipment.Source source) {
        return String.format("等级%d的%s%s，来源：%s",
            level, quality.getName(), slotType.getName(), source.getName());
    }
    
    /**
     * 随机品质
     */
    private int randomQuality(int min, int max) {
        // 越高品质概率越低
        int roll = random.nextInt(100);
        
        if (max >= 6 && roll < 5) return 6;   // 5% 红色
        if (max >= 5 && roll < 20) return 5;  // 15% 橙色
        if (max >= 4 && roll < 50) return 4;  // 30% 紫色
        
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * 属性相加
     */
    private void addAttributes(Equipment.Attributes target, Equipment.Attributes source) {
        if (source == null) return;
        
        target.setAttack(target.getAttack() + source.getAttack());
        target.setDefense(target.getDefense() + source.getDefense());
        target.setValor(target.getValor() + source.getValor());
        target.setCommand(target.getCommand() + source.getCommand());
        target.setDodge(target.getDodge() + source.getDodge());
        target.setMobility(target.getMobility() + source.getMobility());
        target.setHp(target.getHp() + source.getHp());
        target.setCritRate(target.getCritRate() + source.getCritRate());
        target.setCritDamage(target.getCritDamage() + source.getCritDamage());
    }
}


