package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 兵法模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tactics {
    
    private String id;              // 兵法ID
    private String name;            // 兵法名称
    private TacticsType type;       // 兵法类型
    private TacticsQuality quality; // 兵法品质
    private String description;     // 兵法描述
    private String icon;            // 图标
    
    // 效果
    private List<TacticsEffect> effects;  // 兵法效果列表
    private Integer triggerRate;          // 触发概率（百分比）
    private String triggerCondition;      // 触发条件描述
    
    // 学习要求
    private Integer learnLevel;       // 学习所需武将等级
    private String learnCondition;    // 其他学习条件
    
    // 升级相关
    private Integer level;            // 当前等级
    private Integer maxLevel;         // 最大等级
    private Integer exp;              // 当前经验
    private Integer maxExp;           // 升级所需经验
    
    private Long createTime;
    private Long updateTime;
    
    /**
     * 兵法类型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TacticsType {
        private Integer id;
        private String name;        // 主动、被动、指挥、阵法
        private String description;
        private String icon;
    }
    
    /**
     * 兵法品质
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TacticsQuality {
        private Integer id;         // 1-白,2-绿,3-蓝,4-紫,5-橙,6-红
        private String name;
        private String color;
        private Double multiplier;  // 效果倍率
    }
    
    /**
     * 兵法效果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TacticsEffect {
        private String effectType;   // 效果类型：DAMAGE, HEAL, BUFF, DEBUFF
        private String targetType;   // 目标类型：SELF, SINGLE_ENEMY, ALL_ENEMIES, ALLY, ALL_ALLIES
        private String attribute;    // 影响属性：attack, defense, mobility, hp等
        private Integer baseValue;   // 基础数值
        private Double ratio;        // 攻击力/防御力等系数
        private Integer duration;    // 持续回合（0表示即时）
        private String description;  // 效果描述
    }
    
    /**
     * 武将已学习的兵法
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralTactics {
        private String generalId;       // 武将ID
        private Tactics primaryTactics; // 主兵法（固有）
        private Tactics secondaryTactics; // 副兵法（可更换）
        private List<String> learnedTacticsIds; // 已学习的兵法ID列表
    }
}
