package com.tencent.wxcloudrun.service.vip;

import com.tencent.wxcloudrun.dao.VipGiftClaimMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.tactics.TacticsService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VipService {

    @Autowired private VipGiftClaimMapper claimMapper;
    @Autowired private WarehouseService warehouseService;
    @Autowired private UserResourceService userResourceService;
    @Autowired private TacticsService tacticsService;

    // VIP等级阈值（元）
    private static final int[] VIP_THRESHOLDS = {0, 6, 30, 98, 198, 328, 648, 998, 1998, 6000, 20000};

    // ── 套装部件 ──
    private static final String[] XUANWU_PARTS  = {"宣武武器", "宣武戒指", "宣武铠甲", "宣武项链", "宣武头盔", "宣武鞋子"};
    private static final String[] YINGYANG_PARTS = {"鹰扬武器", "鹰扬戒指", "鹰扬铠甲", "鹰扬项链", "鹰扬头盔", "鹰扬鞋子"};
    private static final String[] HUXIAO_PARTS   = {"虎啸武器", "虎啸戒指", "虎啸铠甲", "虎啸项链", "虎啸头盔", "虎啸鞋子"};
    private static final String[] TIANLANG_PARTS = {"天狼武器", "天狼戒指", "天狼铠甲", "天狼项链", "天狼头盔", "天狼鞋子"};

    // ── 道具ID（与数据库item表一致）──
    private static final String ID_XUANWU_CHEST  = "11093";  // 宣武宝箱
    private static final String ID_YINGYANG_CHEST = "11091";  // 鹰扬宝箱
    private static final String ID_HUXIAO_CHEST   = "11092";  // 虎啸宝箱
    private static final String ID_TIANLANG_CHEST = "11094";  // 天狼宝箱
    private static final String ID_POJUN_CHEST    = "11095";  // 破军宝箱
    private static final String ID_SELECT_YINGYANG = "16001"; // 指定鹰扬装
    private static final String ID_SELECT_HUXIAO   = "16002"; // 指定虎啸装
    private static final String ID_SELECT_TIANLANG = "16003"; // 指定天狼装

    // ═══════════════════════════════════════════
    //  获取VIP信息（含礼包物品 + 特权描述）
    // ═══════════════════════════════════════════

    public Map<String, Object> getVipInfo(String userId) {
        UserResource resource = userResourceService.getUserResource(userId);
        int vipLevel = resource.getVipLevel() != null ? resource.getVipLevel() : 0;
        long totalRechargeYuan = resource.getTotalRecharge() != null ? resource.getTotalRecharge() / 100 : 0;

        List<Integer> claimed = claimMapper.findClaimedLevels(userId);
        Set<Integer> claimedSet = new HashSet<>(claimed);

        List<Map<String, Object>> levels = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> lv = new LinkedHashMap<>();
            lv.put("level", i);
            lv.put("threshold", VIP_THRESHOLDS[i]);
            lv.put("unlocked", vipLevel >= i);
            lv.put("claimed", claimedSet.contains(i));
            lv.put("giftItems", getGiftItems(i));
            lv.put("privileges", getPrivileges(i));
            levels.add(lv);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vipLevel", vipLevel);
        result.put("totalRechargeYuan", totalRechargeYuan);
        result.put("nextThreshold", vipLevel < 10 ? VIP_THRESHOLDS[vipLevel + 1] : 0);
        result.put("levels", levels);
        return result;
    }

    // ═══════════════════════════════════════════
    //  领取VIP礼包
    // ═══════════════════════════════════════════

    public Map<String, Object> claimGift(String userId, int level) {
        UserResource resource = userResourceService.getUserResource(userId);
        int vipLevel = resource.getVipLevel() != null ? resource.getVipLevel() : 0;
        if (vipLevel < level) throw new BusinessException("VIP等级不足，需要VIP" + level);
        if (level < 1 || level > 10) throw new BusinessException("无效的VIP等级");
        if (claimMapper.countClaim(userId, level) > 0) throw new BusinessException("该等级礼包已领取");

        List<String> rewards = new ArrayList<>();
        grantRewards(userId, level, rewards);

        claimMapper.insertClaim(userId, level, System.currentTimeMillis());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("level", level);
        result.put("rewards", rewards);
        return result;
    }

    // ═══════════════════════════════════════════
    //  开启宝箱（随机获得对应套装一件）
    // ═══════════════════════════════════════════

    public Map<String, Object> openChest(String userId, String chestItemId) {
        String[] parts;
        String setName;
        int setLevel;
        switch (chestItemId) {
            case ID_XUANWU_CHEST:  parts = XUANWU_PARTS;  setName = "宣武"; setLevel = 20; break;
            case ID_YINGYANG_CHEST: parts = YINGYANG_PARTS; setName = "鹰扬"; setLevel = 40; break;
            case ID_HUXIAO_CHEST:   parts = HUXIAO_PARTS;   setName = "虎啸"; setLevel = 60; break;
            case ID_TIANLANG_CHEST: parts = TIANLANG_PARTS; setName = "天狼"; setLevel = 60; break;
            default: throw new BusinessException("无效的宝箱");
        }

        if (!warehouseService.removeItem(userId, chestItemId, 1)) {
            throw new BusinessException("宝箱数量不足");
        }

        String part = parts[new Random().nextInt(parts.length)];
        addEquipmentToWarehouse(userId, part, setName, setLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipment", part);
        result.put("setName", setName);
        return result;
    }

    // ═══════════════════════════════════════════
    //  自选套装部件
    // ═══════════════════════════════════════════

    public Map<String, Object> selectEquipment(String userId, String setName, String partName) {
        String[] parts;
        int setLevel;
        String selectItemId;
        switch (setName) {
            case "鹰扬": parts = YINGYANG_PARTS; setLevel = 40; selectItemId = ID_SELECT_YINGYANG; break;
            case "虎啸": parts = HUXIAO_PARTS;   setLevel = 60; selectItemId = ID_SELECT_HUXIAO; break;
            case "天狼": parts = TIANLANG_PARTS; setLevel = 60; selectItemId = ID_SELECT_TIANLANG; break;
            default: throw new BusinessException("无效的套装");
        }

        boolean validPart = false;
        for (String p : parts) {
            if (p.equals(partName)) { validPart = true; break; }
        }
        if (!validPart) throw new BusinessException("无效的部件名称");

        if (!warehouseService.removeItem(userId, selectItemId, 1)) {
            throw new BusinessException("自选券不足");
        }

        addEquipmentToWarehouse(userId, partName, setName, setLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipment", partName);
        result.put("setName", setName);
        return result;
    }

    // ═══════════════════════════════════════════
    //  APK 对齐的礼包发放（道具ID与数据库一致）
    // ═══════════════════════════════════════════

    private void grantRewards(String userId, int level, List<String> rewards) {
        switch (level) {
            case 1:
                addItems(userId, rewards, "11011", "银条", 5);
                addItems(userId, rewards, "15032", "粮食包", 5);
                addItems(userId, rewards, "11052", "金属堆", 5);
                break;
            case 2:
                addItems(userId, rewards, ID_XUANWU_CHEST, "宣武宝箱", 3);
                userResourceService.addSilver(userId, 100000);
                rewards.add("白银 x100000");
                addItems(userId, rewards, "15052", "军需令", 10);
                break;
            case 3:
                addItems(userId, rewards, ID_XUANWU_CHEST, "宣武宝箱", 3);
                userResourceService.addFood(userId, 50000);
                rewards.add("粮食 x50000");
                addItems(userId, rewards, "15052", "军需令", 20);
                break;
            case 4:
                addItems(userId, rewards, ID_YINGYANG_CHEST, "鹰扬宝箱", 1);
                addItems(userId, rewards, "11001", "声望符", 20);
                addItems(userId, rewards, "15052", "军需令", 30);
                break;
            case 5:
                addItems(userId, rewards, ID_SELECT_YINGYANG, "指定鹰扬装", 1);
                userResourceService.addPaper(userId, 50000);
                rewards.add("纸张 x50000");
                addItems(userId, rewards, "15042", "特训符", 30);
                break;
            case 6:
                addItems(userId, rewards, ID_HUXIAO_CHEST, "虎啸宝箱", 1);
                addItems(userId, rewards, "11002", "高级声望符", 10);
                addItems(userId, rewards, "15052", "军需令", 50);
                break;
            case 7:
                addItems(userId, rewards, ID_SELECT_HUXIAO, "指定虎啸装", 1);
                addItems(userId, rewards, "11002", "高级声望符", 20);
                addItems(userId, rewards, "15052", "军需令", 80);
                break;
            case 8:
                addItems(userId, rewards, ID_HUXIAO_CHEST, "虎啸宝箱", 2);
                addItems(userId, rewards, "11002", "高级声望符", 30);
                addItems(userId, rewards, "15052", "军需令", 80);
                break;
            case 9:
                rewards.add("名将：貂蝉（弓兵）");
                addItems(userId, rewards, ID_SELECT_HUXIAO, "指定虎啸装", 1);
                addItems(userId, rewards, "11002", "高级声望符", 50);
                break;
            case 10:
                tacticsService.grantTactics(userId, "t_special_lvbu", 1);
                rewards.add("吕布专属兵法：战神突击");
                addItems(userId, rewards, "14036", "6阶品质石", 30);
                addItems(userId, rewards, "11002", "高级声望符", 50);
                break;
        }
    }

    // ═══════════════════════════════════════════
    //  礼包物品描述（前端展示用）
    // ═══════════════════════════════════════════

    private List<Map<String, Object>> getGiftItems(int level) {
        List<Map<String, Object>> items = new ArrayList<>();
        switch (level) {
            case 1:
                gi(items, "11011", "银条", 5, "11011.jpg");
                gi(items, "15032", "粮食包", 5, "15032.jpg");
                gi(items, "11052", "金属堆", 5, "11052.jpg");
                break;
            case 2:
                gi(items, ID_XUANWU_CHEST, "宣武宝箱", 3, "15206.jpg");
                gi(items, null, "白银", 100000, "11013.jpg");
                gi(items, "15052", "军需令", 10, "15052.jpg");
                break;
            case 3:
                gi(items, ID_XUANWU_CHEST, "宣武宝箱", 3, "15206.jpg");
                gi(items, null, "粮食", 50000, "11053.jpg");
                gi(items, "15052", "军需令", 20, "15052.jpg");
                break;
            case 4:
                gi(items, ID_YINGYANG_CHEST, "鹰扬宝箱", 1, "15216.jpg");
                gi(items, "11001", "声望符", 20, "11001.jpg");
                gi(items, "15052", "军需令", 30, "15052.jpg");
                break;
            case 5:
                gi(items, ID_SELECT_YINGYANG, "指定鹰扬装", 1, "15216.jpg");
                gi(items, null, "纸张", 50000, "11054.jpg");
                gi(items, "15042", "特训符", 30, "15042.jpg");
                break;
            case 6:
                gi(items, ID_HUXIAO_CHEST, "虎啸宝箱", 1, "15217.jpg");
                gi(items, "11002", "高级声望符", 10, "11002.jpg");
                gi(items, "15052", "军需令", 50, "15052.jpg");
                break;
            case 7:
                gi(items, ID_SELECT_HUXIAO, "指定虎啸装", 1, "15217.jpg");
                gi(items, "11002", "高级声望符", 20, "11002.jpg");
                gi(items, "15052", "军需令", 80, "15052.jpg");
                break;
            case 8:
                gi(items, ID_HUXIAO_CHEST, "虎啸宝箱", 2, "15217.jpg");
                gi(items, "11002", "高级声望符", 30, "11002.jpg");
                gi(items, "15052", "军需令", 80, "15052.jpg");
                break;
            case 9:
                gi(items, null, "名将：貂蝉", 1, null);
                gi(items, ID_SELECT_HUXIAO, "指定虎啸装", 1, "15217.jpg");
                gi(items, "11002", "高级声望符", 50, "11002.jpg");
                break;
            case 10:
                gi(items, null, "兵法：战神突击", 1, null);
                gi(items, "14036", "6阶品质石", 30, "14036.jpg");
                gi(items, "11002", "高级声望符", 50, "11002.jpg");
                break;
        }
        return items;
    }

    private void gi(List<Map<String, Object>> list, String itemId, String name, int count, String icon) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (itemId != null) m.put("itemId", itemId);
        m.put("name", name);
        m.put("count", count);
        if (icon != null) m.put("icon", icon);
        list.add(m);
    }

    // ═══════════════════════════════════════════
    //  APK 对齐的特权描述
    // ═══════════════════════════════════════════

    private List<Map<String, Object>> getPrivileges(int level) {
        String[][] data;
        switch (level) {
            case 1: data = new String[][]{
                {"将领", "可招募将领数量＋1"},
                {"boss战", "降低boss战5%休整时间"}
            }; break;
            case 2: data = new String[][]{
                {"将领", "将领数量＋1"},
                {"boss战", "降低boss战5%休整时间"},
                {"军需", "每天可拦截军需次数＋1"},
                {"训练", "训练和特训效果1.2倍"}
            }; break;
            case 3: data = new String[][]{
                {"将领", "将领数量＋2"},
                {"boss战", "降低boss战10%休整时间"},
                {"军需", "拦截军需次数＋1"},
                {"训练", "训练和特训效果1.2倍"},
                {"英雄榜", "挑战次数增加到17次"},
                {"国战", "紧急转移轮空次数降低为2次"}
            }; break;
            case 4: data = new String[][]{
                {"将领", "将领数量＋2"},
                {"boss战", "降低boss战10%休整时间"},
                {"军需", "拦截军需次数＋2"},
                {"训练", "训练和特训效果1.4倍"},
                {"英雄榜", "挑战次数增加到18次"}
            }; break;
            case 5: data = new String[][]{
                {"将领", "将领数量＋3"},
                {"boss战", "降低boss战15%休整时间"},
                {"军需", "拦截军需次数＋2"},
                {"训练", "训练和特训效果1.4倍"},
                {"英雄榜", "挑战次数增加到19次"}
            }; break;
            case 6: data = new String[][]{
                {"将领", "将领数量＋3"},
                {"boss战", "降低boss战15%休整时间"},
                {"军需", "拦截军需次数＋3"},
                {"训练", "训练和特训效果1.6倍"},
                {"英雄榜", "挑战次数增加到20次"},
                {"国战", "紧急转移轮空次数降低为2次"}
            }; break;
            case 7: data = new String[][]{
                {"将领", "将领数量＋4"},
                {"boss战", "降低boss战20%休整时间"},
                {"军需", "拦截军需次数＋3"},
                {"训练", "训练和特训效果1.6倍"},
                {"英雄榜", "挑战次数增加到22次"},
                {"国战", "紧急转移轮空次数降低为2次"}
            }; break;
            case 8: data = new String[][]{
                {"将领", "将领数量＋4"},
                {"boss战", "降低boss战20%休整时间"},
                {"军需", "拦截军需次数＋5"},
                {"训练", "训练和特训效果1.8倍"},
                {"英雄榜", "挑战次数增加到22次"},
                {"国战", "紧急转移轮空次数降低为1次"}
            }; break;
            case 9: data = new String[][]{
                {"将领", "将领数量＋5"},
                {"boss战", "降低boss战25%休整时间"},
                {"军需", "拦截军需次数＋5"},
                {"训练", "训练和特训效果1.8倍"},
                {"英雄榜", "挑战次数增加到25次"},
                {"国战", "紧急转移轮空次数降低为1次"}
            }; break;
            case 10: data = new String[][]{
                {"将领", "将领数量＋5"},
                {"boss战", "降低boss战25%休整时间"},
                {"军需", "拦截军需次数＋7"},
                {"训练", "训练和特训效果2倍"},
                {"英雄榜", "挑战次数增加到25次"},
                {"国战", "紧急转移轮空次数降低为0次"}
            }; break;
            default: data = new String[0][];
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] d : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", d[0]);
            m.put("desc", d[1]);
            result.add(m);
        }
        return result;
    }

    // ═══════════════════════════════════════════
    //  内部工具方法
    // ═══════════════════════════════════════════

    private void addItems(String userId, List<String> rewards, String itemId, String name, int count) {
        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(itemId).itemType("item").name(name)
                .icon(getIcon(itemId))
                .quality(getQuality(itemId)).count(count).maxStack(9999)
                .usable(true).build();
        warehouseService.addItem(userId, item);
        rewards.add(name + " x" + count);
    }

    private void addEquipmentToWarehouse(String userId, String name, String setName, int setLevel) {
        String slot = "武器";
        if (name.contains("戒指")) slot = "戒指";
        else if (name.contains("铠甲")) slot = "铠甲";
        else if (name.contains("项链")) slot = "项链";
        else if (name.contains("头盔")) slot = "头盔";
        else if (name.contains("鞋子")) slot = "鞋子";

        String quality = setLevel >= 80 ? "6" : (setLevel >= 60 ? "5" : (setLevel >= 40 ? "4" : "3"));
        Warehouse.WarehouseItem equip = Warehouse.WarehouseItem.builder()
                .itemId(UUID.randomUUID().toString().substring(0, 8))
                .itemType("equipment").name(name)
                .quality(quality)
                .count(1).maxStack(1)
                .description(setName + "套装·" + slot + " Lv." + setLevel)
                .usable(false).build();
        warehouseService.addItem(userId, equip);
    }

    private String getIcon(String itemId) {
        switch (itemId) {
            case "11091": return "15216.jpg";  // 鹰扬宝箱
            case "11092": return "15217.jpg";  // 虎啸宝箱
            case "11093": return "15206.jpg";  // 宣武宝箱
            case "11094": return "15212.jpg";  // 天狼宝箱
            case "11095": return "15213.jpg";  // 破军宝箱
            case "16001": return "15216.jpg";  // 指定鹰扬装
            case "16002": return "15217.jpg";  // 指定虎啸装
            case "16003": return "15212.jpg";  // 指定天狼装
            default:      return itemId + ".jpg";
        }
    }

    private String getQuality(String itemId) {
        switch (itemId) {
            case "11011": case "11052": case "15032": case "15031":
                return "2";
            case "11001": case "15052": case "15042": case "11093": case "16001":
                return "3";
            case "11002": case "14036": case "11092": case "16002":
                return "4";
            default:
                return "1";
        }
    }
}
