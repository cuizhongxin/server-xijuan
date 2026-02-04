package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 秘境模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretRealm {
    
    /**
     * 秘境ID
     */
    private String id;
    
    /**
     * 秘境名称
     */
    private String name;
    
    /**
     * 秘境描述
     */
    private String description;
    
    /**
     * 解锁等级
     */
    private Integer unlockLevel;
    
    /**
     * 秘境层数
     */
    private Integer layer;
    
    /**
     * 秘境图标
     */
    private String icon;
    
    /**
     * 背景图
     */
    private String background;
    
    /**
     * 探索消耗黄金
     */
    private Integer goldCost;
    
    /**
     * 掉落装备等级
     */
    private Integer equipmentLevel;
    
    /**
     * 掉落装备品质范围（最低品质ID）
     */
    private Integer minQuality;
    
    /**
     * 掉落装备品质范围（最高品质ID）
     */
    private Integer maxQuality;
    
    /**
     * 每日探索次数限制
     */
    private Integer dailyLimit;
    
    /**
     * 可能掉落的套装ID列表
     */
    private List<String> dropSets;
    
    /**
     * 秘境故事背景
     */
    private String lore;
}


