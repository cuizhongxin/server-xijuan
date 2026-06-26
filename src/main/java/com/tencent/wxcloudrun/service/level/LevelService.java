package com.tencent.wxcloudrun.service.level;

import com.tencent.wxcloudrun.config.LevelConfig;
import com.tencent.wxcloudrun.model.UserLevel;
import com.tencent.wxcloudrun.repository.UserLevelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 等级服务
 */
@Service
public class LevelService {
    
    private static final Logger logger = LoggerFactory.getLogger(LevelService.class);
    
    @Autowired
    private LevelConfig levelConfig;
    
    @Autowired
    private UserLevelRepository userLevelRepository;
    
    /**
     * 获取用户等级信息
     */
    public UserLevel getUserLevel(String userId) {
        UserLevel userLevel = userLevelRepository.findByUserId(userId);
        if (userLevel == null) {
            userLevel = userLevelRepository.initUserLevel(userId);
        }
        
        // 检查是否需要重置今日经验
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (!today.equals(userLevel.getLastUpdateDate())) {
            userLevel.setTodayExp(0L);
            userLevel.setLastUpdateDate(today);
            userLevelRepository.save(userLevel);
        }
        
        return userLevel;
    }
    
    /**
     * 增加经验值
     * @return 返回升级信息
     */
    public Map<String, Object> addExp(String userId, long exp, String source) {
        UserLevel userLevel = getUserLevel(userId);
        
        int oldLevel = userLevel.getLevel();
        long oldTotalExp = userLevel.getTotalExp();

        // 来源分档衰减：前期尽量保留体感，中后期逐段降速
        double sourceScale = LevelConfig.getSourceExpScale(source, oldLevel);
        long scaledBaseExp = Math.max(1L, Math.round(exp * sourceScale));
        
        // 计算VIP加成（按等级分段封顶，避免与高来源叠加过高）
        int effectiveVipBonus = LevelConfig.getEffectiveVipExpBonus(userLevel.getVipLevel(), oldLevel);
        long bonusExp = Math.round(scaledBaseExp * effectiveVipBonus / 100.0);
        long totalExpGain = scaledBaseExp + bonusExp;
        
        // 增加经验
        userLevel.setTotalExp(oldTotalExp + totalExpGain);
        userLevel.setTodayExp(userLevel.getTodayExp() + totalExpGain);
        
        // 计算新等级
        int newLevel = levelConfig.calculateLevel(userLevel.getTotalExp());
        
        // 检查是否升级
        boolean levelUp = newLevel > oldLevel;
        int levelsGained = newLevel - oldLevel;
        
        // 更新等级信息
        userLevel.setLevel(newLevel);
        userLevel.setCurrentLevelExp(levelConfig.getCurrentLevelExp(userLevel.getTotalExp(), newLevel));
        userLevel.setExpToNextLevel(levelConfig.getExpForNextLevel(newLevel));
        
        userLevelRepository.save(userLevel);
        
        logger.info("用户 {} 获得经验 {} (来源: {}, 原始经验: {}, 来源系数: {}, 衰减后经验: {}, VIP加成: {}%), 等级: {} -> {}",
                   userId, totalExpGain, source, exp, sourceScale, scaledBaseExp, effectiveVipBonus, oldLevel, newLevel);
        
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("expGained", scaledBaseExp);
        result.put("rawExp", exp);
        result.put("sourceScale", sourceScale);
        result.put("bonusExp", bonusExp);
        result.put("totalExpGained", totalExpGain);
        result.put("levelUp", levelUp);
        result.put("levelsGained", levelsGained);
        result.put("oldLevel", oldLevel);
        result.put("newLevel", newLevel);
        result.put("currentLevel", newLevel);
        result.put("currentLevelExp", userLevel.getCurrentLevelExp());
        result.put("expToNextLevel", userLevel.getExpToNextLevel());
        result.put("totalExp", userLevel.getTotalExp());
        result.put("todayExp", userLevel.getTodayExp());
        
        return result;
    }

    /**
     * 副本经验对齐 APK：按关卡经验原值入账，不应用来源衰减与VIP叠加。
     */
    public Map<String, Object> addCampaignExpAlignedApk(String userId, long exp) {
        UserLevel userLevel = getUserLevel(userId);

        int oldLevel = userLevel.getLevel();
        long oldTotalExp = userLevel.getTotalExp();
        long totalExpGain = Math.max(1L, exp);

        userLevel.setTotalExp(oldTotalExp + totalExpGain);
        userLevel.setTodayExp(userLevel.getTodayExp() + totalExpGain);

        int newLevel = levelConfig.calculateLevel(userLevel.getTotalExp());
        boolean levelUp = newLevel > oldLevel;
        int levelsGained = newLevel - oldLevel;

        userLevel.setLevel(newLevel);
        userLevel.setCurrentLevelExp(levelConfig.getCurrentLevelExp(userLevel.getTotalExp(), newLevel));
        userLevel.setExpToNextLevel(levelConfig.getExpForNextLevel(newLevel));

        userLevelRepository.save(userLevel);

        logger.info("用户 {} 获得副本经验(对齐APK) {}，等级: {} -> {}",
                userId, totalExpGain, oldLevel, newLevel);

        Map<String, Object> result = new HashMap<>();
        result.put("expGained", totalExpGain);
        result.put("rawExp", exp);
        result.put("sourceScale", 1.0);
        result.put("bonusExp", 0L);
        result.put("totalExpGained", totalExpGain);
        result.put("levelUp", levelUp);
        result.put("levelsGained", levelsGained);
        result.put("oldLevel", oldLevel);
        result.put("newLevel", newLevel);
        result.put("currentLevel", newLevel);
        result.put("currentLevelExp", userLevel.getCurrentLevelExp());
        result.put("expToNextLevel", userLevel.getExpToNextLevel());
        result.put("totalExp", userLevel.getTotalExp());
        result.put("todayExp", userLevel.getTodayExp());
        return result;
    }
    
    /**
     * 领取每日登录经验
     */
    public Map<String, Object> claimDailyLoginExp(String userId) {
        return addExp(userId, LevelConfig.DAILY_LOGIN_EXP, "每日登录");
    }
    
    /**
     * 完成每日任务获得经验
     */
    public Map<String, Object> completeDailyQuest(String userId) {
        return addExp(userId, LevelConfig.DAILY_QUEST_EXP, "每日任务");
    }
    
    public Map<String, Object> addBattleExp(String userId, int baseExp, boolean isFirstClear) {
        long exp = baseExp;
        if (isFirstClear) {
            exp = (long)(baseExp * LevelConfig.FIRST_CLEAR_BONUS);
        }
        return addExp(userId, exp, isFirstClear ? "副本首通" : "副本战斗");
    }
    
    /**
     * 获取等级配置信息
     */
    public Map<String, Object> getLevelConfigInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("maxLevel", LevelConfig.MAX_LEVEL);
        info.put("levelExpTable", levelConfig.getLevelExpTable());
        info.put("milestoneComparisons", levelConfig.getMilestoneComparisons());
        info.put("highFrequencyExpSources", Arrays.asList(
                "战役战斗(CampaignService)",
                "练兵训练/军事演习(TrainingService/TrainingController)",
                "Boss战(BossWarService)",
                "英雄榜每日奖励(HeroRankService)",
                "使用经验符(WarehouseService)",
                "每日登录/每日任务(LevelService)"
        ));
        info.put("guide", levelConfig.getLevelGuide());
        return info;
    }
    
    /**
     * 设置VIP等级
     */
    public void setVipLevel(String userId, int vipLevel) {
        UserLevel userLevel = getUserLevel(userId);
        userLevel.setVipLevel(vipLevel);
        userLevelRepository.save(userLevel);
        logger.info("用户 {} VIP等级设置为 {}", userId, vipLevel);
    }

}


