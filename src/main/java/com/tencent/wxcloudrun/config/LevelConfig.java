package com.tencent.wxcloudrun.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 君主等级经验系统配置
 *
 * 经验曲线：100 × level² × 阶段系数
 *
 * 设计目标：
 * 1) 保持前20级节奏基本不变；
 * 2) 从21级开始逐段抬高经验需求，显著放缓中后期升级速度；
 * 3) 所有参数集中配置，便于后续继续调参。
 */
@Component
public class LevelConfig {
    
    public static final int MAX_LEVEL = 100;
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(10, 20, 30, 40, 50, 60);
    
    private Map<Integer, Long> levelExpTable = new HashMap<>();
    private Map<Integer, Long> totalExpTable = new HashMap<>();
    
    @PostConstruct
    public void init() {
        initLevelExpTable();
    }
    
    private void initLevelExpTable() {
        levelExpTable.clear();
        totalExpTable.clear();
        long totalExp = 0;

        for (int level = 1; level <= MAX_LEVEL; level++) {
            long expNeeded = Math.round(100L * level * level * getLevelCurveMultiplier(level));
            levelExpTable.put(level, expNeeded);
        }

        // 1级为初始等级，达到1级所需累计经验应为0，避免经验条超阈值不升级
        totalExpTable.put(1, 0L);
        for (int level = 2; level <= MAX_LEVEL; level++) {
            totalExp += levelExpTable.getOrDefault(level, 0L);
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
        if (currentLevel <= 1) return Math.max(0L, totalExp);
        long currentLevelStartExp = totalExpTable.getOrDefault(currentLevel, 0L);
        return Math.max(0L, totalExp - currentLevelStartExp);
    }
    
    public Map<Integer, Long> getLevelExpTable() {
        return new HashMap<>(levelExpTable);
    }
    
    public Map<Integer, Long> getTotalExpTable() {
        return new HashMap<>(totalExpTable);
    }
    
    // ==================== 经验获取来源配置 ====================

    /**
     * 等级曲线分段系数：
     * - 1~20：保持原节奏
     * - 21~40：大幅放缓
     * - 41~60：显著放缓
     * - 61~80：强放缓
     * - 81~100：超强放缓
     */
    public static double getLevelCurveMultiplier(int level) {
        if (level <= 20) return 1.00;
        if (level <= 40) return 8.50;
        if (level <= 60) return 14.00;
        if (level <= 80) return 22.00;
        return 32.00;
    }
    
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

    /**
     * VIP经验加成上限（按等级分段）：
     * 前期保留较高体感，中后期逐步收敛，降低“来源×VIP”叠加爆发。
     */
    public static int getVipExpBonusCapByLevel(int level) {
        if (level <= 20) return 50;
        if (level <= 40) return 10;
        if (level <= 60) return 8;
        return 5;
    }

    /**
     * 获取生效VIP经验加成百分比（已封顶）。
     */
    public static int getEffectiveVipExpBonus(int vipLevel, int level) {
        int rawBonus = getVipExpBonus(vipLevel);
        int capBonus = getVipExpBonusCapByLevel(level);
        return Math.min(rawBonus, capBonus);
    }

    /**
     * 来源分档经验系数（配置优先）：
     * - 通过来源分类 + 等级段控制节奏，避免在业务层硬编码“硬砍”
     * - 目标：前期流畅，中后期逐段放缓
     */
    public static double getSourceExpScale(String source, int level) {
        final String src = source == null ? "" : source;
        final String sourceType = resolveSourceType(src);

        if ("battle".equals(sourceType)) {
            if (level <= 20) return 1.00;
            if (level <= 40) return 0.10;
            if (level <= 60) return 0.08;
            return 0.06;
        }

        if ("training".equals(sourceType)) {
            if (level <= 20) return 0.95;
            if (level <= 40) return 0.08;
            if (level <= 60) return 0.06;
            return 0.05;
        }

        if ("boss".equals(sourceType)) {
            if (level <= 20) return 1.00;
            if (level <= 40) return 0.12;
            if (level <= 60) return 0.09;
            return 0.07;
        }

        if ("rank".equals(sourceType)) {
            if (level <= 20) return 1.00;
            if (level <= 40) return 0.10;
            if (level <= 60) return 0.08;
            return 0.06;
        }

        if ("item".equals(sourceType)) {
            if (level <= 20) return 1.00;
            if (level <= 40) return 0.08;
            if (level <= 60) return 0.06;
            return 0.05;
        }

        if ("daily".equals(sourceType)) {
            if (level <= 20) return 1.00;
            if (level <= 40) return 0.12;
            if (level <= 60) return 0.10;
            return 0.08;
        }

        // 兜底：未知来源保守衰减
        if (level <= 20) return 1.00;
        if (level <= 40) return 0.10;
        if (level <= 60) return 0.08;
        return 0.06;
    }

    private static String resolveSourceType(String source) {
        if (source.contains("战役") || source.contains("副本")) return "battle";
        if (source.contains("军事演习") || source.contains("练兵训练")) return "training";
        if (source.contains("Boss战")) return "boss";
        if (source.contains("英雄榜")) return "rank";
        if (source.contains("使用经验符")) return "item";
        if (source.contains("每日登录") || source.contains("每日任务")) return "daily";
        return "other";
    }

    private long getLegacyExpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0L;
        long nextLevel = currentLevel + 1L;
        return 100L * nextLevel * nextLevel;
    }

    public Map<Integer, Map<String, Object>> getMilestoneComparisons() {
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        final long sampleBattleRawExp = 2000L;

        for (Integer level : MILESTONE_LEVELS) {
            long legacyNeed = getLegacyExpForNextLevel(level);
            long currentNeed = getExpForNextLevel(level);
            double needDeltaPct = legacyNeed <= 0 ? 0 : ((currentNeed - legacyNeed) * 100.0 / legacyNeed);
            double battleScale = getSourceExpScale("战役战斗", level);
            long battleGain = Math.max(1L, Math.round(sampleBattleRawExp * battleScale));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("legacyNeedExp", legacyNeed);
            item.put("currentNeedExp", currentNeed);
            item.put("needDeltaPct", Math.round(needDeltaPct * 100.0) / 100.0);
            item.put("sampleBattleRawExp", sampleBattleRawExp);
            item.put("sampleBattleScale", battleScale);
            item.put("sampleBattleGain", battleGain);
            result.put(level, item);
        }

        return result;
    }
    
    public String getLevelGuide() {
        return "等级经验系统说明：\n" +
               "每级所需经验 = 100 × 等级² × 阶段系数\n" +
               "阶段系数: 1~20级=1.00, 21~40级=8.50, 41~60级=14.00, 61~80级=22.00, 81~100级=32.00\n" +
               "来源经验系数按来源类型 + 等级段强收敛，确保20级后升级显著放缓\n" +
               "VIP经验加成按等级分段封顶: 1~20级50%, 21~40级10%, 41~60级8%, 61~100级5%";
    }
}
