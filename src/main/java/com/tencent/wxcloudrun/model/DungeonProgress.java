package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户副本进度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DungeonProgress {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 副本ID
     */
    private String dungeonId;
    
    /**
     * 当前进度（已击败的NPC数量）
     */
    @Builder.Default
    private Integer currentProgress = 0;
    
    /**
     * 已击败的NPC序号集合
     */
    private Set<Integer> defeatedNpcs;
    
    /**
     * 今日已进入次数
     */
    @Builder.Default
    private Integer todayEntries = 0;
    
    /**
     * 上次进入日期（yyyyMMdd格式）
     */
    private String lastEntryDate;
    
    /**
     * 是否已通关
     */
    @Builder.Default
    private Boolean cleared = false;
    
    /**
     * 通关次数
     */
    @Builder.Default
    private Integer clearCount = 0;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
    
    // ====== defeatedNpcs 辅助：逗号分隔存储 ======
    public String getDefeatedNpcsStr() {
        if (defeatedNpcs != null && !defeatedNpcs.isEmpty()) {
            return defeatedNpcs.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return null;
    }
    
    public void setDefeatedNpcsStr(String str) {
        if (str != null && !str.isEmpty()) {
            this.defeatedNpcs = Arrays.stream(str.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        } else {
            this.defeatedNpcs = new HashSet<>();
        }
    }
}


