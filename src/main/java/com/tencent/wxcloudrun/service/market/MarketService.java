package com.tencent.wxcloudrun.service.market;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.MarketMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketService {

    private static final Logger logger = LoggerFactory.getLogger(MarketService.class);

    private static final int MAX_LISTINGS = 10;
    private static final long MAX_PRICE = 50000;
    private static final long COMMISSION_RATE = 50;
    private static final int PAGE_SIZE = 12;
    private static final long EXPIRE_DAYS = 7;

    @Autowired
    private MarketMapper marketMapper;
    @Autowired
    private UserResourceService resourceService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private EquipmentRepository equipmentRepository;

    /**
     * 浏览市场（分页 + 分类 + 搜索）
     */
    public Map<String, Object> browse(String itemType, String keyword, int page) {
        int offset = page * PAGE_SIZE;
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<Map<String, Object>> listings = marketMapper.findActive(itemType, kw, offset, PAGE_SIZE);
        int total = marketMapper.countActive(itemType, kw);

        Map<String, Object> result = new HashMap<>();
        result.put("listings", listings);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", PAGE_SIZE);
        result.put("totalPages", Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE));
        return result;
    }

    /**
     * 我的挂牌
     */
    public Map<String, Object> myListings(String userId) {
        List<Map<String, Object>> listings = marketMapper.findBySeller(userId, 50);
        int activeCount = marketMapper.countActiveBySeller(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("listings", listings);
        result.put("activeCount", activeCount);
        result.put("maxListings", MAX_LISTINGS);
        return result;
    }

    /**
     * 挂牌出售装备
     */
    public Map<String, Object> listEquipment(String userId, String equipmentId, long price) {
        validateListingPrice(price);
        checkListingQuota(userId);

        Equipment equip = equipmentRepository.findById(equipmentId);
        if (equip == null) throw new BusinessException(400, "装备不存在");
        if (!userId.equals(equip.getUserId())) throw new BusinessException(400, "只能出售自己的装备");
        if (Boolean.TRUE.equals(equip.getBound())) throw new BusinessException(400, "绑定装备无法交易");
        if (Boolean.TRUE.equals(equip.getEquipped())) throw new BusinessException(400, "请先卸下装备再挂牌");

        long commission = price * COMMISSION_RATE;
        if (!resourceService.consumeSilver(userId, commission)) {
            throw new BusinessException(400, "白银不足，手续费需要" + commission + "银");
        }

        warehouseService.removeEquipment(userId, equipmentId);

        String snapshot = JSON.toJSONString(equip);
        String sellerName = resolveSellerName(userId);
        int qualityId = equip.getQuality() != null && equip.getQuality().getId() != null ? equip.getQuality().getId() : 1;

        marketMapper.insertListing(userId, sellerName, "equipment", equipmentId,
                equip.getName(), equip.getIcon(),
                equip.getLevel() != null ? equip.getLevel() : 0,
                qualityId, 1, price, commission, snapshot,
                System.currentTimeMillis());

        logger.info("用户 {} 挂牌装备 [{}] 售价 {} 黄金, 手续费 {} 白银", userId, equip.getName(), price, commission);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("commission", commission);
        result.put("price", price);
        result.put("itemName", equip.getName());
        result.put("activeCount", marketMapper.countActiveBySeller(userId));
        result.put("maxListings", MAX_LISTINGS);
        return result;
    }

    /**
     * 挂牌出售道具
     */
    public Map<String, Object> listItem(String userId, String itemId, int count, long price) {
        validateListingPrice(price);
        if (count <= 0) throw new BusinessException(400, "数量必须大于0");
        checkListingQuota(userId);

        Warehouse warehouse = warehouseService.getWarehouse(userId);
        Warehouse.WarehouseItem target = null;
        for (Warehouse.WarehouseItem wi : warehouse.getItemStorage().getItems()) {
            if (wi.getItemId().equals(itemId)) {
                target = wi;
                break;
            }
        }
        if (target == null) throw new BusinessException(400, "物品不存在");
        if (target.getCount() < count) throw new BusinessException(400, "物品数量不足");
        if (Boolean.TRUE.equals(target.getBound())) throw new BusinessException(400, "绑定道具无法交易");

        long commission = price * COMMISSION_RATE;
        if (!resourceService.consumeSilver(userId, commission)) {
            throw new BusinessException(400, "白银不足，手续费需要" + commission + "银");
        }

        warehouseService.removeItem(userId, itemId, count);

        String sellerName = resolveSellerName(userId);
        int qualityVal = resolveQuality(target.getQuality());

        Map<String, Object> snapshotMap = new HashMap<>();
        snapshotMap.put("itemId", target.getItemId());
        snapshotMap.put("name", target.getName());
        snapshotMap.put("icon", target.getIcon());
        snapshotMap.put("itemType", target.getItemType());
        snapshotMap.put("quality", target.getQuality());
        snapshotMap.put("count", count);

        marketMapper.insertListing(userId, sellerName, "item", itemId,
                target.getName(), target.getIcon(), 0, qualityVal, count,
                price, commission, JSON.toJSONString(snapshotMap),
                System.currentTimeMillis());

        logger.info("用户 {} 挂牌道具 [{}]x{} 售价 {} 黄金, 手续费 {} 白银",
                userId, target.getName(), count, price, commission);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("commission", commission);
        result.put("price", price);
        result.put("itemName", target.getName());
        result.put("activeCount", marketMapper.countActiveBySeller(userId));
        result.put("maxListings", MAX_LISTINGS);
        return result;
    }

    /**
     * 购买挂牌物品
     */
    public Map<String, Object> buy(String buyerId, long listingId) {
        Map<String, Object> listing = marketMapper.findById(listingId);
        if (listing == null) throw new BusinessException(400, "挂牌不存在");
        if (!"active".equals(listing.get("status"))) throw new BusinessException(400, "该物品已下架");

        String sellerId = (String) listing.get("sellerId");
        if (buyerId.equals(sellerId)) throw new BusinessException(400, "不能购买自己出售的物品");

        long price = getLong(listing, "price", 0);
        if (!resourceService.consumeGold(buyerId, price)) {
            throw new BusinessException(400, "黄金不足，需要" + price + "黄金");
        }

        resourceService.addGold(sellerId, price);

        String itemType = (String) listing.get("itemType");
        String itemName = (String) listing.get("itemName");
        String buyerName = resolveSellerName(buyerId);

        if ("equipment".equals(itemType)) {
            String snapshot = (String) listing.get("itemSnapshot");
            Equipment equip = JSON.parseObject(snapshot, Equipment.class);
            equip.setUserId(buyerId);
            equip.setEquipped(false);
            equip.setEquippedGeneralId(null);
            equip.setBound(false);
            equipmentRepository.save(equip);
            warehouseService.addEquipment(buyerId, equip.getId());
        } else {
            String snapshot = (String) listing.get("itemSnapshot");
            Map<String, Object> itemData = JSON.parseObject(snapshot, Map.class);
            Warehouse.WarehouseItem wItem = new Warehouse.WarehouseItem();
            wItem.setItemId((String) itemData.getOrDefault("itemId", listing.get("itemId")));
            wItem.setName(itemName);
            wItem.setIcon((String) listing.get("itemIcon"));
            wItem.setItemType((String) itemData.getOrDefault("itemType", "material"));
            wItem.setQuality(String.valueOf(itemData.getOrDefault("quality", "普通")));
            Object cnt = listing.get("itemCount");
            wItem.setCount(cnt instanceof Number ? ((Number) cnt).intValue() : 1);
            wItem.setMaxStack(99);
            wItem.setUsable(true);
            wItem.setBound(false);
            warehouseService.addItem(buyerId, wItem);
        }

        marketMapper.updateStatus(listingId, "sold", buyerId, buyerName, System.currentTimeMillis());
        marketMapper.insertTradeLog(listingId, sellerId, buyerId, itemType, itemName, price, System.currentTimeMillis());

        logger.info("用户 {} 购买了 [{}] 花费 {} 黄金, 卖家 {}", buyerId, itemName, price, sellerId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("itemName", itemName);
        result.put("price", price);
        return result;
    }

    /**
     * 撤销挂牌（退还佣金 + 物品）
     */
    public Map<String, Object> cancel(String userId, long listingId) {
        Map<String, Object> listing = marketMapper.findById(listingId);
        if (listing == null) throw new BusinessException(400, "挂牌不存在");
        if (!userId.equals(listing.get("sellerId"))) throw new BusinessException(400, "只能撤销自己的挂牌");
        if (!"active".equals(listing.get("status"))) throw new BusinessException(400, "该物品已不可撤销");

        long commission = getLong(listing, "commission", 0);
        resourceService.addSilver(userId, commission);

        restoreItemToOwner(userId, listing);
        marketMapper.updateStatus(listingId, "cancelled", null, null, System.currentTimeMillis());

        logger.info("用户 {} 撤销挂牌 {}, 退还手续费 {} 白银", userId, listingId, commission);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("commission", commission);
        result.put("itemName", listing.get("itemName"));
        return result;
    }

    /**
     * 交易记录
     */
    public List<Map<String, Object>> getTradeLogs(String userId) {
        return marketMapper.findTradeLogs(userId, 50);
    }

    /**
     * 每小时检查过期挂牌，自动下架并退还物品 + 佣金
     */
    @Scheduled(fixedRate = 3600000)
    public void autoDelistTask() {
        long expireTime = System.currentTimeMillis() - EXPIRE_DAYS * 24 * 3600 * 1000L;
        List<Map<String, Object>> expired = marketMapper.findExpiredListings(expireTime, 200);
        if (expired.isEmpty()) return;

        for (Map<String, Object> listing : expired) {
            try {
                String sellerId = (String) listing.get("sellerId");
                long commission = getLong(listing, "commission", 0);
                resourceService.addSilver(sellerId, commission);
                restoreItemToOwner(sellerId, listing);
            } catch (Exception e) {
                logger.error("自动下架退还物品失败, listingId={}", listing.get("id"), e);
            }
        }

        int count = marketMapper.autoDelistExpired(expireTime, System.currentTimeMillis());
        logger.info("自动下架过期挂牌 {} 条", count);
    }

    // ===== 内部方法 =====

    private void validateListingPrice(long price) {
        if (price <= 0) throw new BusinessException(400, "售价必须大于0");
        if (price > MAX_PRICE) throw new BusinessException(400, "总价超过上限" + MAX_PRICE / 10000 + "W!");
    }

    private void checkListingQuota(String userId) {
        int active = marketMapper.countActiveBySeller(userId);
        if (active >= MAX_LISTINGS) {
            throw new BusinessException(400, "挂单数量已满！最多可同时挂" + MAX_LISTINGS + "个出售单");
        }
    }

    private String resolveSellerName(String userId) {
        UserResource res = resourceService.getUserResource(userId);
        if (res != null && res.getLevel() != null) return "Lv." + res.getLevel() + "主公";
        return "主公";
    }

    private int resolveQuality(String qStr) {
        if (qStr == null) return 1;
        switch (qStr) {
            case "绿色": case "优秀": return 2;
            case "蓝色": case "精良": return 3;
            case "紫色": case "史诗": return 4;
            case "橙色": case "传说": return 5;
            case "红色": case "神话": return 6;
            default: return 1;
        }
    }

    private void restoreItemToOwner(String userId, Map<String, Object> listing) {
        String itemType = (String) listing.get("itemType");
        if ("equipment".equals(itemType)) {
            String snapshot = (String) listing.get("itemSnapshot");
            Equipment equip = JSON.parseObject(snapshot, Equipment.class);
            equip.setUserId(userId);
            equipmentRepository.save(equip);
            warehouseService.addEquipment(userId, equip.getId());
        } else {
            String snapshot = (String) listing.get("itemSnapshot");
            Map<String, Object> itemData = JSON.parseObject(snapshot, Map.class);
            Warehouse.WarehouseItem wItem = new Warehouse.WarehouseItem();
            wItem.setItemId((String) itemData.getOrDefault("itemId", listing.get("itemId")));
            wItem.setName((String) listing.get("itemName"));
            wItem.setIcon((String) listing.get("itemIcon"));
            wItem.setItemType((String) itemData.getOrDefault("itemType", "material"));
            wItem.setQuality(String.valueOf(itemData.getOrDefault("quality", "普通")));
            Object cnt = listing.get("itemCount");
            wItem.setCount(cnt instanceof Number ? ((Number) cnt).intValue() : 1);
            wItem.setMaxStack(99);
            wItem.setUsable(true);
            warehouseService.addItem(userId, wItem);
        }
    }

    private long getLong(Map<String, Object> m, String key, long def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return def;
    }
}
