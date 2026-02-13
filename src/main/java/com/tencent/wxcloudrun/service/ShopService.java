package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.ItemMapper;
import com.tencent.wxcloudrun.dao.ShopMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Item;
import com.tencent.wxcloudrun.model.Shop;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商城服务
 * 购买商品后统一将道具放入仓库
 */
@Service
public class ShopService {
    
    private static final Logger logger = LoggerFactory.getLogger(ShopService.class);
    
    @Autowired
    private ShopMapper shopMapper;
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private UserResourceService userResourceService;
    
    @Autowired
    private WarehouseService warehouseService;
    
    /**
     * 获取商品列表
     * @param tab 商品分类（classify），为空或"all"时返回全部
     */
    public List<Shop> getShopGoods(String tab) {
        List<Shop> goods;
        if (tab == null || tab.isEmpty() || "all".equalsIgnoreCase(tab)) {
            goods = shopMapper.findAll();
        } else {
            goods = shopMapper.findByClassify(tab);
        }
        return goods;
    }
    
    /**
     * 购买商品（扣款 + 道具放入仓库，事务保证原子性）
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @param count 购买数量
     */
    @Transactional
    public Map<String, Object> buyGoods(String userId, Long goodsId, int count) {
        if (count <= 0) {
            throw new BusinessException(400, "购买数量必须大于0");
        }
        
        // 查询商品
        Shop goods = shopMapper.findById(goodsId);
        if (goods == null) {
            throw new BusinessException(400, "商品不存在");
        }
        
        // 校验商品关联的道具
        if (goods.getItemId() == null || goods.getItemId() == 0) {
            throw new BusinessException(400, "商品未关联道具，无法购买");
        }
        
        Item item = itemMapper.findById(goods.getItemId().intValue());
        if (item == null) {
            throw new BusinessException(400, "商品关联的道具不存在，item_id=" + goods.getItemId());
        }
        
        // 先检查仓库容量（避免扣了钱但道具放不进去）
        checkWarehouseCapacity(userId);
        
        // 计算总价
        long totalPrice = (long) goods.getPrice() * count;
        
        // 根据货币类型扣费
        boolean success;
        String currencyType = goods.getCurrency();
        switch (currencyType) {
            case "gold":
                success = userResourceService.consumeGold(userId, totalPrice);
                break;
            case "silver":
                success = userResourceService.consumeSilver(userId, totalPrice);
                break;
            case "diamond":
                success = userResourceService.consumeDiamond(userId, totalPrice);
                break;
            default:
                throw new BusinessException(400, "不支持的货币类型: " + currencyType);
        }
        
        if (!success) {
            String currencyName = getCurrencyName(currencyType);
            throw new BusinessException(400, currencyName + "不足，需要" + totalPrice);
        }
        
        // 道具放入仓库
        String deliveredItems = deliverToWarehouse(userId, goods, item, count);
        
        logger.info("用户 {} 购买商品 [{}] x{}, 花费 {} {}, 道具 [{}](item_id={}) 已放入仓库",
                userId, goods.getName(), count, totalPrice, currencyType, item.getItemName(), item.getItemId());
        
        // 获取购买后的用户资源
        UserResource resource = userResourceService.getUserResource(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("goods", goods);
        result.put("count", count);
        result.put("totalPrice", totalPrice);
        result.put("currency", currencyType);
        result.put("remainingGold", resource.getGold());
        result.put("remainingSilver", resource.getSilver());
        result.put("remainingDiamond", resource.getDiamond());
        result.put("deliveredItems", deliveredItems);
        return result;
    }
    
    /**
     * 检查仓库物品栏是否有空间
     */
    private void checkWarehouseCapacity(String userId) {
        try {
            Warehouse warehouse = warehouseService.getWarehouse(userId);
            Warehouse.ItemStorage storage = warehouse.getItemStorage();
            if (storage.getUsedSlots() >= storage.getCapacity()) {
                throw new BusinessException(400, "仓库物品栏已满，请先扩充或清理后再购买");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("检查仓库容量异常，跳过检查: {}", e.getMessage());
        }
    }
    
    /**
     * 将购买的道具放入仓库
     * @param userId 用户ID
     * @param goods 商品信息
     * @param item 道具信息（来自 item 表）
     * @param count 购买数量
     * @return 发放描述（用于前端展示）
     */
    private String deliverToWarehouse(String userId, Shop goods, Item item, int count) {
        // 用 item_id 作为仓库物品的唯一标识，保证同一道具可以堆叠
        String warehouseItemId = String.valueOf(item.getItemId());
        
        // 根据商品分类映射到仓库物品类型
        String itemType = mapClassifyToItemType(goods.getClassify());
        
        Warehouse.WarehouseItem warehouseItem = Warehouse.WarehouseItem.builder()
                .itemId(warehouseItemId)
                .itemType(itemType)
                .name(item.getItemName())
                .icon(goods.getIcon())
                .quality(String.valueOf(item.getQuality() != null ? item.getQuality() : 1))
                .count(count)
                .maxStack(9999)
                .description(goods.getDesc())
                .usable(true)
                .build();
        
        warehouseService.addItem(userId, warehouseItem);
        
        return item.getItemName() + " x" + count;
    }
    
    /**
     * 将商品 classify 映射为仓库物品类型
     */
    private String mapClassifyToItemType(String classify) {
        if (classify == null) return "other";
        switch (classify) {
            case "enhance":
                return "material";
            case "recruit":
                return "token";
            case "resource":
                return "resource";
            case "consumable":
                return "consumable";
            case "special":
                return "special";
            default:
                return "other";
        }
    }
    
    /**
     * 获取货币中文名称
     */
    private String getCurrencyName(String currency) {
        switch (currency) {
            case "gold": return "黄金";
            case "silver": return "白银";
            case "diamond": return "钻石";
            default: return currency;
        }
    }
}
