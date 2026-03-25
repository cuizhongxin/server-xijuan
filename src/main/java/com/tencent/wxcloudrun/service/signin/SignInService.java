package com.tencent.wxcloudrun.service.signin;

import com.tencent.wxcloudrun.dao.SignInMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
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
    private static final String MAKEUP_SCROLL_ID = "15061";
    private static final String MAKEUP_SCROLL_NAME = "补签令";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ═══════════════════ 31天签到奖励表（按自然月天数动态截取）═══════════════════
    // 基于APK道具体系设计，所有道具类奖励均为绑定道具
    private static final DayReward[] DAILY_REWARDS = {
        /*  1 */ r("silver",       "白银",        1, 2000),
        /*  2 */ stone(1, 5),
        /*  3 */ item("初级招贤令",  2, 1,  "15011"),
        /*  4 */ r("food",         "粮食",        1, 2000),
        /*  5 */ r("boundGold",    "绑金",        3, 20),
        /*  6 */ r("metal",        "金属",        1, 2000),
        /*  7 */ stone(2, 3),
        /*  8 */ r("silver",       "白银",        1, 3000),
        /*  9 */ item("声望符",     2, 2,  "11001"),
        /* 10 */ item("中级招贤令",  3, 1,  "15012"),
        /* 11 */ r("paper",        "纸张",        1, 2000),
        /* 12 */ item("精力丹",     2, 2,  "11101"),
        /* 13 */ stone(1, 10),
        /* 14 */ r("boundGold",    "绑金",        3, 30),
        /* 15 */ stone(3, 2),
        /* 16 */ r("silver",       "白银",        1, 5000),
        /* 17 */ item("将魂石",     4, 1,  "11026"),
        /* 18 */ r("food",         "粮食",        1, 3000),
        /* 19 */ item("高级声望符",  3, 1,  "11002"),
        /* 20 */ r("boundGold",    "绑金",        4, 50),
        /* 21 */ item("中级招贤令",  3, 2,  "15012"),
        /* 22 */ stone(4, 1),
        /* 23 */ r("silver",       "白银",        1, 8000),
        /* 24 */ r("metal",        "金属",        1, 5000),
        /* 25 */ item("1阶品质石",   4, 1,  "14031"),
        /* 26 */ item("高级招贤令",  5, 1,  "15013"),
        /* 27 */ stone(5, 1),
        /* 28 */ r("boundGold",    "绑金",        4, 50),
        /* 29 */ item("初级招贤令",  2, 3,  "15011"),
        /* 30 */ r("silver",       "白银",        1, 10000),
        /* 31 */ r("boundGold",    "绑金",        4, 30),
    };

    // ═══════════════════ 累签里程碑（绑金奖励，按当月天数动态调整满勤）═══════════════════
    // 满勤里程碑的天数在运行时按当月实际天数计算
    private static final int MILESTONE_TIER1_DAYS = 10;
    private static final long MILESTONE_TIER1_GOLD = 50;
    private static final int MILESTONE_TIER2_DAYS = 20;
    private static final long MILESTONE_TIER2_GOLD = 200;
    private static final long MILESTONE_FULL_GOLD = 500;

    @Autowired
    private SignInMapper signInMapper;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private WarehouseService warehouseService;

    /**
     * 获取签到信息（含每日奖励表和累签里程碑）
     */
    public Map<String, Object> getSignInInfo(String userId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(MONTH_FMT);
        String todayStr = today.format(DATE_FMT);

        List<String> signedDates = signInMapper.findSignedDates(userId, yearMonth);
        boolean todaySigned = signedDates.contains(todayStr);
        int consecutiveDays = calcConsecutiveDays(signedDates, today);
        int makeupUsed = signInMapper.countMonthMakeup(userId, yearMonth);
        List<Integer> claimedMilestones = signInMapper.findClaimedMilestones(userId, yearMonth);

        // 将日期字符串转为日号(1-31)供前端使用
        List<Integer> signedDayNumbers = new ArrayList<>();
        for (String dateStr : signedDates) {
            try {
                LocalDate d = LocalDate.parse(dateStr, DATE_FMT);
                signedDayNumbers.add(d.getDayOfMonth());
            } catch (Exception ignored) {}
        }

        int daysInMonth = today.lengthOfMonth();

        // 构建当月奖励预览（按自然月天数截取）
        List<Map<String, Object>> rewards = new ArrayList<>();
        for (int i = 0; i < daysInMonth && i < DAILY_REWARDS.length; i++) {
            DayReward dr = DAILY_REWARDS[i];
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("day", i + 1);
            rm.put("name", dr.name);
            rm.put("quality", dr.quality);
            rm.put("count", dr.count);
            rm.put("type", dr.type);
            if (dr.itemId != null) rm.put("itemId", dr.itemId);
            rewards.add(rm);
        }

        // 构建累签里程碑状态（满勤天数跟随自然月）
        int totalSigned = signedDates.size();
        int[][] monthMilestones = getMilestonesForMonth(daysInMonth);
        List<Map<String, Object>> milestones = new ArrayList<>();
        for (int[] ms : monthMilestones) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("days", ms[0]);
            mm.put("boundGold", ms[1]);
            mm.put("claimed", claimedMilestones.contains(ms[0]));
            mm.put("reached", totalSigned >= ms[0]);
            milestones.add(mm);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signedDays", signedDayNumbers);
        result.put("totalSigned", totalSigned);
        result.put("daysInMonth", daysInMonth);
        result.put("todaySigned", todaySigned);
        result.put("currentDay", today.getDayOfMonth());
        result.put("consecutiveDays", consecutiveDays);
        result.put("makeupCount", makeupUsed);
        result.put("maxMakeup", MAX_MAKEUP_PER_MONTH);
        result.put("makeupScrollCount", warehouseService.getItemCount(userId, MAKEUP_SCROLL_ID));
        result.put("currentMonth", yearMonth);
        result.put("rewards", rewards);
        result.put("milestones", milestones);
        return result;
    }

    /**
     * 每日签到 — 发放当日对应奖励（绑定道具）+ 检查累签里程碑
     */
    public Map<String, Object> doSignIn(String userId) {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FMT);
        String yearMonth = today.format(MONTH_FMT);

        if (signInMapper.countSignIn(userId, todayStr) > 0) {
            throw new BusinessException("今日已签到");
        }

        signInMapper.insertSignIn(userId, todayStr, 0);

        int dayOfMonth = today.getDayOfMonth();
        DayReward reward = getDayReward(dayOfMonth);

        // 发放当日奖励
        deliverReward(userId, reward);
        logger.info("用户 {} 签到成功, 日期: {}, 奖励: {}x{}", userId, todayStr, reward.name, reward.count);

        // 检查累签里程碑
        List<String> signedDates = signInMapper.findSignedDates(userId, yearMonth);
        int totalSigned = signedDates.size();
        int consecutiveDays = calcConsecutiveDays(signedDates, today);
        Map<String, Object> milestoneReward = checkAndClaimMilestones(userId, yearMonth, totalSigned);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("date", todayStr);
        result.put("dayReward", buildRewardDisplay(reward));
        result.put("milestoneReward", milestoneReward);
        result.put("totalSigned", totalSigned);
        result.put("consecutiveDays", consecutiveDays);
        return result;
    }

    /**
     * 补签 — 同样发放对应日期的奖励（绑定道具）
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

        boolean consumed = warehouseService.consumeItem(userId, MAKEUP_SCROLL_ID, 1);
        if (!consumed) {
            throw new BusinessException(MAKEUP_SCROLL_NAME + "不足，补签需要1个" + MAKEUP_SCROLL_NAME);
        }

        signInMapper.insertSignIn(userId, targetDateStr, 1);

        DayReward reward = getDayReward(day);
        deliverReward(userId, reward);
        logger.info("用户 {} 补签成功, 日期: {}, 消耗补签令x1, 奖励: {}x{}", userId, targetDateStr, reward.name, reward.count);

        List<String> signedDates = signInMapper.findSignedDates(userId, yearMonth);
        int totalSigned = signedDates.size();
        int consecutiveDays = calcConsecutiveDays(signedDates, today);
        Map<String, Object> milestoneReward = checkAndClaimMilestones(userId, yearMonth, totalSigned);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("date", targetDateStr);
        result.put("costItem", MAKEUP_SCROLL_NAME);
        result.put("dayReward", buildRewardDisplay(reward));
        result.put("milestoneReward", milestoneReward);
        result.put("makeupAvailable", MAX_MAKEUP_PER_MONTH - makeupUsed - 1);
        result.put("makeupScrollCount", warehouseService.getItemCount(userId, MAKEUP_SCROLL_ID));
        result.put("totalSigned", totalSigned);
        result.put("consecutiveDays", consecutiveDays);
        return result;
    }

    // ═══════════════════ 奖励发放 ═══════════════════

    private void deliverReward(String userId, DayReward reward) {
        switch (reward.type) {
            case "silver":
                userResourceService.addSilver(userId, reward.count);
                break;
            case "boundGold":
                userResourceService.addBoundGold(userId, reward.count);
                break;
            case "food":
                userResourceService.addFood(userId, reward.count);
                break;
            case "metal":
                addMetal(userId, reward.count);
                break;
            case "paper":
                userResourceService.addPaper(userId, reward.count);
                break;
            case "item":
                addBoundWarehouseItem(userId, reward);
                break;
            default:
                logger.warn("未知奖励类型: {}", reward.type);
                break;
        }
    }

    private void addMetal(String userId, long amount) {
        UserResource res = userResourceService.getUserResource(userId);
        res.setMetal((res.getMetal() != null ? res.getMetal() : 0L) + amount);
        userResourceService.saveResource(res);
    }

    /**
     * 签到道具统一为绑定道具（bound=true），不可在市场交易
     */
    private void addBoundWarehouseItem(String userId, DayReward reward) {
        Warehouse.WarehouseItem warehouseItem = Warehouse.WarehouseItem.builder()
                .itemId(reward.itemId)
                .itemType("consumable")
                .name(reward.name)
                .quality(String.valueOf(reward.quality))
                .count(reward.count)
                .maxStack(9999)
                .usable(true)
                .bound(true)
                .description("签到奖励（绑定）")
                .build();
        warehouseService.addItem(userId, warehouseItem);
    }

    // ═══════════════════ 累签里程碑 ═══════════════════

    private Map<String, Object> checkAndClaimMilestones(String userId, String yearMonth, int totalSigned) {
        int daysInMonth = LocalDate.now().lengthOfMonth();
        int[][] monthMilestones = getMilestonesForMonth(daysInMonth);
        List<Map<String, Object>> claimed = new ArrayList<>();
        for (int[] ms : monthMilestones) {
            int days = ms[0];
            long boundGold = ms[1];
            if (totalSigned >= days && signInMapper.countMilestoneClaimed(userId, yearMonth, days) == 0) {
                signInMapper.insertMilestoneClaim(userId, yearMonth, days, boundGold);
                userResourceService.addBoundGold(userId, boundGold);
                logger.info("用户 {} 达成累签{}天里程碑，奖励绑金 {}", userId, days, boundGold);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("days", days);
                m.put("boundGold", boundGold);
                claimed.add(m);
            }
        }
        return claimed.isEmpty() ? null : (claimed.size() == 1 ? claimed.get(0) : Collections.singletonMap("milestones", claimed));
    }

    /**
     * 根据当月天数动态生成里程碑：10天、20天、满勤
     */
    private static int[][] getMilestonesForMonth(int daysInMonth) {
        return new int[][] {
            { MILESTONE_TIER1_DAYS, (int) MILESTONE_TIER1_GOLD },
            { MILESTONE_TIER2_DAYS, (int) MILESTONE_TIER2_GOLD },
            { daysInMonth,          (int) MILESTONE_FULL_GOLD  },
        };
    }

    // ═══════════════════ 辅助方法 ═══════════════════

    private DayReward getDayReward(int dayOfMonth) {
        int idx = dayOfMonth - 1;
        if (idx >= 0 && idx < DAILY_REWARDS.length) {
            return DAILY_REWARDS[idx];
        }
        return DAILY_REWARDS[idx % DAILY_REWARDS.length];
    }

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

    private Map<String, Object> buildRewardDisplay(DayReward reward) {
        Map<String, Object> rm = new LinkedHashMap<>();
        rm.put("type", reward.type);
        rm.put("name", reward.name);
        rm.put("quality", reward.quality);
        rm.put("count", reward.count);
        return rm;
    }

    // ═══════════════════ 奖励定义 (内部类 + 工厂方法) ═══════════════════

    private static class DayReward {
        final String type;
        final String name;
        final int quality;
        final int count;
        final String itemId;

        DayReward(String type, String name, int quality, int count, String itemId) {
            this.type = type;
            this.name = name;
            this.quality = quality;
            this.count = count;
            this.itemId = itemId;
        }
    }

    private static DayReward r(String type, String name, int quality, int count) {
        return new DayReward(type, name, quality, count, null);
    }

    private static DayReward stone(int level, int count) {
        int quality = Math.min(level, 5);
        return new DayReward("item", level + "级强化石", quality, count, String.valueOf(14000 + level));
    }

    private static DayReward item(String name, int quality, int count, String itemId) {
        return new DayReward("item", name, quality, count, itemId);
    }
}
