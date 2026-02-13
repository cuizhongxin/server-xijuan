package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 道具模型，对应 item 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    
    /**
     * 道具ID
     */
    private Integer itemId;
    
    /**
     * 道具名称
     */
    private String itemName;
    
    /**
     * 道具品质，1~6：白、绿、蓝、紫、橙、红
     */
    private Integer quality;
}
