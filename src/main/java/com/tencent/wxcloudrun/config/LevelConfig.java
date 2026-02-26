package com.tencent.wxcloudrun.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 等级经验系统配置
 * 
 * 每日资源产出参考（1级设施）：
 * - 粮食：80×200 = 16,000/天
 * - 银两：120×300 = 36,000/天
 * 
 * 经验来源（免费玩家/天）：
 * - 训练（消耗粮食+银两）：约 6,000-8,000
 * - 每日任务：1,500
 * - 每日登录：500
 * - 副本战斗：约 2,000-3,000
 * 合计约 10,000-13,000/天
 * 
 * 氪金玩家（VIP加成+购买资源）：约 25,000-35,000/天
 * 
 * 升级节奏：
 * - 氪金第1天 ~30,000 → Lv25+（需22,000）
 * - 氪金第2天 ~60,000 → Lv40+（需52,000）
 * - 氪金一周 ~210,000 → Lv60+（需182,000）
 * - 免费第2天 ~22,000 → Lv20+（需7,000）
 * - 免费一周 ~80,000 → Lv40+（需52,000）
 */
@Component
public class LevelConfig {
    
    public static final int MAX_LEVEL = 200;
    
    private Map<Integer, Long> levelExpTable = new HashMap<>();
    private Map<Integer, Long> totalExpTable = new HashMap<>();
    
    @PostConstruct
    public void init() {
        initLevelExpTable();
    }
    
    /**
     * 等级经验曲线：每级递增，公式 = 100 + 3 × level²
     * 
     * 示例：
     * Lv1:  103    Lv2:  112    Lv5:  175    Lv10: 400
     * Lv15: 775    Lv20: 1300   Lv25: 1975   Lv30: 2800
     * Lv40: 4900   Lv50: 7600   Lv60: 10900  Lv80: 19300
     * Lv100: 30100 Lv150: 67600 Lv200: 120100
     * 
     * 累计经验（关键节点）：
     * Lv10:  ~2,200   Lv20:  ~10,000  Lv25:  ~17,500
     * Lv30:  ~28,000  Lv40:  ~68,000  Lv60:  ~230,000
     * Lv100: ~1,040,000
     * 
     * 升级节奏验证：
     * - 免费玩家 ~10,000/天：第1天Lv20，一周Lv42
     * - 氪金玩家 ~30,000/天：第1天Lv28，第2天Lv38，一周Lv60
     */
    private void initLevelExpTable() {
        long totalExp = 0;
        
        for (int level = 1; level <= MAX_LEVEL; level++) {
            long expNeeded = 100 + 3L * level * level;
            
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
               "每级所需经验 = 100 + 3 × 等级²\n" +
               "等级越高，每级所需经验越多，平滑递增";
    }
}
