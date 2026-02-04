package com.tencent.wxcloudrun.dto;

import lombok.Data;

/**
 * 副本请求DTO
 */
public class DungeonRequest {
    
    /**
     * 进入副本请求
     */
    @Data
    public static class EnterRequest {
        private String dungeonId;
        private Integer playerLevel;
        private Integer currentStamina;
    }
    
    /**
     * 挑战NPC请求
     */
    @Data
    public static class ChallengeRequest {
        private String dungeonId;
        private Integer npcIndex;
        private String generalId;
        private Integer playerLevel;
    }
    
    /**
     * 重置副本请求
     */
    @Data
    public static class ResetRequest {
        private String dungeonId;
    }
}


