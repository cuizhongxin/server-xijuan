package com.tencent.wxcloudrun.service.login;

import com.tencent.wxcloudrun.dao.ContinuousLoginMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ContinuousLoginService {

    private static final Logger logger = LoggerFactory.getLogger(ContinuousLoginService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CYCLE_DAYS = 7;

    private static final Map<Integer, Map<String, Object>> DAY_REWARDS = new LinkedHashMap<>();

    static {
        DAY_REWARDS.put(1, buildReward("gold", "黄金", 100));
        DAY_REWARDS.put(2, buildReward("silver", "白银", 200));
        DAY_REWARDS.put(3, buildReward("food", "粮食", 500));
        DAY_REWARDS.put(4, buildReward("gold", "黄金", 300));
        DAY_REWARDS.put(5, buildReward("diamond", "钻石", 20));
        DAY_REWARDS.put(6, buildReward("gold", "黄金", 500));
        DAY_REWARDS.put(7, buildReward("diamond", "钻石", 50));
    }

    private static Map<String, Object> buildReward(String type, String name, int amount) {
        Map<String, Object> r = new HashMap<>();
        r.put("type", type);
        r.put("name", name);
        r.put("amount", amount);
        return r;
    }

    @Autowired
    private ContinuousLoginMapper continuousLoginMapper;

    @Autowired
    private UserResourceService userResourceService;

    public Map<String, Object> getInfo(String userId) {
        Map<String, Object> record = continuousLoginMapper.findByUserId(userId);

        int consecutiveDays = 0;
        int totalLoginDays = 0;
        String lastLoginDate = null;
        Set<Integer> claimedSet = new HashSet<>();

        if (record != null) {
            consecutiveDays = record.get("consecutiveDays") != null
                    ? ((Number) record.get("consecutiveDays")).intValue() : 0;
            totalLoginDays = record.get("totalLoginDays") != null
                    ? ((Number) record.get("totalLoginDays")).intValue() : 0;
            lastLoginDate = (String) record.get("lastLoginDate");
            claimedSet = parseClaimedDays((String) record.get("claimedDays"));
        }

        int cycleDay = consecutiveDays > 0 ? ((consecutiveDays - 1) % CYCLE_DAYS) + 1 : 0;

        List<Map<String, Object>> rewardList = new ArrayList<>();
        for (int day = 1; day <= CYCLE_DAYS; day++) {
            Map<String, Object> reward = new HashMap<>(DAY_REWARDS.get(day));
            reward.put("day", day);
            reward.put("claimed", claimedSet.contains(day));
            reward.put("canClaim", day <= cycleDay && !claimedSet.contains(day));
            rewardList.add(reward);
        }

        String today = LocalDate.now().format(DATE_FMT);
        boolean checkedInToday = today.equals(lastLoginDate);

        Map<String, Object> result = new HashMap<>();
        result.put("consecutiveDays", consecutiveDays);
        result.put("totalLoginDays", totalLoginDays);
        result.put("lastLoginDate", lastLoginDate);
        result.put("cycleDay", cycleDay);
        result.put("checkedInToday", checkedInToday);
        result.put("rewards", rewardList);
        return result;
    }

    @Transactional
    public Map<String, Object> checkin(String userId) {
        String today = LocalDate.now().format(DATE_FMT);
        Map<String, Object> record = continuousLoginMapper.findByUserId(userId);

        int consecutiveDays;
        int totalLoginDays;
        String claimedDays = "";

        if (record == null) {
            consecutiveDays = 1;
            totalLoginDays = 1;
        } else {
            String lastLoginDate = (String) record.get("lastLoginDate");
            totalLoginDays = record.get("totalLoginDays") != null
                    ? ((Number) record.get("totalLoginDays")).intValue() : 0;
            int prevConsecutive = record.get("consecutiveDays") != null
                    ? ((Number) record.get("consecutiveDays")).intValue() : 0;
            claimedDays = record.get("claimedDays") != null
                    ? (String) record.get("claimedDays") : "";

            if (today.equals(lastLoginDate)) {
                throw new BusinessException(400, "今日已签到");
            }

            if (lastLoginDate != null) {
                LocalDate lastDate = LocalDate.parse(lastLoginDate, DATE_FMT);
                LocalDate todayDate = LocalDate.now();
                long daysBetween = ChronoUnit.DAYS.between(lastDate, todayDate);

                if (daysBetween == 1) {
                    consecutiveDays = prevConsecutive + 1;
                    if (prevConsecutive % CYCLE_DAYS == 0) {
                        claimedDays = "";
                    }
                } else {
                    consecutiveDays = 1;
                    claimedDays = "";
                }
            } else {
                consecutiveDays = 1;
                claimedDays = "";
            }

            totalLoginDays++;
        }

        continuousLoginMapper.upsertRecord(userId, consecutiveDays, today, totalLoginDays, claimedDays);

        logger.info("用户 {} 签到成功, 连续{}天, 累计{}天", userId, consecutiveDays, totalLoginDays);

        Map<String, Object> result = new HashMap<>();
        result.put("consecutiveDays", consecutiveDays);
        result.put("totalLoginDays", totalLoginDays);
        result.put("lastLoginDate", today);
        return result;
    }

    @Transactional
    public Map<String, Object> claim(String userId, int day) {
        if (day < 1 || day > CYCLE_DAYS) {
            throw new BusinessException(400, "无效的领取天数，范围1-" + CYCLE_DAYS);
        }

        Map<String, Object> record = continuousLoginMapper.findByUserId(userId);
        if (record == null) {
            throw new BusinessException(400, "请先签到");
        }

        int consecutiveDays = record.get("consecutiveDays") != null
                ? ((Number) record.get("consecutiveDays")).intValue() : 0;
        String claimedDaysStr = record.get("claimedDays") != null
                ? (String) record.get("claimedDays") : "";
        int totalLoginDays = record.get("totalLoginDays") != null
                ? ((Number) record.get("totalLoginDays")).intValue() : 0;
        String lastLoginDate = (String) record.get("lastLoginDate");

        int cycleDay = consecutiveDays > 0 ? ((consecutiveDays - 1) % CYCLE_DAYS) + 1 : 0;
        if (day > cycleDay) {
            throw new BusinessException(400, "连续登录天数不足，无法领取第" + day + "天奖励");
        }

        Set<Integer> claimedSet = parseClaimedDays(claimedDaysStr);
        if (claimedSet.contains(day)) {
            throw new BusinessException(400, "第" + day + "天奖励已领取");
        }

        Map<String, Object> reward = DAY_REWARDS.get(day);
        String rewardType = (String) reward.get("type");
        int rewardAmount = (int) reward.get("amount");

        grantReward(userId, rewardType, rewardAmount);

        claimedSet.add(day);
        String newClaimedDays = formatClaimedDays(claimedSet);
        continuousLoginMapper.upsertRecord(userId, consecutiveDays, lastLoginDate, totalLoginDays, newClaimedDays);

        logger.info("用户 {} 领取连续登录第{}天奖励: {} x{}", userId, day, rewardType, rewardAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("day", day);
        result.put("reward", reward);
        result.put("claimedDays", new ArrayList<>(claimedSet));
        result.put("userResource", userResourceService.getUserResource(userId));
        return result;
    }

    private void grantReward(String userId, String type, int amount) {
        switch (type) {
            case "gold":
                userResourceService.addGold(userId, amount);
                break;
            case "silver":
                userResourceService.addSilver(userId, amount);
                break;
            case "food":
                userResourceService.addFood(userId, amount);
                break;
            case "wood":
                userResourceService.addWood(userId, amount);
                break;
            case "diamond":
                userResourceService.addDiamond(userId, amount);
                break;
            default:
                logger.warn("未知奖励类型: {}", type);
        }
    }

    private Set<Integer> parseClaimedDays(String claimedDays) {
        Set<Integer> set = new HashSet<>();
        if (claimedDays == null || claimedDays.isEmpty()) {
            return set;
        }
        for (String s : claimedDays.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                set.add(Integer.parseInt(trimmed));
            }
        }
        return set;
    }

    private String formatClaimedDays(Set<Integer> claimedSet) {
        List<Integer> sorted = new ArrayList<>(claimedSet);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(sorted.get(i));
        }
        return sb.toString();
    }
}
