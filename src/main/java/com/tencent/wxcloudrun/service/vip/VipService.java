package com.tencent.wxcloudrun.service.vip;

import com.tencent.wxcloudrun.dao.VipGiftClaimMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VipService {

    @Autowired private VipGiftClaimMapper claimMapper;
    @Autowired private WarehouseService warehouseService;
    @Autowired private UserResourceService userResourceService;

    // VIP等级阈值（元）
    private static final int[] VIP_THRESHOLDS = {0, 6, 30, 98, 198, 328, 648, 998, 1998, 6000, 20000};

    // 鹰扬套装部件
    private static final String[] YINGYANG_PARTS = {"鹰扬武器", "鹰扬戒指", "鹰扬铠甲", "鹰扬项链", "鹰扬头盔", "鹰扬鞋子"};
    // 虎啸套装部件
    private static final String[] HUXIAO_PARTS = {"虎啸武器", "虎啸戒指", "虎啸铠甲", "虎啸项链", "虎啸头盔", "虎啸鞋子"};
    // 凤鸣套装部件
    private static final String[] FENGMING_PARTS = {"凤鸣武器", "凤鸣戒指", "凤鸣铠甲", "凤鸣项链", "凤鸣头盔", "凤鸣鞋子"};

    /**
     * 获取VIP信息
     */
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
            lv.put("rewards", getRewardDesc(i));
            levels.add(lv);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vipLevel", vipLevel);
        result.put("totalRechargeYuan", totalRechargeYuan);
        result.put("nextThreshold", vipLevel < 10 ? VIP_THRESHOLDS[vipLevel + 1] : 0);
        result.put("levels", levels);
        return result;
    }

    /**
     * 领取VIP礼包
     */
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

    /**
     * 开启宝箱（随机获得对应套装一件）
     */
    public Map<String, Object> openChest(String userId, String chestItemId) {
        String[] parts;
        String setName;
        int setLevel;
        switch (chestItemId) {
            case "7001": parts = YINGYANG_PARTS; setName = "鹰扬"; setLevel = 40; break;
            case "7002": parts = HUXIAO_PARTS; setName = "虎啸"; setLevel = 60; break;
            case "7003": parts = FENGMING_PARTS; setName = "凤鸣"; setLevel = 80; break;
            default: throw new BusinessException("无效的宝箱");
        }

        // 检查仓库中是否有该宝箱
        if (!warehouseService.removeItem(userId, chestItemId, 1)) {
            throw new BusinessException("宝箱数量不足");
        }

        // 随机一件装备
        String part = parts[new Random().nextInt(parts.length)];
        addEquipmentToWarehouse(userId, part, setName, setLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipment", part);
        result.put("setName", setName);
        return result;
    }

    /**
     * 自选套装部件
     */
    public Map<String, Object> selectEquipment(String userId, String setName, String partName) {
        String[] parts;
        int setLevel;
        String selectItemId;
        switch (setName) {
            case "鹰扬": parts = YINGYANG_PARTS; setLevel = 40; selectItemId = "鹰扬自选券"; break;
            case "虎啸": parts = HUXIAO_PARTS; setLevel = 60; selectItemId = "虎啸自选券"; break;
            case "凤鸣": parts = FENGMING_PARTS; setLevel = 80; selectItemId = "凤鸣自选券"; break;
            default: throw new BusinessException("无效的套装");
        }

        boolean validPart = false;
        for (String p : parts) {
            if (p.equals(partName)) { validPart = true; break; }
        }
        if (!validPart) throw new BusinessException("无效的部件名称");

        // 消耗自选券
        if (!warehouseService.removeItem(userId, selectItemId, 1)) {
            throw new BusinessException("自选券不足");
        }

        addEquipmentToWarehouse(userId, partName, setName, setLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipment", partName);
        result.put("setName", setName);
        return result;
    }

    // ======================== 内部方法 ========================

    private void grantRewards(String userId, int level, List<String> rewards) {
        switch (level) {
            case 1:
                addItems(userId, rewards, "6001", "银锭", 1);
                addItems(userId, rewards, "6101", "初级粮食包", 1);
                addItems(userId, rewards, "6201", "初级木材包", 1);
                addItems(userId, rewards, "6301", "初级纸张包", 1);
                break;
            case 2:
                addItems(userId, rewards, "6001", "银锭", 3);
                addItems(userId, rewards, "6101", "初级粮食包", 3);
                addItems(userId, rewards, "6201", "初级木材包", 3);
                addItems(userId, rewards, "6301", "初级纸张包", 3);
                break;
            case 3:
                addItems(userId, rewards, "6001", "银锭", 5);
                addItems(userId, rewards, "6101", "初级粮食包", 5);
                addItems(userId, rewards, "6201", "初级木材包", 5);
                addItems(userId, rewards, "6301", "初级纸张包", 5);
                break;
            case 4:
                addItems(userId, rewards, "6001", "银锭", 10);
                addItems(userId, rewards, "6101", "初级粮食包", 10);
                addItems(userId, rewards, "6201", "初级木材包", 10);
                addItems(userId, rewards, "6301", "初级纸张包", 10);
                addItems(userId, rewards, "6401", "初级声望符", 10);
                addItems(userId, rewards, "7001", "鹰扬宝箱", 1);
                break;
            case 5:
                addItems(userId, rewards, "6001", "银锭", 20);
                addItems(userId, rewards, "6102", "中级粮食包", 5);
                addItems(userId, rewards, "6202", "中级木材包", 5);
                addItems(userId, rewards, "6302", "中级纸张包", 5);
                addItems(userId, rewards, "6402", "中级声望符", 5);
                addItems(userId, rewards, "7001", "鹰扬宝箱", 3);
                break;
            case 6:
                addItems(userId, rewards, "6002", "银砖", 5);
                addItems(userId, rewards, "6102", "中级粮食包", 10);
                addItems(userId, rewards, "6202", "中级木材包", 10);
                addItems(userId, rewards, "6302", "中级纸张包", 10);
                addItems(userId, rewards, "6402", "中级声望符", 10);
                addSelectTicket(userId, rewards, "鹰扬自选券", 2);
                break;
            case 7:
                addItems(userId, rewards, "6002", "银砖", 10);
                addItems(userId, rewards, "6103", "高级粮食包", 5);
                addItems(userId, rewards, "6203", "高级木材包", 5);
                addItems(userId, rewards, "6303", "高级纸张包", 5);
                addItems(userId, rewards, "6403", "高级声望符", 5);
                addItems(userId, rewards, "7002", "虎啸宝箱", 3);
                break;
            case 8:
                addItems(userId, rewards, "6002", "银砖", 20);
                addItems(userId, rewards, "6103", "高级粮食包", 10);
                addItems(userId, rewards, "6203", "高级木材包", 10);
                addItems(userId, rewards, "6303", "高级纸张包", 10);
                addItems(userId, rewards, "6403", "高级声望符", 10);
                addItems(userId, rewards, "2002", "高级招贤令", 2);
                addSelectTicket(userId, rewards, "虎啸自选券", 2);
                break;
            case 9:
                rewards.add("专属武将：貂蝉（弓兵）");
                addItems(userId, rewards, "6002", "银砖", 50);
                addItems(userId, rewards, "6103", "高级粮食包", 20);
                addItems(userId, rewards, "6203", "高级木材包", 20);
                addItems(userId, rewards, "6303", "高级纸张包", 20);
                addItems(userId, rewards, "6403", "高级声望符", 20);
                addItems(userId, rewards, "2002", "高级招贤令", 5);
                addItems(userId, rewards, "7003", "凤鸣宝箱", 3);
                break;
            case 10:
                rewards.add("吕布专属兵法：辕门射戟");
                addItems(userId, rewards, "6002", "银砖", 100);
                addItems(userId, rewards, "6103", "高级粮食包", 50);
                addItems(userId, rewards, "6203", "高级木材包", 50);
                addItems(userId, rewards, "6303", "高级纸张包", 50);
                addItems(userId, rewards, "6403", "高级声望符", 50);
                addItems(userId, rewards, "2002", "高级招贤令", 10);
                addSelectTicket(userId, rewards, "凤鸣自选券", 2);
                break;
        }
    }

    private void addItems(String userId, List<String> rewards, String itemId, String name, int count) {
        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(itemId).itemType("item").name(name)
                .quality(getQuality(itemId)).count(count).maxStack(9999)
                .usable(true).build();
        warehouseService.addItem(userId, item);
        rewards.add(name + " x" + count);
    }

    private void addSelectTicket(String userId, List<String> rewards, String name, int count) {
        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(name).itemType("item").name(name)
                .quality("5").count(count).maxStack(99)
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

        Warehouse.WarehouseItem equip = Warehouse.WarehouseItem.builder()
                .itemId(UUID.randomUUID().toString().substring(0, 8))
                .itemType("equipment").name(name)
                .quality(setLevel >= 80 ? "6" : (setLevel >= 60 ? "5" : "4"))
                .count(1).maxStack(1)
                .description(setName + "套装·" + slot + " Lv." + setLevel)
                .usable(false).build();
        warehouseService.addItem(userId, equip);
    }

    private String getQuality(String itemId) {
        if (itemId.startsWith("70")) return "5";
        if (itemId.equals("6002")) return "4";
        if (itemId.equals("6001")) return "2";
        if (itemId.endsWith("03") || itemId.equals("2002")) return "5";
        if (itemId.endsWith("02")) return "3";
        return "1";
    }

    private List<Map<String, Object>> getRewardDesc(int level) {
        List<Map<String, Object>> list = new ArrayList<>();
        switch (level) {
            case 1: desc(list,"银锭",1); desc(list,"初级粮食包",1); desc(list,"初级木材包",1); desc(list,"初级纸张包",1); break;
            case 2: desc(list,"银锭",3); desc(list,"初级粮食包",3); desc(list,"初级木材包",3); desc(list,"初级纸张包",3); break;
            case 3: desc(list,"银锭",5); desc(list,"初级粮食包",5); desc(list,"初级木材包",5); desc(list,"初级纸张包",5); break;
            case 4: desc(list,"银锭",10); desc(list,"初级粮食包",10); desc(list,"初级木材包",10); desc(list,"初级纸张包",10); desc(list,"初级声望符",10); desc(list,"鹰扬宝箱",1); break;
            case 5: desc(list,"银锭",20); desc(list,"中级粮食包",5); desc(list,"中级木材包",5); desc(list,"中级纸张包",5); desc(list,"中级声望符",5); desc(list,"鹰扬宝箱",3); break;
            case 6: desc(list,"银砖",5); desc(list,"中级粮食包",10); desc(list,"中级木材包",10); desc(list,"中级纸张包",10); desc(list,"中级声望符",10); desc(list,"鹰扬套装自选",2); break;
            case 7: desc(list,"银砖",10); desc(list,"高级粮食包",5); desc(list,"高级木材包",5); desc(list,"高级纸张包",5); desc(list,"高级声望符",5); desc(list,"虎啸宝箱",3); break;
            case 8: desc(list,"银砖",20); desc(list,"高级粮食包",10); desc(list,"高级木材包",10); desc(list,"高级纸张包",10); desc(list,"高级声望符",10); desc(list,"高级招贤令",2); desc(list,"虎啸套装自选",2); break;
            case 9: desc(list,"貂蝉(弓兵)",1); desc(list,"银砖",50); desc(list,"高级粮食包",20); desc(list,"高级木材包",20); desc(list,"高级纸张包",20); desc(list,"高级声望符",20); desc(list,"高级招贤令",5); desc(list,"凤鸣宝箱",3); break;
            case 10: desc(list,"辕门射戟(兵法)",1); desc(list,"银砖",100); desc(list,"高级粮食包",50); desc(list,"高级木材包",50); desc(list,"高级纸张包",50); desc(list,"高级声望符",50); desc(list,"高级招贤令",10); desc(list,"凤鸣套装自选",2); break;
        }
        return list;
    }

    private void desc(List<Map<String, Object>> list, String name, int count) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name); m.put("count", count);
        list.add(m);
    }
}
