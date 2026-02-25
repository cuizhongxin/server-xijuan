package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyData {
    private String userId;
    @Builder.Default
    private Integer todayTransport = 0;
    @Builder.Default
    private Integer todayRobbery = 0;
    @Builder.Default
    private Integer currentGradeId = 1;
    @Builder.Default
    private Integer refreshTokens = 0;
    private String lastResetDate;
    private Long updateTime;
}
