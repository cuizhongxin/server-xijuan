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
    
    @Autowired
    private com.tencent.wxcloudrun.service.level.LevelService levelService;

    @Autowired
    private com.tencent.wxcloudrun.service.equipment.EquipmentService equipmentService;
    
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

        List<Equipment> allEquipments = equipmentRepository.findUnequippedByUserId(userId);
        if (allEquipments == null) allEquipments = new ArrayList<>();

        Warehouse.EquipmentStorage es = warehouse.getEquipmentStorage();
        es.setUsedSlots(allEquipments.size());
        if (es.getEquipmentIds() == null) es.setEquipmentIds(new ArrayList<>());
        List<String> ids = new ArrayList<>();
        for (Equipment e : allEquipments) ids.add(e.getId());
        es.setEquipmentIds(ids);
        warehouseRepository.save(warehouse);

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
        result.put("capacity", es.getCapacity());
        result.put("used", allEquipments.size());
        
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
        
        if (storage.getEquipmentIds() == null) {
            storage.setEquipmentIds(new java.util.ArrayList<>());
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
        
        if (storage.getEquipmentIds() != null && storage.getEquipmentIds().remove(equipmentId)) {
            storage.setUsedSlots(Math.max(0, storage.getUsedSlots() - 1));
            warehouseRepository.save(warehouse);
            return true;
        }
        return false;
    }
    
    /**
     * 查询仓库中某物品的数量
     */
    public int getItemCount(String userId, String itemId) {
        Warehouse warehouse = getWarehouse(userId);
        for (Warehouse.WarehouseItem item : warehouse.getItemStorage().getItems()) {
            if (item.getItemId().equals(itemId)) {
                return item.getCount();
            }
        }
        return 0;
    }

    /**
     * 从仓库扣除指定数量的物品（不触发效果）
     */
    public boolean consumeItem(String userId, String itemId, int count) {
        Warehouse warehouse = getWarehouse(userId);
        Warehouse.ItemStorage storage = warehouse.getItemStorage();
        for (int i = 0; i < storage.getItems().size(); i++) {
            Warehouse.WarehouseItem item = storage.getItems().get(i);
            if (item.getItemId().equals(itemId)) {
                if (item.getCount() < count) {
                    return false;
                }
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
     * 添加物品到仓库
     */
    public boolean addItem(String userId, Warehouse.WarehouseItem item) {
        Warehouse warehouse = getWarehouse(userId);
        Warehouse.ItemStorage storage = warehouse.getItemStorage();

        if (storage.getItems() == null) {
            storage.setItems(new java.util.ArrayList<>());
        }

        // 过滤掉MyBatis LEFT JOIN可能产生的空项
        storage.getItems().removeIf(i -> i == null || i.getItemId() == null);
        
        // 检查是否可以堆叠（绑定/非绑定分开堆叠）
        for (Warehouse.WarehouseItem existingItem : storage.getItems()) {
            if (existingItem.getItemId().equals(item.getItemId())
                    && Objects.equals(existingItem.getBound(), item.getBound())) {
                int maxStack = existingItem.getMaxStack() != null ? existingItem.getMaxStack() : 9999;
                int newCount = existingItem.getCount() + item.getCount();
                if (newCount <= maxStack) {
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
        logger.info("【物品使用】userId={}, itemId={}, count={}", userId, itemId, count);

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
            logger.warn("【物品使用】物品不存在 userId={}, itemId={}", userId, itemId);
            throw new BusinessException(400, "物品不存在");
        }
        
        if (targetItem.getCount() < count) {
            logger.warn("【物品使用】数量不足 userId={}, itemId={}, 拥有={}, 需要={}", userId, itemId, targetItem.getCount(), count);
            throw new BusinessException(400, "物品数量不足");
        }
        
        // 根据物品类型执行效果
        Map<String, Object> effect = applyItemEffect(userId, targetItem, count);
        logger.info("【物品使用】效果={}", effect);
        
        // 扣除物品（指定套装选择券等需要前端二次确认的不立即扣除）
        if (!Boolean.TRUE.equals(effect.remove("_skipRemoval"))) {
            removeItem(userId, itemId, count);
            logger.info("【物品使用】完成 userId={}, itemId={}, 扣除数量={}", userId, itemId, count);
        } else {
            logger.info("【物品使用】跳过扣除(需二次选择) userId={}, itemId={}", userId, itemId);
        }
        
        return effect;
    }
    
    /**
     * 应用物品效果 — 按 itemId 精确分发
     */
    private Map<String, Object> applyItemEffect(String userId, Warehouse.WarehouseItem item, int count) {
        Map<String, Object> effect = new HashMap<>();
        UserResource resource = userResourceService.getUserResource(userId);
        logger.info("【applyItemEffect】userId={}, itemId={}, name={}, count={}, 当前silver={}, food={}, metal={}, paper={}",
                userId, item.getItemId(), item.getName(), count,
                resource.getSilver(), resource.getFood(), resource.getMetal(), resource.getPaper());

        int id = 0;
        try { id = Integer.parseInt(item.getItemId()); } catch (Exception ignored) {}

        switch (id) {
            // ═══════ 声望符 ═══════
            case 11001: addFameEffect(userId, effect, 100L * count); break;
            case 11002: addFameEffect(userId, effect, 500L * count); break;

            // ═══════ 白银 ═══════
            case 11011: addSilverEffect(resource, effect, 2000L * count); break;
            case 11012: addSilverEffect(resource, effect, 10000L * count); break;
            case 11013: addSilverEffect(resource, effect, 50000L * count); break;

            // ═══════ 黄金 ═══════
            case 11021: addGoldEffect(resource, effect, 10L * count); break;
            case 11022: addGoldEffect(resource, effect, 20L * count); break;

            // ═══════ 绑金 ═══════
            case 11025: addBoundGoldEffect(userId, effect, 10L * count); break;
            case 11027: addBoundGoldEffect(userId, effect, 20L * count); break;
            case 11023: addBoundGoldEffect(userId, effect, 30L * count); break;
            case 11024: addBoundGoldEffect(userId, effect, 50L * count); break;
            case 11028: addBoundGoldEffect(userId, effect, 70L * count); break;
            case 11029: addBoundGoldEffect(userId, effect, 100L * count); break;

            // ═══════ 将魂 ═══════
            case 11026:
                int soul = 50 * count;
                resource.setSoulPoint((resource.getSoulPoint() != null ? resource.getSoulPoint() : 0) + soul);
                resourceRepository.save(resource);
                effect.put("type", "soul"); effect.put("gain", soul);
                effect.put("message", "获得" + soul + "将魂");
                break;

            // ═══════ 君主经验符 ═══════
            case 11042: addLordExpEffect(userId, effect, 5000L * count); break;
            case 11043: addLordExpEffect(userId, effect, 20000L * count); break;
            case 11044: addLordExpEffect(userId, effect, 100000L * count); break;
            case 11045: addLordExpEffect(userId, effect, 1000000L * count); break;

            // ═══════ 资源包 ═══════
            case 11052: case 11053: case 11054: addMaterialEffect(resource, effect, id, 2000L * count); break;
            case 11096: addMaterialEffect(resource, effect, 11052, 20000L * count); break;
            case 11097: addMaterialEffect(resource, effect, 11053, 50000L * count); break;
            case 11098: addMaterialEffect(resource, effect, 11054, 50000L * count); break;
            case 11051: openResourceGiftPack(resource, effect, count); break;
            case 11061: openAllianceWarChest(userId, effect, count); break;
            case 11062: openAllianceWarGiftBox(userId, effect, count); break;

            // ═══════ 粮食包系列 ═══════
            case 15031: addMaterialEffect(resource, effect, 11053, 1000L * count); break;
            case 15032: addMaterialEffect(resource, effect, 11053, 2000L * count); break;
            case 15033: addMaterialEffect(resource, effect, 11053, 3000L * count); break;
            case 15034: addMaterialEffect(resource, effect, 11053, 5000L * count); break;

            // ═══════ 精力丹 ═══════
            case 11101:
                int stGain = 5 * count;
                userResourceService.addStamina(userId, stGain);
                effect.put("type", "stamina"); effect.put("gain", stGain);
                effect.put("message", "恢复" + stGain + "点精力");
                break;

            // ═══════ 免战牌 / 恢复符 — 状态效果 ═══════
            case 11102:
                effect.put("type", "shield"); effect.put("hours", 2 * count);
                effect.put("message", "获得" + (2 * count) + "小时免战保护");
                break;
            case 11103:
                effect.put("type", "recovery"); effect.put("times", 20 * count);
                effect.put("message", "接下来" + (20 * count) + "次战斗自动补兵");
                break;

            // ═══════ 宝箱（随机获得对应套装装备） ═══════
            case 11091: case 11092: case 11093: case 11094:
                openChestEffect(userId, item, id, count, effect);
                break;

            // ═══════ 指定套装选择券（需前端弹出部位选择） ═══════
            case 16001: case 16002: case 16003: {
                String selectSet = id == 16001 ? "鹰扬" : (id == 16002 ? "虎啸" : "天狼");
                effect.put("type", "selectRequired");
                effect.put("setName", selectSet);
                effect.put("parts", Arrays.asList(
                        selectSet + "武器", selectSet + "戒指", selectSet + "铠甲",
                        selectSet + "项链", selectSet + "头盔", selectSet + "鞋子"));
                effect.put("message", "请选择" + selectSet + "套装部位");
                effect.put("_skipRemoval", true);
                break;
            }

            default:
                if (!applyEffectByName(userId, resource, item, count, effect)) {
                    effect.put("type", "generic");
                    effect.put("message", "使用了" + item.getName() + " x" + count);
                }
                break;
        }

        return effect;
    }

    private void addFameEffect(String userId, Map<String, Object> e, long amount) {
        userResourceService.addFame(userId, amount);
        e.put("type", "fame"); e.put("gain", amount);
        e.put("message", "获得" + amount + "声望");
    }
    private void addSilverEffect(UserResource r, Map<String, Object> e, long amount) {
        long before = r.getSilver() != null ? r.getSilver() : 0L;
        r.setSilver(before + amount);
        resourceRepository.save(r);
        logger.info("【addSilverEffect】before={}, add={}, after={}", before, amount, r.getSilver());
        e.put("type", "silver"); e.put("gain", amount);
        e.put("message", "获得" + amount + "白银");
    }
    private void addGoldEffect(UserResource r, Map<String, Object> e, long amount) {
        r.setGold((r.getGold() != null ? r.getGold() : 0L) + amount);
        resourceRepository.save(r);
        e.put("type", "gold"); e.put("gain", amount);
        e.put("message", "获得" + amount + "黄金");
    }
    private void addBoundGoldEffect(String userId, Map<String, Object> e, long amount) {
        userResourceService.addBoundGold(userId, amount);
        e.put("type", "boundGold"); e.put("gain", amount);
        e.put("message", "获得" + amount + "绑金");
    }
    private void addLordExpEffect(String userId, Map<String, Object> e, long amount) {
        Map<String, Object> levelResult = levelService.addExp(userId, amount, "使用经验符");
        e.put("type", "lordExp"); e.put("gain", amount);
        e.put("message", "获得" + amount + "君主经验");
        if (levelResult != null && Boolean.TRUE.equals(levelResult.get("levelUp"))) {
            e.put("levelUp", true);
            e.put("newLevel", levelResult.get("newLevel"));
        }
    }
    private void addMaterialEffect(UserResource r, Map<String, Object> e, int matId, long amount) {
        String matName;
        switch (matId) {
            case 11052:
                r.setMetal((r.getMetal() != null ? r.getMetal() : 0L) + amount);
                matName = "金属"; break;
            case 11053:
                r.setFood((r.getFood() != null ? r.getFood() : 0L) + amount);
                matName = "粮食"; break;
            case 11054:
                r.setPaper((r.getPaper() != null ? r.getPaper() : 0L) + amount);
                matName = "纸张"; break;
            default: matName = "资源"; break;
        }
        resourceRepository.save(r);
        logger.info("【addMaterialEffect】matId={}, matName={}, add={}", matId, matName, amount);
        e.put("type", "material"); e.put("gain", amount);
        e.put("message", "获得" + amount + matName);
    }
    private void addRandomResourceEffect(UserResource r, Map<String, Object> e, long amount) {
        int[] matIds = {11052, 11053, 11054};
        int pick = matIds[new java.util.Random().nextInt(matIds.length)];
        addMaterialEffect(r, e, pick, amount);
    }

    private void openResourceGiftPack(UserResource resource, Map<String, Object> effect, int count) {
        // 11051 资源礼包：随机 2000 金属/纸张/粮食/白银
        Random rng = new Random();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int roll = rng.nextInt(4);
            if (roll == 0) {
                addMaterialEffect(resource, new HashMap<>(), 11052, 2000L);
                result.put("金属+2000", result.getOrDefault("金属+2000", 0) + 1);
            } else if (roll == 1) {
                addMaterialEffect(resource, new HashMap<>(), 11054, 2000L);
                result.put("纸张+2000", result.getOrDefault("纸张+2000", 0) + 1);
            } else if (roll == 2) {
                addMaterialEffect(resource, new HashMap<>(), 11053, 2000L);
                result.put("粮食+2000", result.getOrDefault("粮食+2000", 0) + 1);
            } else {
                addSilverEffect(resource, new HashMap<>(), 2000L);
                result.put("白银+2000", result.getOrDefault("白银+2000", 0) + 1);
            }
        }
        effect.put("type", "box");
        effect.put("openResults", result);
        effect.put("message", "开启资源礼包获得: " + formatOpenResults(result));
    }

    private void openAllianceWarChest(String userId, Map<String, Object> effect, int count) {
        // 11061 盟战宝箱
        // 5% 天地宝盒，15% 高级声望符，20% 4级强化石，10% 招财符，30% 中级招贤令，20% 4阶品质石
        Random rng = new Random();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int roll = rng.nextInt(100);
            if (roll < 5) {
                addItemById(userId, "11064", "天地宝盒", 1);
                result.put("天地宝盒", result.getOrDefault("天地宝盒", 0) + 1);
            } else if (roll < 20) {
                addItemById(userId, "11002", "高级声望符", 1);
                result.put("高级声望符", result.getOrDefault("高级声望符", 0) + 1);
            } else if (roll < 40) {
                addItemById(userId, "14004", "4级强化石", 1);
                result.put("4级强化石", result.getOrDefault("4级强化石", 0) + 1);
            } else if (roll < 50) {
                addItemById(userId, "11104", "招财符", 1);
                result.put("招财符", result.getOrDefault("招财符", 0) + 1);
            } else if (roll < 80) {
                addItemById(userId, "15012", "中级招贤令", 1);
                result.put("中级招贤令", result.getOrDefault("中级招贤令", 0) + 1);
            } else {
                addItemById(userId, "14034", "4阶品质石", 1);
                result.put("4阶品质石", result.getOrDefault("4阶品质石", 0) + 1);
            }
        }
        effect.put("type", "box");
        effect.put("openResults", result);
        effect.put("message", "开启盟战宝箱获得: " + formatOpenResults(result));
    }

    private void openAllianceWarGiftBox(String userId, Map<String, Object> effect, int count) {
        // 11062 盟战礼盒：随机一个（等概率）
        // 资源礼包 / 10绑金 / 军需令 / 招财符 / 银锭
        Random rng = new Random();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int roll = rng.nextInt(5);
            if (roll == 0) {
                addItemById(userId, "11051", "资源礼包", 1);
                result.put("资源礼包", result.getOrDefault("资源礼包", 0) + 1);
            } else if (roll == 1) {
                addItemById(userId, "11025", "10绑金", 1);
                result.put("10绑金", result.getOrDefault("10绑金", 0) + 1);
            } else if (roll == 2) {
                addItemById(userId, "15052", "军需令", 1);
                result.put("军需令", result.getOrDefault("军需令", 0) + 1);
            } else if (roll == 3) {
                addItemById(userId, "11104", "招财符", 1);
                result.put("招财符", result.getOrDefault("招财符", 0) + 1);
            } else {
                addItemById(userId, "11012", "银锭", 1);
                result.put("银锭", result.getOrDefault("银锭", 0) + 1);
            }
        }
        effect.put("type", "box");
        effect.put("openResults", result);
        effect.put("message", "开启盟战礼盒获得: " + formatOpenResults(result));
    }

    private void addItemById(String userId, String itemId, String itemName, int count) {
        Warehouse.WarehouseItem reward = Warehouse.WarehouseItem.builder()
                .itemId(itemId)
                .itemType("item")
                .name(itemName)
                .quality("")
                .count(count)
                .maxStack(9999)
                .usable(true)
                .build();
        addItem(userId, reward);
    }

    private String formatOpenResults(Map<String, Integer> result) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : result.entrySet()) {
            parts.add(e.getKey() + "x" + e.getValue());
        }
        return String.join("、", parts);
    }

    private static final String[][] CHEST_PARTS = {
            {"鹰扬武器","鹰扬戒指","鹰扬铠甲","鹰扬项链","鹰扬头盔","鹰扬鞋子"},
            {"虎啸武器","虎啸戒指","虎啸铠甲","虎啸项链","虎啸头盔","虎啸鞋子"},
            {"宣武武器","宣武戒指","宣武铠甲","宣武项链","宣武头盔","宣武鞋子"},
            {"天狼武器","天狼戒指","天狼铠甲","天狼项链","天狼头盔","天狼鞋子"}
    };
    private static final String[] CHEST_SET_NAMES = {"鹰扬", "虎啸", "宣武", "天狼"};

    private void openChestEffect(String userId, Warehouse.WarehouseItem item, int chestId, int count, Map<String, Object> effect) {
        int idx = chestId - 11091;
        String[] parts = CHEST_PARTS[idx];
        String setName = CHEST_SET_NAMES[idx];

        List<String> obtained = new ArrayList<>();
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            String part = parts[rng.nextInt(parts.length)];
            obtained.add(part);
            equipmentService.createSetEquipment(userId, setName, part, "CHEST", item.getName());
        }
        logger.info("【宝箱开启】userId={}, chest={}, obtained={}", userId, item.getName(), obtained);
        effect.put("type", "chest");
        effect.put("equipment", obtained.size() == 1 ? obtained.get(0) : obtained);
        effect.put("setName", setName);
        effect.put("message", "开启" + item.getName() + "，获得：" + String.join("、", obtained));
    }

    /**
     * 按物品名称匹配效果（当 itemId 无法识别时的兜底）
     */
    private boolean applyEffectByName(String userId, UserResource resource, Warehouse.WarehouseItem item, int count, Map<String, Object> effect) {
        String name = item.getName();
        if (name == null || name.isEmpty()) return false;
        logger.info("【applyEffectByName】按名称匹配: name={}, count={}", name, count);

        // ── 白银类 ──
        if (name.contains("银锭"))   { addSilverEffect(resource, effect, 2000L * count); return true; }
        if (name.contains("银条"))   { addSilverEffect(resource, effect, 2000L * count); return true; }
        if (name.contains("白银袋")) { addSilverEffect(resource, effect, 10000L * count); return true; }
        if (name.contains("白银箱")) { addSilverEffect(resource, effect, 50000L * count); return true; }

        // ── 黄金类 ──
        if (name.contains("金锭"))   { addGoldEffect(resource, effect, 10L * count); return true; }
        if (name.contains("金条"))   { addGoldEffect(resource, effect, 20L * count); return true; }

        // ── 绑金类 ──
        if (name.contains("绑金")) {
            long unit = 30L;
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)绑金").matcher(name);
                if (m.find()) unit = Long.parseLong(m.group(1));
            } catch (Exception ignored) {}
            addBoundGoldEffect(userId, effect, unit * count);
            return true;
        }

        // ── 粮食类 ──
        if (name.contains("粮食包")) { addMaterialEffect(resource, effect, 11053, 2000L * count); return true; }
        if (name.contains("粮草"))   { addMaterialEffect(resource, effect, 11053, 2000L * count); return true; }

        // ── 金属类 ──
        if (name.contains("金属堆")) { addMaterialEffect(resource, effect, 11052, 2000L * count); return true; }
        if (name.contains("金属包")) { addMaterialEffect(resource, effect, 11052, 2000L * count); return true; }

        // ── 纸张类 ──
        if (name.contains("纸张包")) { addMaterialEffect(resource, effect, 11054, 2000L * count); return true; }

        // ── 声望符 ──
        if (name.contains("高级声望符")) { addFameEffect(userId, effect, 500L * count); return true; }
        if (name.contains("声望符"))     { addFameEffect(userId, effect, 100L * count); return true; }

        // ── 经验符 ──
        if (name.contains("君主经验符")) { addLordExpEffect(userId, effect, 5000L * count); return true; }

        // ── 精力丹 ──
        if (name.contains("精力丹")) {
            int stGain = 5 * count;
            userResourceService.addStamina(userId, stGain);
            effect.put("type", "stamina"); effect.put("gain", stGain);
            effect.put("message", "恢复" + stGain + "点精力");
            return true;
        }

        // ── 将魂 ──
        if (name.contains("将魂石")) {
            int soul = 50 * count;
            resource.setSoulPoint((resource.getSoulPoint() != null ? resource.getSoulPoint() : 0) + soul);
            resourceRepository.save(resource);
            effect.put("type", "soul"); effect.put("gain", soul);
            effect.put("message", "获得" + soul + "将魂");
            return true;
        }

        logger.warn("【applyEffectByName】无法匹配: name={}", name);
        return false;
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

