package com.tencent.wxcloudrun.service.warehouse;

import com.tencent.wxcloudrun.dao.PeerageConfigMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import com.tencent.wxcloudrun.repository.WarehouseRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 仓库服务
 */
@Service
public class WarehouseService {
    
    private static final Logger logger = LoggerFactory.getLogger(WarehouseService.class);
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private UserResourceRepository resourceRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private UserResourceService userResourceService;
    
    @Autowired
    private PeerageConfigMapper peerageConfigMapper;
    
    /**
     * 获取用户仓库
     */
    public Warehouse getWarehouse(String userId) {
        Warehouse warehouse = warehouseRepository.findByUserId(userId);
        if (warehouse == null) {
            warehouse = warehouseRepository.initWarehouse(userId);
        }
        return warehouse;
    }
    
    /**
     * 获取仓库概览信息
     */
    public Map<String, Object> getWarehouseInfo(String userId) {
        Warehouse warehouse = getWarehouse(userId);
        
        Map<String, Object> info = new HashMap<>();
        
        // 装备仓库信息
        Map<String, Object> equipmentInfo = new HashMap<>();
        equipmentInfo.put("capacity", warehouse.getEquipmentStorage().getCapacity());
        equipmentInfo.put("used", warehouse.getEquipmentStorage().getUsedSlots());
        equipmentInfo.put("expandTimes", warehouse.getEquipmentStorage().getExpandTimes());
        equipmentInfo.put("maxExpandTimes", warehouseRepository.getMaxExpandTimes());
        equipmentInfo.put("nextExpandCost", warehouseRepository.getExpandCost(warehouse.getEquipmentStorage().getExpandTimes()));
        info.put("equipment", equipmentInfo);
        
        // 物品仓库信息
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("capacity", warehouse.getItemStorage().getCapacity());
        itemInfo.put("used", warehouse.getItemStorage().getUsedSlots());
        itemInfo.put("expandTimes", warehouse.getItemStorage().getExpandTimes());
        itemInfo.put("maxExpandTimes", warehouseRepository.getMaxExpandTimes());
        itemInfo.put("nextExpandCost", warehouseRepository.getExpandCost(warehouse.getItemStorage().getExpandTimes()));
        info.put("item", itemInfo);
        
        return info;
    }
    
    /**
     * 扩充装备仓库
     */
    public Map<String, Object> expandEquipmentStorage(String userId) {
        Warehouse warehouse = getWarehouse(userId);
        UserResource resource = resourceRepository.findByUserId(userId);
        
        int expandTimes = warehouse.getEquipmentStorage().getExpandTimes();
        int cost = warehouseRepository.getExpandCost(expandTimes);
        
        if (cost == -1) {
            throw new BusinessException(400, "装备仓库已达到最大容量，无法继续扩充");
        }
        
        if (resource.getGold() < cost) {
            throw new BusinessException(400, "元宝不足，需要" + cost + "元宝");
        }
        
        // 扣除元宝
        resource.setGold(resource.getGold() - cost);
        resourceRepository.save(resource);
        
        // 扩充仓库
        Warehouse.EquipmentStorage storage = warehouse.getEquipmentStorage();
        storage.setCapacity(storage.getCapacity() + warehouseRepository.getExpandAmount());
        storage.setExpandTimes(expandTimes + 1);
        warehouseRepository.save(warehouse);
        
        logger.info("用户 {} 扩充装备仓库，花费 {} 元宝，当前容量 {}", userId, cost, storage.getCapacity());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("cost", cost);
        result.put("newCapacity", storage.getCapacity());
        result.put("expandTimes", storage.getExpandTimes());
        result.put("nextExpandCost", warehouseRepository.getExpandCost(storage.getExpandTimes()));
        result.put("remainingGold", resource.getGold());
        
        return result;
    }
    
    /**
     * 扩充物品仓库
     */
    public Map<String, Object> expandItemStorage(String userId) {
        Warehouse warehouse = getWarehouse(userId);
        UserResource resource = resourceRepository.findByUserId(userId);
        
        int expandTimes = warehouse.getItemStorage().getExpandTimes();
        int cost = warehouseRepository.getExpandCost(expandTimes);
        
        if (cost == -1) {
            throw new BusinessException(400, "物品仓库已达到最大容量，无法继续扩充");
        }
        
        if (resource.getGold() < cost) {
            throw new BusinessException(400, "元宝不足，需要" + cost + "元宝");
        }
        
        // 扣除元宝
        resource.setGold(resource.getGold() - cost);
        resourceRepository.save(resource);
        
        // 扩充仓库
        Warehouse.ItemStorage storage = warehouse.getItemStorage();
        storage.setCapacity(storage.getCapacity() + warehouseRepository.getExpandAmount());
        storage.setExpandTimes(expandTimes + 1);
        warehouseRepository.save(warehouse);
        
        logger.info("用户 {} 扩充物品仓库，花费 {} 元宝，当前容量 {}", userId, cost, storage.getCapacity());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("cost", cost);
        result.put("newCapacity", storage.getCapacity());
        result.put("expandTimes", storage.getExpandTimes());
        result.put("nextExpandCost", warehouseRepository.getExpandCost(storage.getExpandTimes()));
        result.put("remainingGold", resource.getGold());
        
        return result;
    }
    
    /**
     * 获取装备列表
     */
    public Map<String, Object> getEquipments(String userId, int page, int pageSize) {
        Warehouse warehouse = getWarehouse(userId);
        List<String> equipmentIds = warehouse.getEquipmentStorage().getEquipmentIds();
        
        // 获取装备详情
        List<Equipment> allEquipments = new ArrayList<>();
        for (String id : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(id);
            if (equipment != null) {
                allEquipments.add(equipment);
            }
        }
        
        // 分页
        int total = allEquipments.size();
        int start = page * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Equipment> pageEquipments = start < total ? allEquipments.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("equipments", pageEquipments);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("capacity", warehouse.getEquipmentStorage().getCapacity());
        result.put("used", warehouse.getEquipmentStorage().getUsedSlots());
        
        return result;
    }
    
    /**
     * 获取物品列表
     */
    public Map<String, Object> getItems(String userId, int page, int pageSize, String itemType) {
        Warehouse warehouse = getWarehouse(userId);
        List<Warehouse.WarehouseItem> allItems = warehouse.getItemStorage().getItems();
        
        // 按类型筛选
        if (itemType != null && !itemType.isEmpty() && !"all".equals(itemType)) {
            allItems = new ArrayList<>();
            for (Warehouse.WarehouseItem item : warehouse.getItemStorage().getItems()) {
                if (itemType.equals(item.getItemType())) {
                    allItems.add(item);
                }
            }
        }
        
        // 分页
        int total = allItems.size();
        int start = page * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Warehouse.WarehouseItem> pageItems = start < total ? allItems.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("items", pageItems);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("capacity", warehouse.getItemStorage().getCapacity());
        result.put("used", warehouse.getItemStorage().getUsedSlots());
        
        return result;
    }
    
    /**
     * 添加装备到仓库
     */
    public boolean addEquipment(String userId, String equipmentId) {
        Warehouse warehouse = getWarehouse(userId);
        Warehouse.EquipmentStorage storage = warehouse.getEquipmentStorage();
        
        if (storage.getUsedSlots() >= storage.getCapacity()) {
            throw new BusinessException(400, "装备仓库已满，请先扩充或清理");
        }
        
        storage.getEquipmentIds().add(equipmentId);
        storage.setUsedSlots(storage.getUsedSlots() + 1);
        warehouseRepository.save(warehouse);
        
        return true;
    }
    
    /**
     * 从仓库移除装备
     */
    public boolean removeEquipment(String userId, String equipmentId) {
        Warehouse warehouse = getWarehouse(userId);
        Warehouse.EquipmentStorage storage = warehouse.getEquipmentStorage();
        
        if (storage.getEquipmentIds().remove(equipmentId)) {
            storage.setUsedSlots(Math.max(0, storage.getUsedSlots() - 1));
            warehouseRepository.save(warehouse);
            return true;
        }
        return false;
    }
    
    /**
     * 添加物品到仓库
     */
    public boolean addItem(String userId, Warehouse.WarehouseItem item) {
        Warehouse warehouse = getWarehouse(userId);
        Warehouse.ItemStorage storage = warehouse.getItemStorage();
        
        // 检查是否可以堆叠
        for (Warehouse.WarehouseItem existingItem : storage.getItems()) {
            if (existingItem.getItemId().equals(item.getItemId())) {
                int newCount = existingItem.getCount() + item.getCount();
                if (newCount <= existingItem.getMaxStack()) {
                    existingItem.setCount(newCount);
                    warehouseRepository.save(warehouse);
                    return true;
                }
            }
        }
        
        // 需要新槽位
        if (storage.getUsedSlots() >= storage.getCapacity()) {
            throw new BusinessException(400, "物品仓库已满，请先扩充或清理");
        }
        
        storage.getItems().add(item);
        storage.setUsedSlots(storage.getUsedSlots() + 1);
        warehouseRepository.save(warehouse);
        
        return true;
    }
    
    /**
     * 从仓库移除物品
     */
    public boolean removeItem(String userId, String itemId, int count) {
        Warehouse warehouse = getWarehouse(userId);
        Warehouse.ItemStorage storage = warehouse.getItemStorage();
        
        for (int i = 0; i < storage.getItems().size(); i++) {
            Warehouse.WarehouseItem item = storage.getItems().get(i);
            if (item.getItemId().equals(itemId)) {
                if (item.getCount() > count) {
                    item.setCount(item.getCount() - count);
                } else {
                    storage.getItems().remove(i);
                    storage.setUsedSlots(Math.max(0, storage.getUsedSlots() - 1));
                }
                warehouseRepository.save(warehouse);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 使用物品
     */
    public Map<String, Object> useItem(String userId, String itemId, int count) {
        Warehouse warehouse = getWarehouse(userId);
        Warehouse.ItemStorage storage = warehouse.getItemStorage();
        
        Warehouse.WarehouseItem targetItem = null;
        for (Warehouse.WarehouseItem item : storage.getItems()) {
            if (item.getItemId().equals(itemId)) {
                targetItem = item;
                break;
            }
        }
        
        if (targetItem == null) {
            throw new BusinessException(400, "物品不存在");
        }
        
        if (targetItem.getCount() < count) {
            throw new BusinessException(400, "物品数量不足");
        }
        
        // 根据物品类型执行效果
        Map<String, Object> effect = applyItemEffect(userId, targetItem, count);
        
        // 扣除物品
        removeItem(userId, itemId, count);
        
        return effect;
    }
    
    /**
     * 应用物品效果
     */
    private Map<String, Object> applyItemEffect(String userId, Warehouse.WarehouseItem item, int count) {
        Map<String, Object> effect = new HashMap<>();
        UserResource resource = resourceRepository.findByUserId(userId);
        
        String itemType = item.getItemType();
        switch (itemType) {
            case "stamina": // 体力丹
                int staminaGain = 20 * count; // 每个回复20体力
                // 这里可以调用体力服务增加体力
                effect.put("type", "stamina");
                effect.put("gain", staminaGain);
                effect.put("message", "恢复" + staminaGain + "点体力");
                break;
                
            case "exp": // 经验丹
                int expGain = 100 * count;
                // 这里可以调用经验服务增加经验
                effect.put("type", "exp");
                effect.put("gain", expGain);
                effect.put("message", "获得" + expGain + "点经验");
                break;
                
            case "resource_wood": // 木材包
                int woodGain = 1000 * count;
                resource.setWood(resource.getWood() + woodGain);
                resourceRepository.save(resource);
                effect.put("type", "wood");
                effect.put("gain", woodGain);
                effect.put("message", "获得" + woodGain + "木材");
                break;
                
            case "resource_metal": // 金属包
                int metalGain = 1000 * count;
                resource.setMetal(resource.getMetal() + metalGain);
                resourceRepository.save(resource);
                effect.put("type", "metal");
                effect.put("gain", metalGain);
                effect.put("message", "获得" + metalGain + "金属");
                break;
                
            case "resource_food": // 粮食包
                int foodGain = 1000 * count;
                resource.setFood(resource.getFood() + foodGain);
                resourceRepository.save(resource);
                effect.put("type", "food");
                effect.put("gain", foodGain);
                effect.put("message", "获得" + foodGain + "粮食");
                break;
                
            case "silver": // 银两包
                int silverGain = 10000 * count;
                resource.setSilver(resource.getSilver() + silverGain);
                resourceRepository.save(resource);
                effect.put("type", "silver");
                effect.put("gain", silverGain);
                effect.put("message", "获得" + silverGain + "银两");
                break;
                
            case "fame_token": // 声望符
                int itemIdNum = 0;
                try { itemIdNum = Integer.parseInt(item.getItemId()); } catch (Exception ignored) {}
                Map<String, Object> fameConfig = peerageConfigMapper.findFameTokenConfig(itemIdNum);
                long fameGain = 500;
                if (fameConfig != null) {
                    Object fa = fameConfig.get("fameAmount");
                    if (fa instanceof Number) fameGain = ((Number) fa).longValue();
                }
                long totalFame = fameGain * count;
                userResourceService.addFame(userId, totalFame);
                effect.put("type", "fame");
                effect.put("gain", totalFame);
                effect.put("message", "获得" + totalFame + "声望");
                break;
                
            default:
                effect.put("type", "unknown");
                effect.put("message", "使用了" + item.getName() + "x" + count);
        }
        
        return effect;
    }
    
    /**
     * 出售装备
     */
    public Map<String, Object> sellEquipment(String userId, String equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(400, "装备不存在");
        }
        
        // 计算出售价格（根据品质）
        int sellPrice = calculateEquipmentSellPrice(equipment);
        
        // 获取用户资源
        UserResource resource = resourceRepository.findByUserId(userId);
        resource.setSilver(resource.getSilver() + sellPrice);
        resourceRepository.save(resource);
        
        // 从仓库移除
        removeEquipment(userId, equipmentId);
        
        // 删除装备
        equipmentRepository.delete(equipmentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sellPrice", sellPrice);
        result.put("totalSilver", resource.getSilver());
        
        return result;
    }
    
    /**
     * 计算装备出售价格
     */
    private int calculateEquipmentSellPrice(Equipment equipment) {
        int basePrice = 100;
        Equipment.Quality quality = equipment.getQuality();
        if (quality == null) return basePrice;
        
        int qualityId = quality.getId() != null ? quality.getId() : 1;
        switch (qualityId) {
            case 6: return basePrice * 100; // 红色(传说)
            case 5: return basePrice * 80;  // 橙色
            case 4: return basePrice * 50;  // 紫色
            case 3: return basePrice * 20;  // 蓝色
            case 2: return basePrice * 5;   // 绿色
            default: return basePrice;      // 白色
        }
    }
    
    /**
     * 批量出售装备
     */
    public Map<String, Object> batchSellEquipments(String userId, List<String> equipmentIds) {
        int totalPrice = 0;
        int soldCount = 0;
        
        for (String equipmentId : equipmentIds) {
            try {
                Map<String, Object> result = sellEquipment(userId, equipmentId);
                totalPrice += (int) result.get("sellPrice");
                soldCount++;
            } catch (Exception e) {
                logger.warn("出售装备失败: {}", equipmentId, e);
            }
        }
        
        UserResource resource = resourceRepository.findByUserId(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("soldCount", soldCount);
        result.put("totalPrice", totalPrice);
        result.put("totalSilver", resource.getSilver());
        
        return result;
    }
}

