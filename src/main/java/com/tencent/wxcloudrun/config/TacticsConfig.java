package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.model.Tactics;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 兵法配置
 */
@Component
public class TacticsConfig {
    
    // 兵法类型配置
    private final Map<Integer, Tactics.TacticsType> tacticsTypes = new HashMap<>();
    
    // 兵法品质配置
    private final Map<Integer, Tactics.TacticsQuality> tacticsQualities = new HashMap<>();
    
    // 兵法模板（所有可用的兵法）
    private final Map<String, Tactics> tacticsTemplates = new HashMap<>();
    
    @PostConstruct
    public void init() {
        initTacticsTypes();
        initTacticsQualities();
        initTacticsTemplates();
    }
    
    private void initTacticsTypes() {
        tacticsTypes.put(1, Tactics.TacticsType.builder()
            .id(1).name("主动").description("主动发动的兵法，消耗行动回合").icon("⚔️").build());
        tacticsTypes.put(2, Tactics.TacticsType.builder()
            .id(2).name("被动").description("满足条件自动触发").icon("🛡️").build());
        tacticsTypes.put(3, Tactics.TacticsType.builder()
            .id(3).name("指挥").description("战斗开始时对己方全体生效").icon("📯").build());
        tacticsTypes.put(4, Tactics.TacticsType.builder()
            .id(4).name("阵法").description("根据阵型提供额外加成").icon("🔄").build());
    }
    
    private void initTacticsQualities() {
        tacticsQualities.put(1, Tactics.TacticsQuality.builder()
            .id(1).name("白色").color("#FFFFFF").multiplier(1.0).build());
        tacticsQualities.put(2, Tactics.TacticsQuality.builder()
            .id(2).name("绿色").color("#00FF00").multiplier(1.2).build());
        tacticsQualities.put(3, Tactics.TacticsQuality.builder()
            .id(3).name("蓝色").color("#0080FF").multiplier(1.5).build());
        tacticsQualities.put(4, Tactics.TacticsQuality.builder()
            .id(4).name("紫色").color("#9370DB").multiplier(1.8).build());
        tacticsQualities.put(5, Tactics.TacticsQuality.builder()
            .id(5).name("橙色").color("#FF8C00").multiplier(2.2).build());
        tacticsQualities.put(6, Tactics.TacticsQuality.builder()
            .id(6).name("红色").color("#DC143C").multiplier(2.8).build());
    }
    
    private void initTacticsTemplates() {
        // ==================== 主动兵法 ====================
        
        // 橙色主动兵法
        addTactics("tactics_001", "落雷", 1, 5, 
            "对敌方单体造成谋略伤害，并有概率造成眩晕", 
            Arrays.asList(
                createDamageEffect("SINGLE_ENEMY", 200, 1.5, "造成{value}点谋略伤害"),
                createDebuffEffect("SINGLE_ENEMY", "stun", 1, 30, "30%概率眩晕1回合")
            ), 35, "普通攻击后触发");
        
        addTactics("tactics_002", "无当飞军", 1, 5, 
            "对敌方全体造成伤害", 
            Arrays.asList(
                createDamageEffect("ALL_ENEMIES", 150, 1.2, "对敌方全体造成{value}点伤害")
            ), 40, "每3回合触发");
        
        addTactics("tactics_003", "虎豹骑", 1, 5, 
            "对敌方单体造成高额物理伤害，并降低目标防御", 
            Arrays.asList(
                createDamageEffect("SINGLE_ENEMY", 280, 2.0, "造成{value}点物理伤害"),
                createDebuffEffect("SINGLE_ENEMY", "defense", 2, -50, "降低目标防御50点，持续2回合")
            ), 30, "攻击时触发");
        
        // 紫色主动兵法
        addTactics("tactics_004", "突击", 1, 4, 
            "对敌方单体造成物理伤害", 
            Arrays.asList(
                createDamageEffect("SINGLE_ENEMY", 150, 1.3, "造成{value}点物理伤害")
            ), 45, "攻击时触发");
        
        addTactics("tactics_005", "箭雨", 1, 4, 
            "对敌方全体造成远程伤害", 
            Arrays.asList(
                createDamageEffect("ALL_ENEMIES", 100, 0.8, "对敌方全体造成{value}点远程伤害")
            ), 35, "每2回合触发");
        
        addTactics("tactics_006", "烈火", 1, 4, 
            "对敌方单体造成持续伤害", 
            Arrays.asList(
                createDamageEffect("SINGLE_ENEMY", 80, 0.6, "造成{value}点火焰伤害"),
                createDebuffEffect("SINGLE_ENEMY", "burn", 3, 30, "灼烧3回合，每回合损失30兵力")
            ), 40, "攻击时触发");
        
        // 蓝色主动兵法
        addTactics("tactics_007", "冲锋", 1, 3, 
            "对敌方单体造成伤害", 
            Arrays.asList(
                createDamageEffect("SINGLE_ENEMY", 100, 1.0, "造成{value}点物理伤害")
            ), 50, "攻击时触发");
        
        addTactics("tactics_008", "射击", 1, 3, 
            "对敌方单体造成远程伤害", 
            Arrays.asList(
                createDamageEffect("SINGLE_ENEMY", 90, 0.9, "造成{value}点远程伤害")
            ), 55, "攻击时触发");
        
        // ==================== 被动兵法 ====================
        
        // 橙色被动兵法
        addTactics("tactics_101", "八门金锁", 2, 5, 
            "战斗开始时，提升己方全体防御和闪避", 
            Arrays.asList(
                createBuffEffect("ALL_ALLIES", "defense", 3, 100, "提升防御100点"),
                createBuffEffect("ALL_ALLIES", "dodge", 3, 15, "提升闪避15%")
            ), 100, "战斗开始时");
        
        addTactics("tactics_102", "藤甲兵", 2, 5, 
            "受到攻击时，有概率反弹伤害", 
            Arrays.asList(
                createDamageEffect("SINGLE_ENEMY", 0, 0.5, "反弹50%受到的伤害")
            ), 25, "受到攻击时");
        
        // 紫色被动兵法
        addTactics("tactics_103", "铁壁", 2, 4, 
            "提升自身防御", 
            Arrays.asList(
                createBuffEffect("SELF", "defense", 0, 80, "永久提升防御80点")
            ), 100, "永久生效");
        
        addTactics("tactics_104", "疾行", 2, 4, 
            "提升自身机动性", 
            Arrays.asList(
                createBuffEffect("SELF", "mobility", 0, 30, "永久提升机动性30点")
            ), 100, "永久生效");
        
        addTactics("tactics_105", "猛攻", 2, 4, 
            "提升自身攻击力", 
            Arrays.asList(
                createBuffEffect("SELF", "attack", 0, 100, "永久提升攻击力100点")
            ), 100, "永久生效");
        
        // 蓝色被动兵法
        addTactics("tactics_106", "坚守", 2, 3, 
            "提升自身防御", 
            Arrays.asList(
                createBuffEffect("SELF", "defense", 0, 50, "永久提升防御50点")
            ), 100, "永久生效");
        
        addTactics("tactics_107", "锐气", 2, 3, 
            "提升自身攻击力", 
            Arrays.asList(
                createBuffEffect("SELF", "attack", 0, 60, "永久提升攻击力60点")
            ), 100, "永久生效");
        
        // ==================== 指挥兵法 ====================
        
        // 橙色指挥兵法
        addTactics("tactics_201", "空城计", 3, 5, 
            "战斗开始时，降低敌方全体攻击力，并有概率使敌方混乱", 
            Arrays.asList(
                createDebuffEffect("ALL_ENEMIES", "attack", 2, -80, "降低敌方攻击80点"),
                createDebuffEffect("ALL_ENEMIES", "confuse", 1, 20, "20%概率混乱1回合")
            ), 100, "战斗开始时");
        
        addTactics("tactics_202", "草船借箭", 3, 5, 
            "战斗开始时，为己方全体恢复兵力", 
            Arrays.asList(
                createHealEffect("ALL_ALLIES", 200, 0.3, "恢复{value}兵力")
            ), 100, "战斗开始时");
        
        // 紫色指挥兵法
        addTactics("tactics_203", "鼓舞", 3, 4, 
            "战斗开始时，提升己方全体攻击力", 
            Arrays.asList(
                createBuffEffect("ALL_ALLIES", "attack", 3, 50, "提升攻击50点，持续3回合")
            ), 100, "战斗开始时");
        
        addTactics("tactics_204", "坚阵", 3, 4, 
            "战斗开始时，提升己方全体防御力", 
            Arrays.asList(
                createBuffEffect("ALL_ALLIES", "defense", 3, 60, "提升防御60点，持续3回合")
            ), 100, "战斗开始时");
        
        // ==================== 阵法兵法 ====================
        
        // 橙色阵法
        addTactics("tactics_301", "八阵图", 4, 5, 
            "阵型加成：前排武将防御+20%，后排武将攻击+15%", 
            Arrays.asList(
                createBuffEffect("FRONT_ROW", "defense", 0, 20, "前排防御+20%"),
                createBuffEffect("BACK_ROW", "attack", 0, 15, "后排攻击+15%")
            ), 100, "阵型生效时");
        
        addTactics("tactics_302", "锥形阵", 4, 5, 
            "阵型加成：全体攻击+10%，中路武将额外+20%攻击", 
            Arrays.asList(
                createBuffEffect("ALL_ALLIES", "attack", 0, 10, "全体攻击+10%"),
                createBuffEffect("MIDDLE", "attack", 0, 20, "中路额外攻击+20%")
            ), 100, "阵型生效时");
        
        // 紫色阵法
        addTactics("tactics_303", "鹤翼阵", 4, 4, 
            "阵型加成：两翼武将机动性+25%", 
            Arrays.asList(
                createBuffEffect("WING", "mobility", 0, 25, "两翼机动+25%")
            ), 100, "阵型生效时");
        
        addTactics("tactics_304", "方圆阵", 4, 4, 
            "阵型加成：全体防御+15%", 
            Arrays.asList(
                createBuffEffect("ALL_ALLIES", "defense", 0, 15, "全体防御+15%")
            ), 100, "阵型生效时");
    }
    
    private void addTactics(String id, String name, int typeId, int qualityId, 
                           String description, List<Tactics.TacticsEffect> effects, 
                           int triggerRate, String triggerCondition) {
        Tactics tactics = Tactics.builder()
            .id(id)
            .name(name)
            .type(tacticsTypes.get(typeId))
            .quality(tacticsQualities.get(qualityId))
            .description(description)
            .icon(getIconByType(typeId))
            .effects(effects)
            .triggerRate(triggerRate)
            .triggerCondition(triggerCondition)
            .learnLevel(qualityId * 10)  // 白10级，绿20级...
            .learnCondition(null)
            .level(1)
            .maxLevel(10)
            .exp(0)
            .maxExp(100)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
        
        tacticsTemplates.put(id, tactics);
    }
    
    private String getIconByType(int typeId) {
        switch (typeId) {
            case 1: return "⚔️";
            case 2: return "🛡️";
            case 3: return "📯";
            case 4: return "🔄";
            default: return "📜";
        }
    }
    
    private Tactics.TacticsEffect createDamageEffect(String targetType, int baseValue, 
                                                      double ratio, String description) {
        return Tactics.TacticsEffect.builder()
            .effectType("DAMAGE")
            .targetType(targetType)
            .attribute("hp")
            .baseValue(baseValue)
            .ratio(ratio)
            .duration(0)
            .description(description)
            .build();
    }
    
    private Tactics.TacticsEffect createHealEffect(String targetType, int baseValue, 
                                                    double ratio, String description) {
        return Tactics.TacticsEffect.builder()
            .effectType("HEAL")
            .targetType(targetType)
            .attribute("hp")
            .baseValue(baseValue)
            .ratio(ratio)
            .duration(0)
            .description(description)
            .build();
    }
    
    private Tactics.TacticsEffect createBuffEffect(String targetType, String attribute, 
                                                    int duration, int value, String description) {
        return Tactics.TacticsEffect.builder()
            .effectType("BUFF")
            .targetType(targetType)
            .attribute(attribute)
            .baseValue(value)
            .ratio(0.0)
            .duration(duration)
            .description(description)
            .build();
    }
    
    private Tactics.TacticsEffect createDebuffEffect(String targetType, String attribute, 
                                                      int duration, int value, String description) {
        return Tactics.TacticsEffect.builder()
            .effectType("DEBUFF")
            .targetType(targetType)
            .attribute(attribute)
            .baseValue(value)
            .ratio(0.0)
            .duration(duration)
            .description(description)
            .build();
    }
    
    // ==================== 公开方法 ====================
    
    public Map<String, Tactics> getAllTactics() {
        return Collections.unmodifiableMap(tacticsTemplates);
    }
    
    public Tactics getTacticsById(String id) {
        return tacticsTemplates.get(id);
    }
    
    public List<Tactics> getTacticsByType(int typeId) {
        List<Tactics> result = new ArrayList<>();
        for (Tactics tactics : tacticsTemplates.values()) {
            if (tactics.getType().getId() == typeId) {
                result.add(tactics);
            }
        }
        return result;
    }
    
    public List<Tactics> getTacticsByQuality(int qualityId) {
        List<Tactics> result = new ArrayList<>();
        for (Tactics tactics : tacticsTemplates.values()) {
            if (tactics.getQuality().getId() == qualityId) {
                result.add(tactics);
            }
        }
        return result;
    }
    
    public Tactics.TacticsType getTacticsType(int typeId) {
        return tacticsTypes.get(typeId);
    }
    
    public Tactics.TacticsQuality getTacticsQuality(int qualityId) {
        return tacticsQualities.get(qualityId);
    }
    
    public Map<Integer, Tactics.TacticsType> getAllTacticsTypes() {
        return Collections.unmodifiableMap(tacticsTypes);
    }
    
    public Map<Integer, Tactics.TacticsQuality> getAllTacticsQualities() {
        return Collections.unmodifiableMap(tacticsQualities);
    }
}
