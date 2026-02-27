package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户掠夺每日数据（只保存次数和购买信息，记录已独立到 plunder_record 表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlunderData {

    private String userId;

    /** 今日已掠夺次数 */
    @Builder.Default
    private Integer todayCount = 0;

    /** 今日已购买次数 */
    @Builder.Default
    private Integer todayPurchased = 0;

    /** 上次重置日期 yyyyMMdd */
    private String lastResetDate;

    /**
     * 获取今日可用掠夺次数
     */
    public int getAvailableCount() {
        int total = 24 + (todayPurchased != null ? todayPurchased : 0);
        int used = todayCount != null ? todayCount : 0;
        return Math.max(0, total - used);
    }
}
