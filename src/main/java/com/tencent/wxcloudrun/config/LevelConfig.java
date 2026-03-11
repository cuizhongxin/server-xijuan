package com.tencent.wxcloudrun.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 君主等级经验系统配置 (对齐APK奖励数据)
 *
 * APK经验来源参考:
 * - 演习: lv5→1000, lv20→2000, lv40→4000, lv60→8000
 * - 战役: lv1-10→300-680, lv40-50→4000-5900, lv80-100→25000-44000
 * - 每日任务/登录: ~2000
 *
 * 公式: 200 + 8 × level²
 *
 * 升级节奏 (日均~10000-30000):
 * - 免费第1天 ~8000  → Lv10 (累计~4900)
 * - 免费一周  ~60000 → Lv25 (累计~44000)
 * - 氪金一周  ~180000→ Lv40 (累计~172000)
 * - 氪金一月  ~800000→ Lv60 (累计~580000)
 * - 长期玩家到lv80    (累计~1.4M)
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
    
    /**
     * 君主等级经验曲线: 200 + 8 × level²
     *
     * 示例:
     * Lv1:  208     Lv5:  400    Lv10: 1000   Lv15: 2000
     * Lv20: 3400    Lv25: 5200   Lv30: 7400   Lv40: 13000
     * Lv50: 20200   Lv60: 29000  Lv70: 39400  Lv80: 51400
     *
     * 累计经验:
     * Lv10: ~4,900   Lv20: ~24,200   Lv30: ~72,000
     * Lv40: ~172,000 Lv50: ~343,000  Lv60: ~580,000
     * Lv70: ~918,000 Lv80: ~1,370,000
     */
    private void initLevelExpTable() {
        long totalExp = 0;
        
        for (int level = 1; level <= MAX_LEVEL; level++) {
            long expNeeded = 200 + 8L * level * level;
            
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
               "每级所需经验 = 200 + 8 × 等级²\n" +
               "等级越高，每级所需经验越多，平滑递增";
    }
}
