package com.tencent.wxcloudrun.service.signin;

import com.tencent.wxcloudrun.dao.SignInMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SignInService {

    private static final Logger logger = LoggerFactory.getLogger(SignInService.class);

    private static final int MAX_MAKEUP_PER_MONTH = 3;
    private static final long MAKEUP_COST_GOLD = 200;
    private static final long SIGN_REWARD_GOLD = 100;
    private static final long SIGN_REWARD_SILVER = 500;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private SignInMapper signInMapper;

    @Autowired
    private UserResourceService userResourceService;

    /**
     * 获取签到信息
     */
    public Map<String, Object> getSignInInfo(String userId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(MONTH_FMT);
        String todayStr = today.format(DATE_FMT);

        List<String> signedDates = signInMapper.findSignedDates(userId, yearMonth);
        int totalSigned = signedDates.size();
        boolean todaySigned = signedDates.contains(todayStr);
        int consecutiveDays = calcConsecutiveDays(signedDates, today);
        int makeupUsed = signInMapper.countMonthMakeup(userId, yearMonth);
        int makeupAvailable = MAX_MAKEUP_PER_MONTH - makeupUsed;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signedDates", signedDates);
        result.put("totalSigned", totalSigned);
        result.put("todaySigned", todaySigned);
        result.put("consecutiveDays", consecutiveDays);
        result.put("makeupAvailable", Math.max(0, makeupAvailable));
        result.put("makeupCost", MAKEUP_COST_GOLD);
        result.put("currentMonth", yearMonth);
        return result;
    }

    /**
     * 每日签到
     */
    public Map<String, Object> doSignIn(String userId) {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FMT);

        if (signInMapper.countSignIn(userId, todayStr) > 0) {
            throw new BusinessException("今日已签到");
        }

        signInMapper.insertSignIn(userId, todayStr, 0);
        logger.info("用户 {} 签到成功, 日期: {}", userId, todayStr);

        userResourceService.addGold(userId, SIGN_REWARD_GOLD);
        userResourceService.addSilver(userId, SIGN_REWARD_SILVER);

        String yearMonth = today.format(MONTH_FMT);
        List<String> signedDates = signInMapper.findSignedDates(userId, yearMonth);
        int consecutiveDays = calcConsecutiveDays(signedDates, today);

        long bonusGold = 0;
        if (consecutiveDays == 7) {
            bonusGold = 500;
            userResourceService.addGold(userId, bonusGold);
            logger.info("用户 {} 连续签到7天，额外奖励黄金 {}", userId, bonusGold);
        } else if (consecutiveDays == 30) {
            bonusGold = 2000;
            userResourceService.addGold(userId, bonusGold);
            logger.info("用户 {} 连续签到30天，额外奖励黄金 {}", userId, bonusGold);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("date", todayStr);
        result.put("rewards", buildRewardList(SIGN_REWARD_GOLD + bonusGold, SIGN_REWARD_SILVER));
        result.put("consecutiveDays", consecutiveDays);
        result.put("totalSigned", signedDates.size());
        return result;
    }

    /**
     * 补签
     */
    public Map<String, Object> doMakeupSign(String userId, int day) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(MONTH_FMT);

        if (day < 1 || day > today.getDayOfMonth()) {
            throw new BusinessException("补签日期无效");
        }

        LocalDate targetDate = today.withDayOfMonth(day);
        if (!targetDate.isBefore(today)) {
            throw new BusinessException("只能补签过去的日期");
        }

        String targetDateStr = targetDate.format(DATE_FMT);

        if (signInMapper.countSignIn(userId, targetDateStr) > 0) {
            throw new BusinessException("该日期已签到，无需补签");
        }

        int makeupUsed = signInMapper.countMonthMakeup(userId, yearMonth);
        if (makeupUsed >= MAX_MAKEUP_PER_MONTH) {
            throw new BusinessException("本月补签次数已用完（最多" + MAX_MAKEUP_PER_MONTH + "次）");
        }

        boolean consumed = userResourceService.consumeGold(userId, MAKEUP_COST_GOLD);
        if (!consumed) {
            throw new BusinessException("黄金不足，补签需要" + MAKEUP_COST_GOLD + "黄金");
        }

        signInMapper.insertSignIn(userId, targetDateStr, 1);
        logger.info("用户 {} 补签成功, 日期: {}, 花费黄金: {}", userId, targetDateStr, MAKEUP_COST_GOLD);

        List<String> signedDates = signInMapper.findSignedDates(userId, yearMonth);
        int consecutiveDays = calcConsecutiveDays(signedDates, today);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("date", targetDateStr);
        result.put("cost", MAKEUP_COST_GOLD);
        result.put("makeupAvailable", MAX_MAKEUP_PER_MONTH - makeupUsed - 1);
        result.put("consecutiveDays", consecutiveDays);
        result.put("totalSigned", signedDates.size());
        return result;
    }

    /**
     * 从今天往前数连续签到天数
     */
    private int calcConsecutiveDays(List<String> signedDates, LocalDate today) {
        Set<String> dateSet = new HashSet<>(signedDates);
        int count = 0;
        LocalDate d = today;
        while (dateSet.contains(d.format(DATE_FMT))) {
            count++;
            d = d.minusDays(1);
        }
        return count;
    }

    private List<Map<String, Object>> buildRewardList(long gold, long silver) {
        List<Map<String, Object>> rewards = new ArrayList<>();
        Map<String, Object> goldReward = new LinkedHashMap<>();
        goldReward.put("type", "gold");
        goldReward.put("name", "黄金");
        goldReward.put("amount", gold);
        rewards.add(goldReward);

        Map<String, Object> silverReward = new LinkedHashMap<>();
        silverReward.put("type", "silver");
        silverReward.put("name", "白银");
        silverReward.put("amount", silver);
        rewards.add(silverReward);
        return rewards;
    }
}
