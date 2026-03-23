package com.tencent.wxcloudrun.model;

import lombok.Data;

@Data
public class StoryProgress {
    private Long id;
    private String userId;
    private Integer serverId;
    private Integer currentNode;
    private Boolean completed;
    private Long createTime;
    private Long updateTime;
}
