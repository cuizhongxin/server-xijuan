package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.SecretRealm;
import com.tencent.wxcloudrun.model.CraftMaterial;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 装备系统配置
 */
@Component
public class EquipmentConfig {
    
    // ==================== 装备槽位配置 ====================
    private Map<Integer, Equipment.SlotType> slotTypes = new HashMap<>();
    
    // ==================== 装备品质配置 ====================
    private Map<Integer, Equipment.Quality> qualities = new HashMap<>();
    
    // ==================== 套装配置 ====================
    private Map<String, Equipment.SetInfo> equipmentSets = new HashMap<>();
    
    // ==================== 秘境配置 ====================
    private Map<String, SecretRealm> secretRealms = new LinkedHashMap<>();
    
    // ==================== 材料配置 ====================
    private Map<String, CraftMaterial> materials = new HashMap<>();
    
    // ==================== 装备模板配置 ====================
    private List<EquipmentTemplate> equipmentTemplates = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        initSlotTypes();
        initQualities();
        initEquipmentSets();
        initSecretRealms();
        initMaterials();
        initEquipmentTemplates();
    }
    
    /**
     * 初始化槽位类型
     */
    private void initSlotTypes() {
        slotTypes.put(1, Equipment.SlotType.builder()
            .id(1).name("主武器").icon("⚔️").mainAttribute("attack").build());
        slotTypes.put(2, Equipment.SlotType.builder()
            .id(2).name("头盔").icon("🪖").mainAttribute("defense").build());
        slotTypes.put(3, Equipment.SlotType.builder()
            .id(3).name("铠甲").icon("🛡️").mainAttribute("defense").build());
        slotTypes.put(4, Equipment.SlotType.builder()
            .id(4).name("戒指").icon("💍").mainAttribute("attack").build());
        slotTypes.put(5, Equipment.SlotType.builder()
            .id(5).name("鞋子").icon("👢").mainAttribute("mobility").build());
        slotTypes.put(6, Equipment.SlotType.builder()
            .id(6).name("项链").icon("📿").mainAttribute("command").build());
    }
    
    /**
     * 初始化品质配置
     */
    private void initQualities() {
        qualities.put(1, Equipment.Quality.builder()
            .id(1).name("白色").color("#FFFFFF").multiplier(1.0).icon("⚪").build());
        qualities.put(2, Equipment.Quality.builder()
            .id(2).name("绿色").color("#32CD32").multiplier(1.2).icon("🟢").build());
        qualities.put(3, Equipment.Quality.builder()
            .id(3).name("蓝色").color("#4169E1").multiplier(1.5).icon("🔵").build());
        qualities.put(4, Equipment.Quality.builder()
            .id(4).name("紫色").color("#9370DB").multiplier(2.0).icon("🟣").build());
        qualities.put(5, Equipment.Quality.builder()
            .id(5).name("橙色").color("#FF8C00").multiplier(2.5).icon("🟠").build());
        qualities.put(6, Equipment.Quality.builder()
            .id(6).name("红色").color("#DC143C").multiplier(3.0).icon("🔴").build());
    }
    
    /**
     * 初始化套装配置
     * 从20级开始每20级一套
     */
    private void initEquipmentSets() {
        // 20级套装 - 新兵套装
        equipmentSets.put("SET_20_RECRUIT", Equipment.SetInfo.builder()
            .setId("SET_20_RECRUIT")
            .setName("新兵套装")
            .setLevel(20)
            .threeSetEffect("攻击力+5%, 防御力+5%")
            .threeSetBonus(Equipment.Attributes.builder().attack(50).defense(50).build())
            .sixSetEffect("全属性+8%, 暴击率+3%")
            .sixSetBonus(Equipment.Attributes.builder().attack(80).defense(80).valor(40).command(40).mobility(20).critRate(3.0).build())
            .build());
        
        // 40级套装 - 精锐套装
        equipmentSets.put("SET_40_ELITE", Equipment.SetInfo.builder()
            .setId("SET_40_ELITE")
            .setName("精锐套装")
            .setLevel(40)
            .threeSetEffect("攻击力+10%, 武勇+8%")
            .threeSetBonus(Equipment.Attributes.builder().attack(120).valor(60).build())
            .sixSetEffect("全属性+15%, 暴击伤害+20%")
            .sixSetBonus(Equipment.Attributes.builder().attack(180).defense(150).valor(100).command(80).mobility(40).critDamage(20.0).build())
            .build());
        
        // 60级套装 - 将军套装
        equipmentSets.put("SET_60_GENERAL", Equipment.SetInfo.builder()
            .setId("SET_60_GENERAL")
            .setName("将军套装")
            .setLevel(60)
            .threeSetEffect("统御+12%, 防御力+12%")
            .threeSetBonus(Equipment.Attributes.builder().command(150).defense(200).build())
            .sixSetEffect("全属性+20%, 闪避率+5%")
            .sixSetBonus(Equipment.Attributes.builder().attack(280).defense(280).valor(180).command(180).mobility(80).dodge(5.0).build())
            .build());
        
        // 80级套装 - 名将套装
        equipmentSets.put("SET_80_FAMOUS", Equipment.SetInfo.builder()
            .setId("SET_80_FAMOUS")
            .setName("名将套装")
            .setLevel(80)
            .threeSetEffect("攻击力+15%, 暴击率+8%")
            .threeSetBonus(Equipment.Attributes.builder().attack(350).critRate(8.0).build())
            .sixSetEffect("全属性+25%, 暴击伤害+30%")
            .sixSetBonus(Equipment.Attributes.builder().attack(500).defense(450).valor(300).command(300).mobility(150).critDamage(30.0).build())
            .build());
        
        // 100级套装 - 战神套装
        equipmentSets.put("SET_100_WARGOD", Equipment.SetInfo.builder()
            .setId("SET_100_WARGOD")
            .setName("战神套装")
            .setLevel(100)
            .threeSetEffect("攻击力+20%, 武勇+15%")
            .threeSetBonus(Equipment.Attributes.builder().attack(600).valor(400).build())
            .sixSetEffect("全属性+35%, 暴击率+15%, 暴击伤害+50%")
            .sixSetBonus(Equipment.Attributes.builder().attack(900).defense(800).valor(600).command(600).mobility(300).critRate(15.0).critDamage(50.0).build())
            .build());
        
        // ==================== 秘境专属套装 ====================
        
        // 昆仑秘境套装 (40级)
        equipmentSets.put("SET_40_KUNLUN", Equipment.SetInfo.builder()
            .setId("SET_40_KUNLUN")
            .setName("昆仑神装")
            .setLevel(40)
            .threeSetEffect("机动性+15%, 闪避率+5%")
            .threeSetBonus(Equipment.Attributes.builder().mobility(80).dodge(5.0).build())
            .sixSetEffect("攻击力+18%, 机动性+25%, 先手概率+20%")
            .sixSetBonus(Equipment.Attributes.builder().attack(200).mobility(120).dodge(8.0).build())
            .build());
        
        // 蓬莱秘境套装 (60级)
        equipmentSets.put("SET_60_PENGLAI", Equipment.SetInfo.builder()
            .setId("SET_60_PENGLAI")
            .setName("蓬莱仙装")
            .setLevel(60)
            .threeSetEffect("统御+18%, 士兵损耗-10%")
            .threeSetBonus(Equipment.Attributes.builder().command(200).hp(500).build())
            .sixSetEffect("全属性+22%, 战斗后恢复5%兵力")
            .sixSetBonus(Equipment.Attributes.builder().attack(300).defense(350).valor(200).command(250).mobility(100).hp(1000).build())
            .build());
        
        // 瑶池秘境套装 (80级)
        equipmentSets.put("SET_80_YAOCHI", Equipment.SetInfo.builder()
            .setId("SET_80_YAOCHI")
            .setName("瑶池圣装")
            .setLevel(80)
            .threeSetEffect("防御力+20%, 受到伤害-8%")
            .threeSetBonus(Equipment.Attributes.builder().defense(500).hp(800).build())
            .sixSetEffect("全属性+28%, 免疫一次致命伤害")
            .sixSetBonus(Equipment.Attributes.builder().attack(550).defense(700).valor(400).command(450).mobility(200).hp(1500).build())
            .build());
        
        // 九天秘境套装 (100级)
        equipmentSets.put("SET_100_JIUTIAN", Equipment.SetInfo.builder()
            .setId("SET_100_JIUTIAN")
            .setName("九天神装")
            .setLevel(100)
            .threeSetEffect("攻击力+25%, 无视15%防御")
            .threeSetBonus(Equipment.Attributes.builder().attack(800).critRate(12.0).build())
            .sixSetEffect("全属性+40%, 攻击附带真实伤害")
            .sixSetBonus(Equipment.Attributes.builder().attack(1200).defense(1000).valor(800).command(800).mobility(400).critRate(20.0).critDamage(60.0).build())
            .build());
    }
    
    /**
     * 初始化秘境配置
     */
    private void initSecretRealms() {
        // 40级 - 昆仑秘境
        secretRealms.put("KUNLUN", SecretRealm.builder()
            .id("KUNLUN")
            .name("昆仑秘境")
            .description("传说中西王母居住的仙山，蕴含着上古神器的力量")
            .unlockLevel(40)
            .layer(1)
            .icon("🏔️")
            .background("kunlun_bg")
            .goldCost(50)
            .equipmentLevel(40)
            .minQuality(3)  // 蓝色起
            .maxQuality(5)  // 橙色止
            .dailyLimit(5)
            .dropSets(Arrays.asList("SET_40_ELITE", "SET_40_KUNLUN"))
            .lore("昆仑山，天下龙脉之祖，西王母居所。山中藏有无数上古神器，等待有缘人探索。")
            .build());
        
        // 60级 - 蓬莱秘境
        secretRealms.put("PENGLAI", SecretRealm.builder()
            .id("PENGLAI")
            .name("蓬莱秘境")
            .description("东海仙岛，传闻秦始皇曾派徐福寻找的长生之地")
            .unlockLevel(60)
            .layer(2)
            .icon("🏝️")
            .background("penglai_bg")
            .goldCost(100)
            .equipmentLevel(60)
            .minQuality(3)
            .maxQuality(5)
            .dailyLimit(4)
            .dropSets(Arrays.asList("SET_60_GENERAL", "SET_60_PENGLAI"))
            .lore("蓬莱仙岛，烟波浩渺之中若隐若现。岛上仙草遍地，神兽出没，是修仙者梦寐以求之地。")
            .build());
        
        // 80级 - 瑶池秘境
        secretRealms.put("YAOCHI", SecretRealm.builder()
            .id("YAOCHI")
            .name("瑶池秘境")
            .description("天庭瑶池，众仙朝会之所，蟠桃盛会的举办地")
            .unlockLevel(80)
            .layer(3)
            .icon("🌊")
            .background("yaochi_bg")
            .goldCost(200)
            .equipmentLevel(80)
            .minQuality(4)  // 紫色起
            .maxQuality(6)  // 红色止
            .dailyLimit(3)
            .dropSets(Arrays.asList("SET_80_FAMOUS", "SET_80_YAOCHI"))
            .lore("瑶池位于天庭之上，池中有千年蟠桃，食之可得长生。每三千年一次的蟠桃盛会，是诸神仙的盛事。")
            .build());
        
        // 100级 - 九天秘境
        secretRealms.put("JIUTIAN", SecretRealm.builder()
            .id("JIUTIAN")
            .name("九天秘境")
            .description("九重天之上，混沌初开之地，藏有创世神器")
            .unlockLevel(100)
            .layer(4)
            .icon("☁️")
            .background("jiutian_bg")
            .goldCost(500)
            .equipmentLevel(100)
            .minQuality(4)
            .maxQuality(6)
            .dailyLimit(2)
            .dropSets(Arrays.asList("SET_100_WARGOD", "SET_100_JIUTIAN"))
            .lore("九天之上，是盘古开天辟地之所在。这里留存着创世时期的神器，拥有毁天灭地的力量。")
            .build());
        
        // ==================== 未来扩展秘境（预留） ====================
        
        // 120级 - 混沌秘境
        secretRealms.put("HUNDUN", SecretRealm.builder()
            .id("HUNDUN")
            .name("混沌秘境")
            .description("天地未分之前的原始空间，蕴含着无尽的可能")
            .unlockLevel(120)
            .layer(5)
            .icon("🌀")
            .background("hundun_bg")
            .goldCost(800)
            .equipmentLevel(120)
            .minQuality(5)
            .maxQuality(6)
            .dailyLimit(2)
            .dropSets(Arrays.asList("SET_120_CHAOS"))
            .lore("混沌是天地之母，万物之源。这里没有时间，没有空间，只有无尽的混沌之气。")
            .build());
        
        // 140级 - 太虚秘境
        secretRealms.put("TAIXU", SecretRealm.builder()
            .id("TAIXU")
            .name("太虚秘境")
            .description("道家至高境界所在，太虚真人修行之地")
            .unlockLevel(140)
            .layer(6)
            .icon("✨")
            .background("taixu_bg")
            .goldCost(1200)
            .equipmentLevel(140)
            .minQuality(5)
            .maxQuality(6)
            .dailyLimit(2)
            .dropSets(Arrays.asList("SET_140_VOID"))
            .lore("太虚之境，虚无缥缈，是道的终极体现。唯有心境通达者，方能踏入此地。")
            .build());
        
        // 160级 - 鸿蒙秘境
        secretRealms.put("HONGMENG", SecretRealm.builder()
            .id("HONGMENG")
            .name("鸿蒙秘境")
            .description("宇宙诞生之前的原始状态，蕴含着宇宙本源之力")
            .unlockLevel(160)
            .layer(7)
            .icon("💫")
            .background("hongmeng_bg")
            .goldCost(2000)
            .equipmentLevel(160)
            .minQuality(5)
            .maxQuality(6)
            .dailyLimit(1)
            .dropSets(Arrays.asList("SET_160_PRIMORDIAL"))
            .lore("鸿蒙初辟，天地始开。这里是宇宙最初的模样，蕴藏着改变命运的力量。")
            .build());
        
        // 180级 - 无极秘境
        secretRealms.put("WUJI", SecretRealm.builder()
            .id("WUJI")
            .name("无极秘境")
            .description("道生一，一生二，二生三，三生万物。无极是道的起点")
            .unlockLevel(180)
            .layer(8)
            .icon("☯️")
            .background("wuji_bg")
            .goldCost(3000)
            .equipmentLevel(180)
            .minQuality(6)
            .maxQuality(6)
            .dailyLimit(1)
            .dropSets(Arrays.asList("SET_180_INFINITY"))
            .lore("无极而太极，太极动而生阳，静而生阴。这是万物的根源，宇宙的起点。")
            .build());
        
        // 200级 - 造化秘境
        secretRealms.put("ZAOHUA", SecretRealm.builder()
            .id("ZAOHUA")
            .name("造化秘境")
            .description("掌控天地造化的终极秘境，可以改写因果")
            .unlockLevel(200)
            .layer(9)
            .icon("🔮")
            .background("zaohua_bg")
            .goldCost(5000)
            .equipmentLevel(200)
            .minQuality(6)
            .maxQuality(6)
            .dailyLimit(1)
            .dropSets(Arrays.asList("SET_200_CREATION"))
            .lore("造化弄人，天道无常。这里是掌控一切的所在，是超越神明的领域。")
            .build());
    }
    
    /**
     * 初始化材料配置
     */
    private void initMaterials() {
        // 木材
        materials.put("WOOD_COMMON", CraftMaterial.builder()
            .id("WOOD_COMMON").name("普通木材").type("WOOD").icon("🪵").quality(1)
            .description("常见的木材，可用于制作基础装备").build());
        materials.put("WOOD_FINE", CraftMaterial.builder()
            .id("WOOD_FINE").name("精良木材").type("WOOD").icon("🪵").quality(2)
            .description("品质较好的木材，适合制作中级装备").build());
        materials.put("WOOD_RARE", CraftMaterial.builder()
            .id("WOOD_RARE").name("稀有木材").type("WOOD").icon("🪵").quality(3)
            .description("珍贵的木材，用于高级装备制作").build());
        
        // 金属
        materials.put("METAL_IRON", CraftMaterial.builder()
            .id("METAL_IRON").name("铁矿石").type("METAL").icon("⚙️").quality(1)
            .description("常见的铁矿，用于基础武器防具").build());
        materials.put("METAL_STEEL", CraftMaterial.builder()
            .id("METAL_STEEL").name("精钢").type("METAL").icon("⚙️").quality(2)
            .description("精炼的钢材，强度更高").build());
        materials.put("METAL_MYSTIC", CraftMaterial.builder()
            .id("METAL_MYSTIC").name("玄铁").type("METAL").icon("⚙️").quality(3)
            .description("神秘的玄铁，传说可以制作神器").build());
        
        // 纸张
        materials.put("PAPER_COMMON", CraftMaterial.builder()
            .id("PAPER_COMMON").name("普通纸张").type("PAPER").icon("📜").quality(1)
            .description("普通的纸张，用于制作基础符文").build());
        materials.put("PAPER_YELLOW", CraftMaterial.builder()
            .id("PAPER_YELLOW").name("黄纸").type("PAPER").icon("📜").quality(2)
            .description("道士专用的黄纸，可以承载灵力").build());
        materials.put("PAPER_TALISMAN", CraftMaterial.builder()
            .id("PAPER_TALISMAN").name("符纸").type("PAPER").icon("📜").quality(3)
            .description("高级符纸，可以绘制强力符咒").build());
        
        // 布料
        materials.put("CLOTH_COTTON", CraftMaterial.builder()
            .id("CLOTH_COTTON").name("棉布").type("CLOTH").icon("🧵").quality(1)
            .description("普通棉布，用于制作基础衣物").build());
        materials.put("CLOTH_SILK", CraftMaterial.builder()
            .id("CLOTH_SILK").name("丝绸").type("CLOTH").icon("🧵").quality(2)
            .description("上等丝绸，柔软而坚韧").build());
        materials.put("CLOTH_BROCADE", CraftMaterial.builder()
            .id("CLOTH_BROCADE").name("云锦").type("CLOTH").icon("🧵").quality(3)
            .description("皇家专用云锦，华贵无比").build());
        
        // 皮革
        materials.put("LEATHER_COMMON", CraftMaterial.builder()
            .id("LEATHER_COMMON").name("普通皮革").type("LEATHER").icon("🥾").quality(1)
            .description("普通兽皮制成的皮革").build());
        materials.put("LEATHER_BEAST", CraftMaterial.builder()
            .id("LEATHER_BEAST").name("猛兽皮").type("LEATHER").icon("🥾").quality(2)
            .description("猛兽皮制成，更加坚韧").build());
        materials.put("LEATHER_DRAGON", CraftMaterial.builder()
            .id("LEATHER_DRAGON").name("龙鳞皮").type("LEATHER").icon("🥾").quality(3)
            .description("传说中的龙鳞制成，刀枪不入").build());
        
        // 宝石
        materials.put("GEM_JADE", CraftMaterial.builder()
            .id("GEM_JADE").name("玉石").type("GEM").icon("💎").quality(1)
            .description("普通玉石，略有灵气").build());
        materials.put("GEM_CRYSTAL", CraftMaterial.builder()
            .id("GEM_CRYSTAL").name("水晶").type("GEM").icon("💎").quality(2)
            .description("通透的水晶，可以储存灵力").build());
        materials.put("GEM_SPIRIT", CraftMaterial.builder()
            .id("GEM_SPIRIT").name("灵石").type("GEM").icon("💎").quality(3)
            .description("蕴含灵力的神奇宝石").build());
    }
    
    /**
     * 初始化装备模板
     */
    private void initEquipmentTemplates() {
        // 这里可以添加具体的装备模板
        // 根据等级、槽位、品质生成不同的装备
    }
    
    // ==================== Getter方法 ====================
    
    public Equipment.SlotType getSlotType(Integer id) {
        return slotTypes.get(id);
    }
    
    public Map<Integer, Equipment.SlotType> getAllSlotTypes() {
        return new HashMap<>(slotTypes);
    }
    
    public Equipment.Quality getQuality(Integer id) {
        return qualities.get(id);
    }
    
    public Map<Integer, Equipment.Quality> getAllQualities() {
        return new HashMap<>(qualities);
    }
    
    public Equipment.SetInfo getEquipmentSet(String setId) {
        return equipmentSets.get(setId);
    }
    
    public Map<String, Equipment.SetInfo> getAllEquipmentSets() {
        return new HashMap<>(equipmentSets);
    }
    
    public List<Equipment.SetInfo> getEquipmentSetsByLevel(Integer level) {
        List<Equipment.SetInfo> result = new ArrayList<>();
        for (Equipment.SetInfo set : equipmentSets.values()) {
            if (set.getSetLevel().equals(level)) {
                result.add(set);
            }
        }
        return result;
    }
    
    public SecretRealm getSecretRealm(String id) {
        return secretRealms.get(id);
    }
    
    public Map<String, SecretRealm> getAllSecretRealms() {
        return new LinkedHashMap<>(secretRealms);
    }
    
    public List<SecretRealm> getUnlockedSecretRealms(Integer playerLevel) {
        List<SecretRealm> result = new ArrayList<>();
        for (SecretRealm realm : secretRealms.values()) {
            if (realm.getUnlockLevel() <= playerLevel) {
                result.add(realm);
            }
        }
        return result;
    }
    
    public CraftMaterial getMaterial(String id) {
        return materials.get(id);
    }
    
    public Map<String, CraftMaterial> getAllMaterials() {
        return new HashMap<>(materials);
    }
    
    public List<CraftMaterial> getMaterialsByType(String type) {
        List<CraftMaterial> result = new ArrayList<>();
        for (CraftMaterial material : materials.values()) {
            if (material.getType().equals(type)) {
                result.add(material);
            }
        }
        return result;
    }
    
    // ==================== APK装备品质配置 (from EquipQuality_cfg.json) ====================

    public static class EquipQualityLevel {
        public final int id;
        public final String name;
        public final int attrRate;      // 万分比
        public final int acquireRate;   // 万分比
        public final int increaseRate;  // 万分比
        public final int needSilver;
        public EquipQualityLevel(int id, String name, int attrRate, int acquireRate, int increaseRate, int needSilver) {
            this.id = id; this.name = name; this.attrRate = attrRate; this.acquireRate = acquireRate; this.increaseRate = increaseRate; this.needSilver = needSilver;
        }
    }

    private static final EquipQualityLevel[] EQUIP_QUALITY_LEVELS = {
        new EquipQualityLevel(1, "粗糙", 8000, 5890, 8000, 500),
        new EquipQualityLevel(2, "普通", 8500, 3000, 4000, 1000),
        new EquipQualityLevel(3, "优良", 9000, 1000, 2500, 2000),
        new EquipQualityLevel(4, "无暇", 9500,  100, 1000, 3000),
        new EquipQualityLevel(5, "完美", 10000,  10,    0,    0),
    };

    public static EquipQualityLevel getEquipQualityLevel(int qualityId) {
        if (qualityId < 1 || qualityId > 5) return EQUIP_QUALITY_LEVELS[0];
        return EQUIP_QUALITY_LEVELS[qualityId - 1];
    }

    public static int rollEquipQuality() {
        Random rng = new Random();
        int roll = rng.nextInt(10000);
        int cumulative = 0;
        for (EquipQualityLevel lv : EQUIP_QUALITY_LEVELS) {
            cumulative += lv.acquireRate;
            if (roll < cumulative) return lv.id;
        }
        return 1;
    }

    public static String getEquipQualityName(int qualityId) {
        if (qualityId < 1 || qualityId > 5) return "粗糙";
        return EQUIP_QUALITY_LEVELS[qualityId - 1].name;
    }

    // ==================== 装备模板内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EquipmentTemplate {
        private String templateId;
        private String name;
        private Integer slotTypeId;
        private Integer level;
        private Integer baseQuality;
        private String setId;
        private Equipment.Attributes baseAttributes;
    }
}


