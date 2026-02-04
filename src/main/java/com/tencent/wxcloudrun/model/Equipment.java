package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 装备模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    
    /**
     * 装备唯一ID
     */
    private String id;
    
    /**
     * 所属用户ID
     */
    private String userId;
    
    /**
     * 装备名称
     */
    private String name;
    
    /**
     * 装备槽位类型
     */
    private SlotType slotType;
    
    /**
     * 装备等级（20/40/60/80/100...）
     */
    private Integer level;
    
    /**
     * 装备品质
     */
    private Quality quality;
    
    /**
     * 套装信息
     */
    private SetInfo setInfo;
    
    /**
     * 基础属性
     */
    private Attributes baseAttributes;
    
    /**
     * 附加属性（随机属性）
     */
    private Attributes bonusAttributes;
    
    /**
     * 获取来源
     */
    private Source source;
    
    /**
     * 是否已装备
     */
    @Builder.Default
    private Boolean equipped = false;
    
    /**
     * 装备到的武将ID（如果已装备）
     */
    private String equippedGeneralId;
    
    /**
     * 装备图标
     */
    private String icon;
    
    /**
     * 装备描述
     */
    private String description;
    
    /**
     * 强化等级 (0-10)
     */
    @Builder.Default
    private Integer enhanceLevel = 0;
    
    /**
     * 强化属性加成
     */
    private Attributes enhanceAttributes;
    
    /**
     * 品质值 (0-100，100为完美品质)
     */
    @Builder.Default
    private Integer qualityValue = 0;
    
    /**
     * 品质属性加成（基于品质值计算）
     */
    private Attributes qualityAttributes;
    
    /**
     * 是否锁定（防止误分解）
     */
    @Builder.Default
    private Boolean locked = false;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
    
    // ==================== 内部类 ====================
    
    /**
     * 装备槽位类型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotType {
        /**
         * 槽位ID: 1-主武器, 2-头盔, 3-铠甲, 4-戒指, 5-鞋子, 6-项链
         */
        private Integer id;
        
        /**
         * 槽位名称
         */
        private String name;
        
        /**
         * 槽位图标
         */
        private String icon;
        
        /**
         * 主要属性类型
         */
        private String mainAttribute;
    }
    
    /**
     * 装备品质
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quality {
        /**
         * 品质ID: 1-白色, 2-绿色, 3-蓝色, 4-紫色, 5-橙色, 6-红色(传说)
         */
        private Integer id;
        
        /**
         * 品质名称
         */
        private String name;
        
        /**
         * 品质颜色
         */
        private String color;
        
        /**
         * 属性加成倍率
         */
        private Double multiplier;
        
        /**
         * 品质图标
         */
        private String icon;
    }
    
    /**
     * 套装信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetInfo {
        /**
         * 套装ID
         */
        private String setId;
        
        /**
         * 套装名称
         */
        private String setName;
        
        /**
         * 套装等级
         */
        private Integer setLevel;
        
        /**
         * 3件套效果描述
         */
        private String threeSetEffect;
        
        /**
         * 3件套属性加成
         */
        private Attributes threeSetBonus;
        
        /**
         * 6件套效果描述
         */
        private String sixSetEffect;
        
        /**
         * 6件套属性加成
         */
        private Attributes sixSetBonus;
    }
    
    /**
     * 装备属性
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attributes {
        /**
         * 攻击力
         */
        @Builder.Default
        private Integer attack = 0;
        
        /**
         * 防御力
         */
        @Builder.Default
        private Integer defense = 0;
        
        /**
         * 武勇
         */
        @Builder.Default
        private Integer valor = 0;
        
        /**
         * 统御
         */
        @Builder.Default
        private Integer command = 0;
        
        /**
         * 闪避率
         */
        @Builder.Default
        private Double dodge = 0.0;
        
        /**
         * 机动性
         */
        @Builder.Default
        private Integer mobility = 0;
        
        /**
         * 生命值
         */
        @Builder.Default
        private Integer hp = 0;
        
        /**
         * 暴击率
         */
        @Builder.Default
        private Double critRate = 0.0;
        
        /**
         * 暴击伤害
         */
        @Builder.Default
        private Double critDamage = 0.0;
    }
    
    /**
     * 获取来源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        /**
         * 来源类型: CRAFT(手工制作), DUNGEON(副本), SECRET_REALM(秘境), CHEST(宝箱)
         */
        private String type;
        
        /**
         * 来源名称
         */
        private String name;
        
        /**
         * 来源详情（如副本名、秘境名）
         */
        private String detail;
    }
}


