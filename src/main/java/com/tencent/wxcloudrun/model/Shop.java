package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shop {
    
    private Long id;
    
    /**
     * 商品名称
     */
    private String name;
    
    /**
     * 价格
     */
    private Integer price;
    
    /**
     * 商品描述
     */
    private String desc;
    
    /**
     * 货币类型: gold/silver/vip
     */
    private String currency;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 商品分类: enhance/recruit/resource/consumable/special
     */
    private String classify;
    
    /**
     * 品质 1白色 2绿色 3蓝色 4红色 5紫色 6橙色
     */
    private Integer quality;
    
    /**
     * 关联的道具ID，对应 item 表的 item_id
     */
    private Long itemId;
}
