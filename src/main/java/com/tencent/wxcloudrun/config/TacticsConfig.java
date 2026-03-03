package com.tencent.wxcloudrun.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 兵法配置 - 9+1种兵种专属兵法
 *
 * 步兵(被动): 方圆阵、长蛇阵、雁行阵
 * 骑兵(主动): 铁骑冲锋、声东击西、擒贼擒王
 * 弓兵(主动/被动): 连射、长虹贯日、落月弓
 * 吕布专属: 辕门射戟
 */
@Component
public class TacticsConfig {

    private final Map<String, TacticsTemplate> templates = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // ========== 步兵兵法（被动，始终生效） ==========
        register(TacticsTemplate.builder()
            .id("t_infantry_1").name("方圆阵").icon("🔄").troopType("步").category("被动")
            .description("步兵阵法，提高防御")
            .effectKey("defBonus").minEffect(2).maxEffect(20).effectUnit("%")
            .effectDesc("提高{value}%的防御")
            .baseTriggerRate(100)
            .craftPaper(3000).craftSilver(8000).upgradePaperPerLv(3000).upgradeSilverPerLv(8000)
            .build());

        register(TacticsTemplate.builder()
            .id("t_infantry_2").name("长蛇阵").icon("🐍").troopType("步").category("被动")
            .description("步兵阵法，提高闪避")
            .effectKey("dodgeBonus").minEffect(1.5).maxEffect(15).effectUnit("%")
            .effectDesc("提高{value}%的闪避")
            .baseTriggerRate(100)
            .craftPaper(3000).craftSilver(8000).upgradePaperPerLv(3000).upgradeSilverPerLv(8000)
            .build());

        register(TacticsTemplate.builder()
            .id("t_infantry_3").name("雁行阵").icon("🦅").troopType("步").category("被动")
            .description("步兵阵法，克制骑兵并增强弓兵伤害")
            .effectKey("yanhang").minEffect(2).maxEffect(20).effectUnit("%")
            .effectDesc("减少骑兵{value}%伤害并反弹受伤的{reflect}%，增加弓兵{archerBonus}%伤害")
            .baseTriggerRate(100)
            .craftPaper(8000).craftSilver(15000).upgradePaperPerLv(6000).upgradeSilverPerLv(12000)
            .build());

        // ========== 骑兵兵法（主动，需发动判定） ==========
        register(TacticsTemplate.builder()
            .id("t_cavalry_1").name("铁骑冲锋").icon("🐎").troopType("骑").category("主动")
            .description("骑兵冲锋，对敌方武将造成额外伤害")
            .effectKey("dmgBonus").minEffect(2).maxEffect(20).effectUnit("%")
            .effectDesc("发动后对敌方武将额外造成{value}%伤害")
            .baseTriggerRate(30)
            .craftPaper(5000).craftSilver(10000).upgradePaperPerLv(5000).upgradeSilverPerLv(10000)
            .build());

        register(TacticsTemplate.builder()
            .id("t_cavalry_2").name("声东击西").icon("🎭").troopType("骑").category("主动")
            .description("骑兵战术，发动后攻击敌方随机弓兵")
            .effectKey("extraTrigger").minEffect(3).maxEffect(30).effectUnit("%")
            .effectDesc("发动后攻击敌方随机弓兵，额外发动概率+{value}%")
            .baseTriggerRate(30)
            .craftPaper(5000).craftSilver(12000).upgradePaperPerLv(4500).upgradeSilverPerLv(10000)
            .build());

        register(TacticsTemplate.builder()
            .id("t_cavalry_3").name("擒贼擒王").icon("👑").troopType("骑").category("主动")
            .description("骑兵战术，发动后攻击敌方等级最高的武将")
            .effectKey("extraTrigger").minEffect(3).maxEffect(30).effectUnit("%")
            .effectDesc("发动后攻击敌方最高武将，额外发动概率+{value}%")
            .baseTriggerRate(30)
            .craftPaper(6000).craftSilver(12000).upgradePaperPerLv(5000).upgradeSilverPerLv(11000)
            .build());

        // ========== 弓兵兵法 ==========
        register(TacticsTemplate.builder()
            .id("t_archer_1").name("连射").icon("🏹").troopType("弓").category("主动")
            .description("弓兵技艺，有概率发动两次攻击")
            .effectKey("doubleShot").minEffect(5).maxEffect(50).effectUnit("%")
            .effectDesc("{value}%的概率发动两次攻击")
            .baseTriggerRate(0)
            .craftPaper(4000).craftSilver(9000).upgradePaperPerLv(4000).upgradeSilverPerLv(9000)
            .build());

        register(TacticsTemplate.builder()
            .id("t_archer_2").name("长虹贯日").icon("☀️").troopType("弓").category("主动")
            .description("弓兵兵法，穿透性攻击，对同一行所有敌人造成伤害")
            .effectKey("aoeDmg").minEffect(45).maxEffect(85).effectUnit("%")
            .effectDesc("对同一行敌人造成{value}%的伤害")
            .baseTriggerRate(30)
            .craftPaper(8000).craftSilver(15000).upgradePaperPerLv(6000).upgradeSilverPerLv(12000)
            .build());

        register(TacticsTemplate.builder()
            .id("t_archer_3").name("落月弓").icon("🌙").troopType("弓").category("被动")
            .description("弓兵绝技，提高攻击伤害")
            .effectKey("dmgBonus").minEffect(5).maxEffect(50).effectUnit("%")
            .effectDesc("提高{value}%的伤害")
            .baseTriggerRate(100)
            .craftPaper(3500).craftSilver(8000).upgradePaperPerLv(3500).upgradeSilverPerLv(8000)
            .build());

        // ========== 吕布专属（VIP10获取，不可制造） ==========
        register(TacticsTemplate.builder()
            .id("t_special_lvbu").name("辕门射戟").icon("🏹").troopType("弓").category("主动")
            .description("吕布专属兵法，对同一行所有敌人造成大量伤害")
            .effectKey("aoeDmg").minEffect(60).maxEffect(100).effectUnit("%")
            .effectDesc("对同一行所有敌人造成{value}%的伤害")
            .baseTriggerRate(40)
            .craftPaper(0).craftSilver(0).upgradePaperPerLv(8000).upgradeSilverPerLv(15000)
            .upgradeGoldPerLv(10)
            .vipExclusive(true).exclusiveGeneralName("吕布")
            .build());
    }

    private void register(TacticsTemplate t) {
        templates.put(t.getId(), t);
    }

    // ==================== 查询方法 ====================

    public Map<String, TacticsTemplate> getAllTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    public TacticsTemplate getById(String id) {
        return templates.get(id);
    }

    public List<TacticsTemplate> getByTroopType(String troopType) {
        List<TacticsTemplate> result = new ArrayList<>();
        for (TacticsTemplate t : templates.values()) {
            if (troopType.equals(t.getTroopType())) result.add(t);
        }
        return result;
    }

    /**
     * 计算指定等级的效果数值: min + (max - min) * (level - 1) / 9
     */
    public static double calcEffect(TacticsTemplate t, int level) {
        if (level < 1) level = 1;
        if (level > 10) level = 10;
        return t.getMinEffect() + (t.getMaxEffect() - t.getMinEffect()) * (level - 1) / 9.0;
    }

    /**
     * 计算指定等级的发动概率
     */
    public static double calcTriggerRate(TacticsTemplate t, int level) {
        double base = t.getBaseTriggerRate();
        if ("extraTrigger".equals(t.getEffectKey())) {
            return base + calcEffect(t, level);
        }
        if ("doubleShot".equals(t.getEffectKey())) {
            return calcEffect(t, level);
        }
        return base;
    }

    /**
     * 计算升级消耗
     */
    public static Map<String, Integer> calcUpgradeCost(TacticsTemplate t, int currentLevel) {
        Map<String, Integer> cost = new HashMap<>();
        int nextLv = currentLevel + 1;
        if (nextLv > 10) return cost;
        if (t.getUpgradePaperPerLv() > 0) cost.put("paper", t.getUpgradePaperPerLv() * nextLv);
        if (t.getUpgradeSilverPerLv() > 0) cost.put("silver", t.getUpgradeSilverPerLv() * nextLv);
        if (t.getUpgradeGoldPerLv() > 0) cost.put("gold", t.getUpgradeGoldPerLv() * nextLv);
        return cost;
    }

    /**
     * 计算制造消耗
     */
    public static Map<String, Integer> calcCraftCost(TacticsTemplate t) {
        Map<String, Integer> cost = new HashMap<>();
        if (t.getCraftPaper() > 0) cost.put("paper", t.getCraftPaper());
        if (t.getCraftSilver() > 0) cost.put("silver", t.getCraftSilver());
        return cost;
    }

    /**
     * 雁行阵特殊效果：反弹比例
     */
    public static double calcYanhangReflect(int level) {
        return 3.0 + (30.0 - 3.0) * (level - 1) / 9.0;
    }

    /**
     * 雁行阵特殊效果：增加弓兵伤害
     */
    public static double calcYanhangArcherBonus(int level) {
        return 5.0 + (50.0 - 5.0) * (level - 1) / 9.0;
    }

    // ==================== 兵法模板数据类 ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TacticsTemplate {
        private String id;
        private String name;
        private String icon;
        private String troopType;       // 步/骑/弓
        private String category;        // 被动/主动
        private String description;
        private String effectKey;       // defBonus/dodgeBonus/yanhang/dmgBonus/extraTrigger/doubleShot/aoeDmg
        private double minEffect;       // 1级效果值
        private double maxEffect;       // 10级效果值
        private String effectUnit;      // %
        private String effectDesc;      // 效果描述模板
        private double baseTriggerRate; // 基础发动率(%)，被动为100
        // 制造消耗
        private int craftPaper;
        private int craftSilver;
        // 升级消耗(每级系数)
        private int upgradePaperPerLv;
        private int upgradeSilverPerLv;
        @lombok.Builder.Default
        private int upgradeGoldPerLv = 0;
        // VIP专属
        @lombok.Builder.Default
        private boolean vipExclusive = false;
        @lombok.Builder.Default
        private String exclusiveGeneralName = null;
    }
}
