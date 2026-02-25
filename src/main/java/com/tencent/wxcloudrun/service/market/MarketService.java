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
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketService {

    private static final Logger logger = LoggerFactory.getLogger(MarketService.class);
    private static final long COMMISSION_RATE = 1000;

    @Autowired
    private MarketMapper marketMapper;
    @Autowired
    private UserResourceService resourceService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private EquipmentRepository equipmentRepository;

    /**
     * 浏览市场（分页）
     */
    public Map<String, Object> browse(String itemType, int page) {
        int pageSize = 12;
        int offset = page * pageSize;
        List<Map<String, Object>> listings = marketMapper.findActive(itemType, offset, pageSize);
        int total = marketMapper.countActive(itemType);

        Map<String, Object> result = new HashMap<>();
        result.put("listings", listings);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize);
        return result;
    }

    /**
     * 我的挂牌
     */
    public List<Map<String, Object>> myListings(String userId) {
        return marketMapper.findBySeller(userId, 50);
    }

    /**
     * 挂牌出售装备
     */
    public Map<String, Object> listEquipment(String userId, String equipmentId, long price) {
        if (price <= 0) throw new BusinessException(400, "售价必须大于0");
        if (price > 1000000) throw new BusinessException(400, "售价不能超过100万黄金");

        Equipment equip = equipmentRepository.findById(equipmentId);
        if (equip == null) throw new BusinessException(400, "装备不存在");
        if (!userId.equals(equip.getUserId())) throw new BusinessException(400, "只能出售自己的装备");
        if (Boolean.TRUE.equals(equip.getBound())) throw new BusinessException(400, "绑定装备无法交易");
        if (Boolean.TRUE.equals(equip.getEquipped())) throw new BusinessException(400, "请先卸下装备再挂牌");

        long commission = price * COMMISSION_RATE;
        if (!resourceService.consumeSilver(userId, commission)) {
            throw new BusinessException(400, "白银不足，佣金需要" + commission + "白银");
        }

        // 从仓库移除
        warehouseService.removeEquipment(userId, equipmentId);

        String snapshot = JSON.toJSONString(equip);
        String sellerName = "主公";
        UserResource res = resourceService.getUserResource(userId);
        if (res != null && res.getLevel() != null) sellerName = "Lv." + res.getLevel() + "主公";

        int qualityId = equip.getQuality() != null && equip.getQuality().getId() != null ? equip.getQuality().getId() : 1;

        marketMapper.insertListing(userId, sellerName, "equipment", equipmentId,
                equip.getName(), equip.getIcon(),
                equip.getLevel() != null ? equip.getLevel() : 0,
                qualityId, 1, price, commission, snapshot,
                System.currentTimeMillis());

        logger.info("用户 {} 挂牌装备 [{}] 售价 {} 黄金, 佣金 {} 白银", userId, equip.getName(), price, commission);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("commission", commission);
        result.put("price", price);
        result.put("itemName", equip.getName());
        return result;
    }

    /**
     * 挂牌出售道具
     */
    public Map<String, Object> listItem(String userId, String itemId, int count, long price) {
        if (price <= 0) throw new BusinessException(400, "售价必须大于0");
        if (count <= 0) throw new BusinessException(400, "数量必须大于0");

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
            throw new BusinessException(400, "白银不足，佣金需要" + commission + "白银");
        }

        warehouseService.removeItem(userId, itemId, count);

        String sellerName = "主公";
        UserResource res = resourceService.getUserResource(userId);
        if (res != null && res.getLevel() != null) sellerName = "Lv." + res.getLevel() + "主公";

        int qualityVal = 1;
        try {
            String qStr = target.getQuality();
            if (qStr != null) {
                switch (qStr) {
                    case "绿色": case "优秀": qualityVal = 2; break;
                    case "蓝色": case "精良": qualityVal = 3; break;
                    case "紫色": case "史诗": qualityVal = 4; break;
                    case "橙色": case "传说": qualityVal = 5; break;
                    case "红色": case "神话": qualityVal = 6; break;
                    default: qualityVal = 1;
                }
            }
        } catch (Exception ignored) {}

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("itemId", target.getItemId());
        snapshot.put("name", target.getName());
        snapshot.put("icon", target.getIcon());
        snapshot.put("itemType", target.getItemType());
        snapshot.put("quality", target.getQuality());
        snapshot.put("count", count);

        marketMapper.insertListing(userId, sellerName, "item", itemId,
                target.getName(), target.getIcon(), 0, qualityVal, count,
                price, commission, JSON.toJSONString(snapshot),
                System.currentTimeMillis());

        logger.info("用户 {} 挂牌道具 [{}]x{} 售价 {} 黄金, 佣金 {} 白银",
                userId, target.getName(), count, price, commission);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("commission", commission);
        result.put("price", price);
        result.put("itemName", target.getName());
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
        if (buyerId.equals(sellerId)) throw new BusinessException(400, "不能购买自己的物品");

        long price = getLong(listing, "price", 0);
        if (!resourceService.consumeGold(buyerId, price)) {
            throw new BusinessException(400, "黄金不足，需要" + price + "黄金");
        }

        // 黄金转给卖家
        resourceService.addGold(sellerId, price);

        String itemType = (String) listing.get("itemType");
        String itemName = (String) listing.get("itemName");
        String buyerName = "买家";
        UserResource buyerRes = resourceService.getUserResource(buyerId);
        if (buyerRes != null && buyerRes.getLevel() != null) buyerName = "Lv." + buyerRes.getLevel() + "主公";

        // 物品转给买家
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
     * 撤销挂牌（退还佣金+物品）
     */
    public Map<String, Object> cancel(String userId, long listingId) {
        Map<String, Object> listing = marketMapper.findById(listingId);
        if (listing == null) throw new BusinessException(400, "挂牌不存在");
        if (!userId.equals(listing.get("sellerId"))) throw new BusinessException(400, "只能撤销自己的挂牌");
        if (!"active".equals(listing.get("status"))) throw new BusinessException(400, "该物品已不可撤销");

        long commission = getLong(listing, "commission", 0);
        resourceService.addSilver(userId, commission);

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

        marketMapper.updateStatus(listingId, "cancelled", null, null, System.currentTimeMillis());

        logger.info("用户 {} 撤销挂牌 {}, 退还佣金 {} 白银", userId, listingId, commission);

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

    private long getLong(Map<String, Object> m, String key, long def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return def;
    }
}
