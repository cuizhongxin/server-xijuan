package com.tencent.wxcloudrun.service.login;

import com.tencent.wxcloudrun.dao.ContinuousLoginMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 连续登录奖励（7天一周期）
 * 对标 APK LoginPriceLayer.lua：支持 resource / item / general 三种奖励类型
 */
@Service
public class ContinuousLoginService {

    private static final Logger logger = LoggerFactory.getLogger(ContinuousLoginService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CYCLE_DAYS = 7;

    // ═══════ 7天奖励表（对齐APK PropShow.json道具体系）═══════
    // 每天可以有多项奖励（List<RewardEntry>）
    private static final List<List<RewardEntry>> DAY_REWARDS = new ArrayList<>();

    static {
        // Day 1: 白银×3000 + 1级强化石×2
        DAY_REWARDS.add(Arrays.asList(
                res("silver", "白银", 2, 3000),
                item("14001", "1级强化石", 2, 2)
        ));
        // Day 2: 精力丹×2 + 粮食包×1
        DAY_REWARDS.add(Arrays.asList(
                item("11101", "精力丹", 3, 2),
                item("15032", "粮食包", 2, 1)
        ));
        // Day 3: 初级招贤令×1
        DAY_REWARDS.add(Collections.singletonList(
                item("15011", "初级招贤令", 2, 1)
        ));
        // Day 4: 白银×5000 + 免战牌×2
        DAY_REWARDS.add(Arrays.asList(
                res("silver", "白银", 2, 5000),
                item("11102", "免战牌", 3, 2)
        ));
        // Day 5: 中级招贤令×1 + 2级强化石×3
        DAY_REWARDS.add(Arrays.asList(
                item("15012", "中级招贤令", 3, 1),
                item("14002", "2级强化石", 2, 3)
        ));
        // Day 6: 绑金×30 + 初级合成符×3
        DAY_REWARDS.add(Arrays.asList(
                res("boundGold", "绑金", 5, 30),
                item("15001", "初级合成符", 2, 3)
        ));
        // Day 7: 绑金×50 + 高级招贤令×1
        DAY_REWARDS.add(Arrays.asList(
                res("boundGold", "绑金", 5, 50),
                item("15013", "高级招贤令", 4, 1)
        ));
    }

    @Autowired
    private ContinuousLoginMapper continuousLoginMapper;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private WarehouseService warehouseService;

    /**
     * 获取连续登录状态（不做任何修改，纯查询）
     * 对标 APK CM_TRICK_OTHER_INFO(351) → 返回 [day, claimState, rewardList]
     */
    public Map<String, Object> getInfo(String userId) {
        Map<String, Object> record = continuousLoginMapper.findByUserId(userId);

        int consecutiveDays = 0;
        int totalLoginDays = 0;
        String lastLoginDate = null;
        Set<Integer> claimedSet = new HashSet<>();

        if (record != null) {
            consecutiveDays = safeInt(record, "consecutiveDays");
            totalLoginDays = safeInt(record, "totalLoginDays");
            lastLoginDate = (String) record.get("lastLoginDate");
            claimedSet = parseClaimedDays((String) record.get("claimedDays"));
        }

        int cycleDay = consecutiveDays > 0 ? ((consecutiveDays - 1) % CYCLE_DAYS) + 1 : 0;
        String today = LocalDate.now().format(DATE_FMT);
        boolean checkedInToday = today.equals(lastLoginDate);

        // APK claimState: 0=明天可领 1=可领取 2=已领取
        int claimState;
        if (!checkedInToday) {
            claimState = 0; // 今天还未签到
        } else if (cycleDay > 0 && !claimedSet.contains(cycleDay)) {
            claimState = 1; // 已签到但未领取
        } else {
            claimState = 2; // 已领取
        }

        List<Map<String, Object>> rewardList = buildRewardList(claimedSet, cycleDay);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consecutiveDays", consecutiveDays);
        result.put("totalLoginDays", totalLoginDays);
        result.put("cycleDay", cycleDay);
        result.put("checkedInToday", checkedInToday);
        result.put("claimState", claimState);
        result.put("claimedDays", new ArrayList<>(claimedSet));
        result.put("rewards", rewardList);
        return result;
    }

    /**
     * 每日签到（checkin）— 更新连续天数，不发放奖励
     * 对标 APK 进入界面时的登录确认
     */
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
            totalLoginDays = safeInt(record, "totalLoginDays");
            int prevConsecutive = safeInt(record, "consecutiveDays");
            claimedDays = record.get("claimedDays") != null ? (String) record.get("claimedDays") : "";

            if (today.equals(lastLoginDate)) {
                // 今天已签到，直接返回当前信息
                return getInfo(userId);
            }

            if (lastLoginDate != null) {
                LocalDate lastDate = LocalDate.parse(lastLoginDate, DATE_FMT);
                long daysBetween = ChronoUnit.DAYS.between(lastDate, LocalDate.now());

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
        logger.info("【连续登录】用户 {} 签到成功, 连续{}天, 累计{}天", userId, consecutiveDays, totalLoginDays);

        return getInfo(userId);
    }

    /**
     * 领取指定天数的奖励
     * 对标 APK CM_CONTINUELOGIN_PRIZE(352)
     */
    @Transactional
    public Map<String, Object> claim(String userId, int day) {
        if (day < 1 || day > CYCLE_DAYS) {
            throw new BusinessException(400, "无效的领取天数，范围1-" + CYCLE_DAYS);
        }

        Map<String, Object> record = continuousLoginMapper.findByUserId(userId);
        if (record == null) {
            throw new BusinessException(400, "请先签到");
        }

        String today = LocalDate.now().format(DATE_FMT);
        String lastLoginDate = (String) record.get("lastLoginDate");
        if (!today.equals(lastLoginDate)) {
            throw new BusinessException(400, "请先完成今日签到");
        }

        int consecutiveDays = safeInt(record, "consecutiveDays");
        String claimedDaysStr = record.get("claimedDays") != null ? (String) record.get("claimedDays") : "";
        int totalLoginDays = safeInt(record, "totalLoginDays");

        int cycleDay = consecutiveDays > 0 ? ((consecutiveDays - 1) % CYCLE_DAYS) + 1 : 0;
        if (day > cycleDay) {
            throw new BusinessException(400, "连续登录天数不足，无法领取第" + day + "天奖励");
        }

        Set<Integer> claimedSet = parseClaimedDays(claimedDaysStr);
        if (claimedSet.contains(day)) {
            throw new BusinessException(400, "第" + day + "天奖励已领取");
        }

        // 发放奖励
        List<RewardEntry> rewards = DAY_REWARDS.get(day - 1);
        List<Map<String, Object>> granted = new ArrayList<>();
        for (RewardEntry entry : rewards) {
            grantReward(userId, entry);
            granted.add(entry.toDisplayMap());
        }

        claimedSet.add(day);
        String newClaimedDays = formatClaimedDays(claimedSet);
        continuousLoginMapper.upsertRecord(userId, consecutiveDays, lastLoginDate, totalLoginDays, newClaimedDays);

        logger.info("【连续登录】用户 {} 领取第{}天奖励: {}", userId, day, granted);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("day", day);
        result.put("rewards", granted);
        result.put("claimedDays", new ArrayList<>(claimedSet));
        return result;
    }

    // ═══════ 奖励发放 ═══════

    private void grantReward(String userId, RewardEntry entry) {
        if ("resource".equals(entry.rewardType)) {
            switch (entry.resourceKey) {
                case "silver":    userResourceService.addSilver(userId, entry.amount); break;
                case "boundGold": userResourceService.addBoundGold(userId, entry.amount); break;
                case "food":      userResourceService.addFood(userId, entry.amount); break;
                case "metal":     addMetal(userId, entry.amount); break;
                case "paper":     userResourceService.addPaper(userId, entry.amount); break;
                default:
                    logger.warn("【连续登录】未知资源类型: {}", entry.resourceKey);
            }
        } else if ("item".equals(entry.rewardType)) {
            Warehouse.WarehouseItem warehouseItem = Warehouse.WarehouseItem.builder()
                    .itemId(entry.itemId)
                    .itemType("consumable")
                    .name(entry.name)
                    .quality(String.valueOf(entry.quality))
                    .count(entry.amount)
                    .maxStack(9999)
                    .usable(true)
                    .description("连续登录奖励")
                    .build();
            warehouseService.addItem(userId, warehouseItem);
        }
    }

    private void addMetal(String userId, long amount) {
        UserResource res = userResourceService.getUserResource(userId);
        res.setMetal((res.getMetal() != null ? res.getMetal() : 0L) + amount);
        userResourceService.saveResource(res);
    }

    // ═══════ 构建前端展示数据 ═══════

    private List<Map<String, Object>> buildRewardList(Set<Integer> claimedSet, int cycleDay) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int day = 1; day <= CYCLE_DAYS; day++) {
            List<RewardEntry> entries = DAY_REWARDS.get(day - 1);
            Map<String, Object> dayInfo = new LinkedHashMap<>();
            dayInfo.put("day", day);
            dayInfo.put("claimed", claimedSet.contains(day));
            dayInfo.put("canClaim", day <= cycleDay && !claimedSet.contains(day));

            List<Map<String, Object>> items = new ArrayList<>();
            for (RewardEntry entry : entries) {
                items.add(entry.toDisplayMap());
            }
            dayInfo.put("items", items);

            // 简短描述
            StringBuilder desc = new StringBuilder();
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) desc.append("+");
                RewardEntry e = entries.get(i);
                desc.append(e.name).append("×").append(e.amount);
            }
            dayInfo.put("desc", desc.toString());

            list.add(dayInfo);
        }
        return list;
    }

    // ═══════ 奖励定义 ═══════

    private static class RewardEntry {
        final String rewardType; // "resource" or "item"
        final String resourceKey; // silver/boundGold/food/metal/paper (for resource type)
        final String itemId;      // PropShow ID (for item type)
        final String name;
        final int quality;        // 1-6 color quality
        final int amount;

        RewardEntry(String rewardType, String resourceKey, String itemId, String name, int quality, int amount) {
            this.rewardType = rewardType;
            this.resourceKey = resourceKey;
            this.itemId = itemId;
            this.name = name;
            this.quality = quality;
            this.amount = amount;
        }

        Map<String, Object> toDisplayMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", rewardType);
            m.put("name", name);
            m.put("quality", quality);
            m.put("amount", amount);
            if (itemId != null) m.put("itemId", itemId);
            if (resourceKey != null) m.put("resourceKey", resourceKey);
            return m;
        }
    }

    private static RewardEntry res(String resourceKey, String name, int quality, int amount) {
        return new RewardEntry("resource", resourceKey, null, name, quality, amount);
    }

    private static RewardEntry item(String itemId, String name, int quality, int amount) {
        return new RewardEntry("item", null, itemId, name, quality, amount);
    }

    // ═══════ 辅助方法 ═══════

    private static int safeInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private Set<Integer> parseClaimedDays(String claimedDays) {
        Set<Integer> set = new HashSet<>();
        if (claimedDays == null || claimedDays.isEmpty()) return set;
        for (String s : claimedDays.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) set.add(Integer.parseInt(trimmed));
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
