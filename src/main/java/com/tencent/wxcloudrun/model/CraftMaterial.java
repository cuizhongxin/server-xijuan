package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 制作材料模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CraftMaterial {
    
    /**
     * 材料ID
     */
    private String id;
    
    /**
     * 材料名称
     */
    private String name;
    
    /**
     * 材料类型: WOOD(木材), PAPER(纸张), METAL(金属), CLOTH(布料), LEATHER(皮革), GEM(宝石)
     */
    private String type;
    
    /**
     * 材料图标
     */
    private String icon;
    
    /**
     * 材料描述
     */
    private String description;
    
    /**
     * 材料品质
     */
    private Integer quality;
    
    /**
     * 堆叠上限
     */
    @Builder.Default
    private Integer maxStack = 999;
}


