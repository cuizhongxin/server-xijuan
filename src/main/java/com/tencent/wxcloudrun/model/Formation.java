package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 阵型实体类
 * 阵型位置：
 *   [0]     [1]     [2]
 *      [3]     [4]
 *          [5]
 * 
 * 位置越靠近中间（序号越大），行动顺序越靠后
 * 战斗时从位置5开始往前，即 5->4->3->2->1->0
 * 如果机动性相同，序号小的先行动
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Formation {
    
    private String id;
    private String odUserId;
    
    /**
     * 阵型名称
     */
    private String name;
    
    /**
     * 阵型槽位（6个位置）
     * 索引0-5对应6个位置
     */
    private List<FormationSlot> slots;
    
    /**
     * 是否为当前使用的阵型
     */
    @Builder.Default
    private Boolean active = true;
    
    private Long createTime;
    private Long updateTime;
    
    /**
     * 阵型槽位
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormationSlot {
        /**
         * 槽位索引 (0-5)
         */
        private Integer index;
        
        /**
         * 武将ID（null表示空位）
         */
        private String generalId;
        
        /**
         * 武将名称（冗余存储，方便显示）
         */
        private String generalName;
        
        /**
         * 武将品质
         */
        private String quality;
        
        /**
         * 武将头像
         */
        private String avatar;
        
        /**
         * 机动性（用于排序）
         */
        private Integer mobility;
    }
}

