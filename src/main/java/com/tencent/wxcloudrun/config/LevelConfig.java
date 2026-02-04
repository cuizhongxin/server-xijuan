package com.tencent.wxcloudrun.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 等级经验系统配置
 * 
 * 升级节奏设计：
 * - 第1天：普通玩家可达20级，小氪玩家可达30级
 * - 第2天：普通玩家可达30级，氪金玩家可达40级
 * - 第30天：氪金玩家可达100级，普通玩家可达80级
 * - 第60天：普通玩家可达100级
 * 
 * 假设每日游玩：
 * - 普通玩家每日可获得约3000-4000经验
 * - 氪金玩家每日可获得约6000-8000经验
 */
@Component
public class LevelConfig {
    
    // 最大等级
    public static final int MAX_LEVEL = 200;
    
    // 每级所需经验表
    private Map<Integer, Long> levelExpTable = new HashMap<>();
    
    // 累计经验表（升到该等级需要的总经验）
    private Map<Integer, Long> totalExpTable = new HashMap<>();
    
    @PostConstruct
    public void init() {
        initLevelExpTable();
    }
    
    /**
     * 初始化等级经验表
     * 
     * 经验曲线设计：
     * 1-10级：每级100经验（快速入门）
     * 11-20级：每级200经验（第一天目标）
     * 21-30级：每级400经验（第一天氪金/第二天普通目标）
     * 31-40级：每级800经验（第二天氪金目标）
     * 41-60级：每级1500经验（稳步提升）
     * 61-80级：每级2500经验（30天普通目标）
     * 81-100级：每级4000经验（30天氪金/60天普通目标）
     * 101-150级：每级6000经验（后期挑战）
     * 151-200级：每级10000经验（终极挑战）
     */
    private void initLevelExpTable() {
        long totalExp = 0;
        
        for (int level = 1; level <= MAX_LEVEL; level++) {
            long expNeeded;
            
            if (level <= 10) {
                // 1-10级：每级100经验，总计1000
                expNeeded = 100;
            } else if (level <= 20) {
                // 11-20级：每级200经验，总计2000，累计3000
                expNeeded = 200;
            } else if (level <= 30) {
                // 21-30级：每级400经验，总计4000，累计7000
                expNeeded = 400;
            } else if (level <= 40) {
                // 31-40级：每级800经验，总计8000，累计15000
                expNeeded = 800;
            } else if (level <= 60) {
                // 41-60级：每级1500经验，总计30000，累计45000
                expNeeded = 1500;
            } else if (level <= 80) {
                // 61-80级：每级2500经验，总计50000，累计95000
                expNeeded = 2500;
            } else if (level <= 100) {
                // 81-100级：每级4000经验，总计80000，累计175000
                expNeeded = 4000;
            } else if (level <= 150) {
                // 101-150级：每级6000经验，总计300000，累计475000
                expNeeded = 6000;
            } else {
                // 151-200级：每级10000经验，总计500000，累计975000
                expNeeded = 10000;
            }
            
            levelExpTable.put(level, expNeeded);
            totalExp += expNeeded;
            totalExpTable.put(level, totalExp);
        }
    }
    
    /**
     * 获取升到下一级需要的经验
     */
    public long getExpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return 0;
        }
        return levelExpTable.getOrDefault(currentLevel + 1, 0L);
    }
    
    /**
     * 获取升到指定等级需要的总经验
     */
    public long getTotalExpForLevel(int level) {
        return totalExpTable.getOrDefault(level, 0L);
    }
    
    /**
     * 根据总经验计算等级
     */
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
    
    /**
     * 计算当前等级的经验进度
     */
    public long getCurrentLevelExp(long totalExp, int currentLevel) {
        if (currentLevel <= 1) {
            return totalExp;
        }
        long prevLevelTotalExp = totalExpTable.getOrDefault(currentLevel - 1, 0L);
        return totalExp - prevLevelTotalExp;
    }
    
    /**
     * 获取等级经验表（用于前端显示）
     */
    public Map<Integer, Long> getLevelExpTable() {
        return new HashMap<>(levelExpTable);
    }
    
    /**
     * 获取累计经验表
     */
    public Map<Integer, Long> getTotalExpTable() {
        return new HashMap<>(totalExpTable);
    }
    
    // ==================== 经验获取来源配置 ====================
    
    /**
     * 每日任务经验（普通玩家主要经验来源）
     */
    public static final int DAILY_QUEST_EXP = 1000;
    
    /**
     * 每日登录经验
     */
    public static final int DAILY_LOGIN_EXP = 200;
    
    /**
     * 首次通关副本额外经验倍率
     */
    public static final double FIRST_CLEAR_BONUS = 2.0;
    
    /**
     * VIP经验加成（按VIP等级）
     */
    public static int getVipExpBonus(int vipLevel) {
        // VIP1: 10%, VIP2: 20%, ... VIP10: 100%
        return vipLevel * 10;
    }
    
    // ==================== 经验曲线说明 ====================
    
    /**
     * 获取经验曲线说明
     * 
     * 普通玩家升级时间预估：
     * - 1-20级：约1天（每日3000经验，需3000经验）
     * - 21-30级：约1天（需额外4000经验）
     * - 31-40级：约2天（需额外8000经验）
     * - 41-60级：约8天（需额外30000经验）
     * - 61-80级：约14天（需额外50000经验）
     * - 81-100级：约22天（需额外80000经验）
     * 总计约30天达到80级，约50天达到100级
     * 
     * 氪金玩家升级时间预估（经验获取量翻倍）：
     * - 1-30级：约1天
     * - 31-40级：约1天
     * - 41-80级：约10天
     * - 81-100级：约10天
     * 总计约22天达到100级
     */
    public String getLevelGuide() {
        return "等级经验系统说明：\n" +
               "1-10级：每级100经验\n" +
               "11-20级：每级200经验\n" +
               "21-30级：每级400经验\n" +
               "31-40级：每级800经验\n" +
               "41-60级：每级1500经验\n" +
               "61-80级：每级2500经验\n" +
               "81-100级：每级4000经验\n" +
               "101-150级：每级6000经验\n" +
               "151-200级：每级10000经验";
    }
}


