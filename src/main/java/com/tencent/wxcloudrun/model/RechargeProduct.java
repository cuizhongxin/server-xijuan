package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 充值商品模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeProduct {
    
    private String id;              // 商品ID
    private String name;            // 商品名称
    private String description;     // 描述
    private String icon;            // 图标
    
    // 价格
    private Long price;             // 价格（分）
    private Long originalPrice;     // 原价（分，用于显示折扣）
    
    // 获得的道具
    private Long goldAmount;        // 获得黄金
    private Long diamondAmount;     // 获得钻石
    private String bonusItems;      // 额外赠送（JSON）
    
    // 显示
    private Boolean hot;            // 是否热销
    private Boolean recommended;    // 是否推荐
    private Integer discount;       // 折扣百分比
    private Integer sortOrder;      // 排序
    
    // 限制
    private Integer dailyLimit;     // 每日限购
    private Integer totalLimit;     // 总限购
    
    private Boolean enabled;        // 是否启用
}
