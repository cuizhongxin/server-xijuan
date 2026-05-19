package com.tencent.wxcloudrun.service.tactics;

import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.config.TacticsConfig.TacticsTemplate;
import com.tencent.wxcloudrun.dao.UserTacticsMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TacticsService {

    private static final Logger logger = LoggerFactory.getLogger(TacticsService.class);

    @Autowired
    private TacticsConfig tacticsConfig;

    @Autowired
    private UserTacticsMapper userTacticsMapper;

    @Autowired
    private GeneralRepository generalRepository;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private WarehouseService warehouseService;

    private final Random random = new Random();

    /**
     * 获取用户拥有的所有兵法（含等级）
     */
    public List<Map<String, Object>> getUserTactics(String userId) {
        List<Map<String, Object>> owned = userTacticsMapper.findByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : owned) {
            String tacticsId = (String) row.get("tacticsId");
            int level = ((Number) row.get("level")).intValue();
            int quantity = parseQuantity(row.get("quantity"));
            TacticsTemplate t = tacticsConfig.getById(tacticsId);
            if (t == null) continue;
            result.add(buildTacticsInfo(userId, t, level, quantity, true));
        }
        return result;
    }

    /**
     * 获取所有兵法配置（含用户拥有状态/等级）
     */
    public List<Map<String, Object>> getAllTacticsWithOwnership(String userId) {
        Map<String, Integer> ownedLevelMap = new HashMap<>();
        Map<String, Integer> ownedQuantityMap = new HashMap<>();
        for (Map<String, Object> row : userTacticsMapper.findByUserId(userId)) {
            String tacticsId = (String) row.get("tacticsId");
            ownedLevelMap.put(tacticsId, ((Number) row.get("level")).intValue());
            ownedQuantityMap.put(tacticsId, parseQuantity(row.get("quantity")));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TacticsTemplate t : tacticsConfig.getAllTemplates().values()) {
            boolean owned = ownedLevelMap.containsKey(t.getId());
            int level = owned ? ownedLevelMap.get(t.getId()) : 0;
            int quantity = owned ? ownedQuantityMap.getOrDefault(t.getId(), 1) : 0;
            result.add(buildTacticsInfo(userId, t, level, quantity, owned));
        }
        return result;
    }

    /**
     * 制造兵法（扣纸张+白银）
     */
    public Map<String, Object> craftTactics(String userId, String tacticsId) {
        TacticsTemplate t = tacticsConfig.getById(tacticsId);
        if (t == null) throw new BusinessException(400, "兵法不存在");
        if (t.isVipExclusive()) throw new BusinessException(400, "此兵法为VIP专属，不可制造");

        Map<String, Integer> cost = TacticsConfig.calcCraftCost(t);
        UserResource resource = userResourceService.getUserResource(userId);
        checkAndDeductResources(resource, cost, userId);

        userTacticsMapper.upsert(userId, tacticsId, 1, System.currentTimeMillis());
        Map<String, Object> latest = userTacticsMapper.findByUserIdAndTacticsId(userId, tacticsId);
        int quantity = latest == null ? 1 : parseQuantity(latest.get("quantity"));
        int level = latest == null ? 1 : ((Number) latest.get("level")).intValue();
        logger.info("用户 {} 制造兵法 {} ({})", userId, t.getName(), tacticsId);

        Map<String, Object> result = new HashMap<>();
        result.put("tacticsId", tacticsId);
        result.put("name", t.getName());
        result.put("level", level);
        result.put("quantity", quantity);
        result.put("cost", cost);
        result.put("remainingPaper", resource.getPaper());
        result.put("remainingSilver", resource.getSilver());
        return result;
    }

    /**
     * 升级兵法（扣资源，等级+1，上限10）
     */
    public Map<String, Object> upgradeTactics(String userId, String tacticsId) {
        TacticsTemplate t = tacticsConfig.getById(tacticsId);
        if (t == null) throw new BusinessException(400, "兵法不存在");

        Map<String, Object> existing = userTacticsMapper.findByUserIdAndTacticsId(userId, tacticsId);
        if (existing == null) throw new BusinessException(400, "未拥有此兵法");

        int currentLevel = ((Number) existing.get("level")).intValue();
        if (currentLevel >= 10) throw new BusinessException(400, "兵法已满级");

        Map<String, Object> cost = TacticsConfig.calcUpgradeCost(t, currentLevel);
        UserResource resource = userResourceService.getUserResource(userId);
        int silverCost = toInt(cost.get("silver"));
        String bookItemId = String.valueOf(cost.get("itemId"));
        String bookItemName = String.valueOf(cost.get("itemName"));
        int bookItemCount = Math.max(1, toInt(cost.get("itemCount")));
        int successRate = Math.max(0, Math.min(100, toInt(cost.get("successRate"))));

        long currentSilver = resource.getSilver() == null ? 0L : resource.getSilver();
        if (currentSilver < silverCost) {
            throw new BusinessException(400, "白银不足，需要" + silverCost);
        }
        int ownedBookCount = warehouseService.getItemCount(userId, bookItemId);
        if (ownedBookCount < bookItemCount) {
            throw new BusinessException(400, bookItemName + "不足，需要" + bookItemCount + "本");
        }

        // APK规则：升级尝试时消耗兵书与白银，成功率按等级判定
        resource.setSilver(currentSilver - silverCost);
        userResourceService.saveResource(resource);
        if (!warehouseService.removeItem(userId, bookItemId, bookItemCount)) {
            throw new BusinessException(400, bookItemName + "不足，升级失败");
        }

        int nextLevel = currentLevel + 1;
        boolean success = random.nextInt(100) < successRate;
        int finalLevel = currentLevel;
        if (success) {
            finalLevel = nextLevel;
            userTacticsMapper.updateLevel(userId, tacticsId, finalLevel);
        }
        int leftBookCount = warehouseService.getItemCount(userId, bookItemId);
        logger.info("用户 {} 升级兵法 {} ({}): lv{}->{} success={} rate={}%",
                userId, t.getName(), tacticsId, currentLevel, nextLevel, success, successRate);

        Map<String, Object> result = new HashMap<>();
        result.put("tacticsId", tacticsId);
        result.put("name", t.getName());
        result.put("success", success);
        result.put("level", finalLevel);
        result.put("fromLevel", currentLevel);
        result.put("targetLevel", nextLevel);
        result.put("successRate", successRate);
        result.put("cost", cost);
        result.put("bookItemId", bookItemId);
        result.put("bookItemName", bookItemName);
        result.put("bookConsumed", bookItemCount);
        result.put("bookRemaining", leftBookCount);
        result.put("silverConsumed", silverCost);
        result.put("effect", TacticsConfig.calcEffect(t, finalLevel));
        result.put("remainingSilver", resource.getSilver());
        result.put("message", success ? "升级成功" : "升级失败，本次未提升等级");
        return result;
    }

    /**
     * 装备兵法（兵种校验）
     */
    public Map<String, Object> equipTactics(String userId, String generalId, String tacticsId) {
        General general = generalRepository.findById(generalId);
        if (general == null) throw new BusinessException(404, "武将不存在");
        if (!userId.equals(general.getUserId())) throw new BusinessException(403, "武将不属于该用户");

        TacticsTemplate t = tacticsConfig.getById(tacticsId);
        if (t == null) throw new BusinessException(400, "兵法不存在");

        Map<String, Object> existing = userTacticsMapper.findByUserIdAndTacticsId(userId, tacticsId);
        if (existing == null) throw new BusinessException(400, "未拥有此兵法");
        int ownedQuantity = parseQuantity(existing.get("quantity"));
        int equippedCount = countEquippedByOtherGenerals(userId, generalId, tacticsId);
        if (equippedCount >= ownedQuantity) {
            throw new BusinessException(400, "该兵法已被其他武将装备，请先卸下或再制造一份");
        }

        if (t.isVipExclusive() && t.getExclusiveGeneralName() != null
                && !isExclusiveGeneralMatch(general.getName(), t.getExclusiveGeneralName())) {
            throw new BusinessException(400, "此兵法仅限" + t.getExclusiveGeneralName() + "装备");
        }

        String troopType = general.getTroopType();
        if (troopType != null && !troopType.equals(t.getTroopType()) && !t.isVipExclusive()) {
            throw new BusinessException(400, "此兵法仅适用于" + t.getTroopType() + "兵");
        }

        general.setTacticsId(tacticsId);
        general.setUpdateTime(System.currentTimeMillis());
        generalRepository.update(general);
        logger.info("武将 {} 装备兵法 {}", general.getName(), t.getName());

        int level = ((Number) existing.get("level")).intValue();
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("tacticsId", tacticsId);
        result.put("tacticsName", t.getName());
        result.put("tacticsLevel", level);
        result.put("ownedQuantity", ownedQuantity);
        result.put("equippedCount", equippedCount + 1);
        return result;
    }

    /**
     * 卸下兵法
     */
    public Map<String, Object> unequipTactics(String userId, String generalId) {
        General general = generalRepository.findById(generalId);
        if (general == null) throw new BusinessException(404, "武将不存在");
        if (!userId.equals(general.getUserId())) throw new BusinessException(403, "武将不属于该用户");

        String oldTacticsId = general.getTacticsId();
        general.setTacticsId(null);
        general.setUpdateTime(System.currentTimeMillis());
        generalRepository.update(general);
        logger.info("武将 {} 卸下兵法 {}", general.getName(), oldTacticsId);

        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("removedTacticsId", oldTacticsId);
        return result;
    }

    /**
     * 获取武将已装备兵法详情
     */
    public Map<String, Object> getEquippedTactics(String userId, String generalId) {
        General general = generalRepository.findById(generalId);
        if (general == null) throw new BusinessException(404, "武将不存在");
        if (!userId.equals(general.getUserId())) throw new BusinessException(403, "武将不属于该用户");

        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());

        if (general.getTacticsId() == null) {
            result.put("equipped", false);
            return result;
        }

        TacticsTemplate t = tacticsConfig.getById(general.getTacticsId());
        if (t == null) {
            result.put("equipped", false);
            return result;
        }

        Map<String, Object> owned = userTacticsMapper.findByUserIdAndTacticsId(userId, general.getTacticsId());
        int level = owned != null ? ((Number) owned.get("level")).intValue() : 1;

        result.put("equipped", true);
        int quantity = owned != null ? parseQuantity(owned.get("quantity")) : 1;
        result.put("tactics", buildTacticsInfo(userId, t, level, quantity, true));
        return result;
    }

    /**
     * 获取兵法效果数值
     */
    public double getTacticsEffect(String tacticsId, int level) {
        TacticsTemplate t = tacticsConfig.getById(tacticsId);
        if (t == null) return 0;
        return TacticsConfig.calcEffect(t, level);
    }

    /**
     * 授予兵法（VIP奖励等场景使用）
     */
    public void grantTactics(String userId, String tacticsId, int level) {
        userTacticsMapper.upsert(userId, tacticsId, level, System.currentTimeMillis());
        logger.info("授予用户 {} 兵法 {}, 等级 {}", userId, tacticsId, level);
    }

    // ==================== 内部方法 ====================

    private Map<String, Object> buildTacticsInfo(String userId, TacticsTemplate t, int level, int quantity, boolean owned) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", t.getId());
        info.put("name", t.getName());
        info.put("icon", t.getIcon());
        info.put("apkIconId", t.getApkIconId());
        info.put("troopType", t.getTroopType());
        info.put("category", t.getCategory());
        info.put("type", t.getCategory());
        info.put("description", t.getDescription());
        info.put("effectDesc", t.getEffectDesc());
        info.put("owned", owned);
        info.put("level", level);
        info.put("quantity", quantity);
        info.put("maxLevel", 10);
        info.put("vipExclusive", t.isVipExclusive());

        if (owned && level > 0) {
            double effectValue = TacticsConfig.calcEffect(t, level);
            info.put("effectValue", Math.round(effectValue * 10.0) / 10.0);
            info.put("triggerRate", TacticsConfig.calcTriggerRate(t, level));
            if (level < 10) {
                double nextVal = Math.round(TacticsConfig.calcEffect(t, level + 1) * 10.0) / 10.0;
                info.put("nextEffect", nextVal);
                info.put("nextEffectValue", nextVal);
                info.put("nextEffectDesc", t.getEffectDesc());
                Map<String, Object> upgradeCost = TacticsConfig.calcUpgradeCost(t, level);
                Object itemIdObj = upgradeCost.get("itemId");
                if (itemIdObj != null) {
                    int itemOwned = warehouseService.getItemCount(userId, String.valueOf(itemIdObj));
                    upgradeCost.put("itemOwned", itemOwned);
                }
                info.put("upgradeCost", upgradeCost);
            }
        } else {
            info.put("craftCost", TacticsConfig.calcCraftCost(t));
        }
        return info;
    }

    /**
     * 获取武将已学习的兵法列表
     */
    public List<Map<String, Object>> getLearnedTactics(String userId, String generalId) {
        List<Map<String, Object>> equipped = new ArrayList<>();
        Map<String, Object> eq = getEquippedTactics(userId, generalId);
        if (eq != null && eq.get("tactics") != null) {
            Object t = eq.get("tactics");
            if (t instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tList = (List<Map<String, Object>>) t;
                equipped.addAll(tList);
            } else if (t instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tMap = (Map<String, Object>) t;
                equipped.add(tMap);
            }
        }
        return equipped;
    }

    /**
     * 武将学习兵法
     */
    public Map<String, Object> learnTactic(String userId, String generalId, String tacticsId) {
        return equipTactics(userId, generalId, tacticsId);
    }

    private boolean isExclusiveGeneralMatch(String generalName, String exclusiveName) {
        if (generalName == null || exclusiveName == null) return false;
        String normalizedGeneral = generalName.replace("(狂)", "").replace("（狂）", "").trim();
        String normalizedExclusive = exclusiveName.trim();
        return normalizedGeneral.equals(normalizedExclusive)
                || normalizedGeneral.contains(normalizedExclusive);
    }

    private int parseQuantity(Object quantityObj) {
        if (quantityObj instanceof Number) {
            return Math.max(1, ((Number) quantityObj).intValue());
        }
        return 1;
    }

    private int countEquippedByOtherGenerals(String userId, String currentGeneralId, String tacticsId) {
        List<General> generals = generalRepository.findByUserId(userId);
        int count = 0;
        for (General g : generals) {
            if (g == null || g.getId() == null) continue;
            if (g.getId().equals(currentGeneralId)) continue;
            if (tacticsId.equals(g.getTacticsId())) {
                count++;
            }
        }
        return count;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void checkAndDeductResources(UserResource resource, Map<String, Integer> cost, String userId) {
        int paperCost = cost.getOrDefault("paper", 0);
        int silverCost = cost.getOrDefault("silver", 0);
        int goldCost = cost.getOrDefault("gold", 0);

        if (paperCost > 0 && (resource.getPaper() == null || resource.getPaper() < paperCost)) {
            throw new BusinessException(400, "纸张不足，需要" + paperCost);
        }
        if (silverCost > 0 && (resource.getSilver() == null || resource.getSilver() < silverCost)) {
            throw new BusinessException(400, "白银不足，需要" + silverCost);
        }
        if (goldCost > 0 && (resource.getGold() == null || resource.getGold() < goldCost)) {
            throw new BusinessException(400, "黄金不足，需要" + goldCost);
        }

        if (paperCost > 0) resource.setPaper(resource.getPaper() - paperCost);
        if (silverCost > 0) resource.setSilver(resource.getSilver() - silverCost);
        if (goldCost > 0) resource.setGold(resource.getGold() - goldCost);
        userResourceService.saveResource(resource);
    }
}
