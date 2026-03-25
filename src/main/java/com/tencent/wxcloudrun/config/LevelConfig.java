package com.tencent.wxcloudrun.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 君主等级经验系统配置
 *
 * 公式: 100 × level²
 *
 * 设计依据: 通关第一战役(20关, 总经验37500) + 1000额外经验 = 38500, 刚好升到10级
 *
 * 关键节点:
 * Lv1: 100      Lv5: 2,500     Lv10: 10,000    Lv20: 40,000
 * Lv50: 250,000 Lv100: 1,000,000
 *
 * 累计经验:
 * Lv10: 38,500    Lv20: 287,000    Lv50: 4,292,500
 * Lv80: 17,388,000 Lv100: 33,835,000
 */
@Component
public class LevelConfig {
    
    public static final int MAX_LEVEL = 100;
    
    private Map<Integer, Long> levelExpTable = new HashMap<>();
    private Map<Integer, Long> totalExpTable = new HashMap<>();
    
    @PostConstruct
    public void init() {
        initLevelExpTable();
    }
    
    private void initLevelExpTable() {
        long totalExp = 0;
        
        for (int level = 1; level <= MAX_LEVEL; level++) {
            long expNeeded = 100L * level * level;
            
            levelExpTable.put(level, expNeeded);
            totalExp += expNeeded;
            totalExpTable.put(level, totalExp);
        }
    }
    
    public long getExpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0;
        return levelExpTable.getOrDefault(currentLevel + 1, 0L);
    }
    
    public long getTotalExpForLevel(int level) {
        return totalExpTable.getOrDefault(level, 0L);
    }
    
    public int calculateLevel(long totalExp) {
        int level = 1;
        for (int i = 1; i <= MAX_LEVEL; i++) {
            if (totalExp >= totalExpTable.get(i)) {
                level = i;
            } else {
                break;
            }
        }
        return level;
    }
    
    public long getCurrentLevelExp(long totalExp, int currentLevel) {
        if (currentLevel <= 1) return totalExp;
        long prevLevelTotalExp = totalExpTable.getOrDefault(currentLevel - 1, 0L);
        return totalExp - prevLevelTotalExp;
    }
    
    public Map<Integer, Long> getLevelExpTable() {
        return new HashMap<>(levelExpTable);
    }
    
    public Map<Integer, Long> getTotalExpTable() {
        return new HashMap<>(totalExpTable);
    }
    
    // ==================== 经验获取来源配置 ====================
    
    /** 每日任务经验 */
    public static final int DAILY_QUEST_EXP = 1500;
    
    /** 每日登录经验 */
    public static final int DAILY_LOGIN_EXP = 500;
    
    /** 首次通关副本额外经验倍率 */
    public static final double FIRST_CLEAR_BONUS = 2.0;
    
    /** VIP经验加成（按VIP等级） VIP1:10%, VIP10:100% */
    public static int getVipExpBonus(int vipLevel) {
        return vipLevel * 10;
    }
    
    public String getLevelGuide() {
        return "等级经验系统说明：\n" +
               "每级所需经验 = 100 × 等级²\n" +
               "等级越高，每级所需经验越多，平滑递增";
    }
}
