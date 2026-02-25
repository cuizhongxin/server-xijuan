package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyTransport {
    private Long id;
    private String userId;
    private Integer gradeId;
    private String gradeName;
    private Long silverReward;
    private Long paperReward;
    private Long foodReward;
    private Long metalReward;
    private Long startTime;
    private Long endTime;
    @Builder.Default
    private Integer speedUpMinutes = 0;
    @Builder.Default
    private Integer robbedCount = 0;
    @Builder.Default
    private Long robbedSilver = 0L;
    @Builder.Default
    private Long robbedPaper = 0L;
    @Builder.Default
    private Long robbedFood = 0L;
    @Builder.Default
    private Long robbedMetal = 0L;
    @Builder.Default
    private String status = "active";
    private String createDate;
}
