package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 副本守关NPC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DungeonNpc {
    
    /**
     * NPC序号（从1开始）
     */
    private Integer index;
    
    /**
     * NPC名称
     */
    private String name;
    
    /**
     * NPC等级
     */
    private Integer level;
    
    /**
     * NPC品质ID：1-白色, 2-绿色, 3-蓝色, 4-紫色, 5-橙色, 6-红色
     */
    private Integer qualityId;
    
    /**
     * 品质名称
     */
    private String qualityName;
    
    /**
     * 品质颜色
     */
    private String qualityColor;
    
    /**
     * NPC头像
     */
    private String avatar;
    
    /**
     * NPC图标
     */
    private String icon;
    
    /**
     * 攻击力
     */
    private Integer attack;
    
    /**
     * 防御力
     */
    private Integer defense;
    
    /**
     * 武勇
     */
    private Integer valor;
    
    /**
     * 统御
     */
    private Integer command;
    
    /**
     * 闪避率
     */
    private Double dodge;
    
    /**
     * 机动性
     */
    private Integer mobility;
    
    /**
     * 总战力
     */
    private Integer power;
    
    /**
     * 士兵数量
     */
    private Integer soldiers;
    
    /**
     * 是否掉落装备
     */
    @Builder.Default
    private Boolean dropEquipment = false;
    
    /**
     * 掉落类型：CRAFT(手工), DUNGEON(副本特色), BLUE(蓝色), RED(红色)
     */
    private String dropType;
    
    /**
     * 掉落装备等级
     */
    private Integer dropLevel;
    
    /**
     * 掉落概率（百分比）
     */
    private Integer dropRate;

    /**
     * 可掉落的 equipment_pre ID 列表
     */
    private List<Integer> dropEquipPreIds;
    
    /**
     * 是否是BOSS（最后一个或中间特殊NPC）
     */
    @Builder.Default
    private Boolean isBoss = false;
    
    /**
     * 是否已被击败
     */
    @Builder.Default
    private Boolean defeated = false;
    
    /**
     * 击败后获得的经验值
     */
    @Builder.Default
    private Integer expReward = 0;
}

