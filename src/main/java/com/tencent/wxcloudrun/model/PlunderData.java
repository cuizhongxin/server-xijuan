package com.tencent.wxcloudrun.model;

import com.tencent.wxcloudrun.config.PlunderConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户掠夺数据 — 时间自动恢复模型
 * 参考APK: 最大12次，每30分钟恢复1次
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlunderData {

    private String userId;

    /** 当前可用掠夺次数（含购买的额外次数） */
    @Builder.Default
    private Integer availableCount = PlunderConfig.MAX_PLUNDER_COUNT;

    /** 上次恢复计算时间戳(ms) */
    @Builder.Default
    private Long lastRecoverTime = 0L;

    /** 今日已购买额外次数 */
    @Builder.Default
    private Integer todayPurchased = 0;

    /** 上次购买重置日期 yyyyMMdd */
    private String lastResetDate;

    /** 兼容旧字段: 今日已掠夺次数（不再使用，仅保留以防旧数据） */
    @Builder.Default
    private Integer todayCount = 0;
}
