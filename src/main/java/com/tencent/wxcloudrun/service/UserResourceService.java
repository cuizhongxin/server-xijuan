package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户资源服务
 */
@Service
public class UserResourceService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserResourceService.class);
    
    // 体力恢复间隔（5分钟恢复1点）
    private static final long STAMINA_RECOVER_INTERVAL = 5 * 60 * 1000;
    
    @Autowired
    private UserResourceRepository resourceRepository;
    
    /**
     * 获取用户资源（自动初始化）
     */
    public UserResource getUserResource(String odUserId) {
        UserResource resource = resourceRepository.findByUserId(odUserId);
        if (resource == null) {
            resource = resourceRepository.initUserResource(odUserId);
        }
        
        // 自动恢复体力
        recoverStamina(resource);
        
        return resource;
    }
    
    /**
     * 自动恢复体力
     */
    private void recoverStamina(UserResource resource) {
        long now = System.currentTimeMillis();
        long lastRecover = resource.getLastStaminaRecoverTime();
        
        if (resource.getStamina() < resource.getMaxStamina()) {
            long elapsed = now - lastRecover;
            int recovered = (int) (elapsed / STAMINA_RECOVER_INTERVAL);
            
            if (recovered > 0) {
                int newStamina = Math.min(resource.getMaxStamina(), resource.getStamina() + recovered);
                resource.setStamina(newStamina);
                resource.setLastStaminaRecoverTime(now);
                resourceRepository.save(resource);
            }
        }
    }
    
    /**
     * 消耗黄金
     */
    public boolean consumeGold(String odUserId, long amount) {
        UserResource resource = getUserResource(odUserId);
        if (resource.getGold() < amount) {
            return false;
        }
        resource.setGold(resource.getGold() - amount);
        resourceRepository.save(resource);
        logger.info("用户 {} 消耗黄金 {}, 剩余 {}", odUserId, amount, resource.getGold());
        return true;
    }
    
    /**
     * 增加黄金
     */
    public void addGold(String odUserId, long amount) {
        UserResource resource = getUserResource(odUserId);
        resource.setGold(resource.getGold() + amount);
        resourceRepository.save(resource);
        logger.info("用户 {} 增加黄金 {}, 现有 {}", odUserId, amount, resource.getGold());
    }
    
    /**
     * 消耗白银
     */
    public boolean consumeSilver(String odUserId, long amount) {
        UserResource resource = getUserResource(odUserId);
        if (resource.getSilver() < amount) {
            return false;
        }
        resource.setSilver(resource.getSilver() - amount);
        resourceRepository.save(resource);
        return true;
    }
    
    /**
     * 增加白银
     */
    public void addSilver(String odUserId, long amount) {
        UserResource resource = getUserResource(odUserId);
        resource.setSilver(resource.getSilver() + amount);
        resourceRepository.save(resource);
    }
    
    /**
     * 消耗钻石
     */
    public boolean consumeDiamond(String odUserId, long amount) {
        UserResource resource = getUserResource(odUserId);
        if (resource.getDiamond() < amount) {
            return false;
        }
        resource.setDiamond(resource.getDiamond() - amount);
        resourceRepository.save(resource);
        return true;
    }
    
    /**
     * 增加钻石
     */
    public void addDiamond(String odUserId, long amount) {
        UserResource resource = getUserResource(odUserId);
        resource.setDiamond(resource.getDiamond() + amount);
        resourceRepository.save(resource);
    }
    
    /**
     * 消耗体力
     */
    public boolean consumeStamina(String odUserId, int amount) {
        UserResource resource = getUserResource(odUserId);
        if (resource.getStamina() < amount) {
            return false;
        }
        resource.setStamina(resource.getStamina() - amount);
        resourceRepository.save(resource);
        return true;
    }
    
    /**
     * 增加体力
     */
    public void addStamina(String odUserId, int amount) {
        UserResource resource = getUserResource(odUserId);
        int newStamina = Math.min(resource.getMaxStamina() + 50, resource.getStamina() + amount); // 可以超过上限50
        resource.setStamina(newStamina);
        resourceRepository.save(resource);
    }
    
    /**
     * 消耗将令
     */
    public boolean consumeGeneralOrder(String odUserId, int amount) {
        UserResource resource = getUserResource(odUserId);
        if (resource.getGeneralOrder() < amount) {
            return false;
        }
        resource.setGeneralOrder(resource.getGeneralOrder() - amount);
        resourceRepository.save(resource);
        return true;
    }
    
    /**
     * 充值成功，增加相应货币
     */
    public void handleRecharge(String odUserId, long amountInFen, String currency) {
        UserResource resource = getUserResource(odUserId);
        
        // 更新累计充值
        resource.setTotalRecharge(resource.getTotalRecharge() + amountInFen);
        
        // 根据充值金额增加货币（1元=10黄金，1元=100钻石）
        long yuan = amountInFen / 100;
        long gold = yuan * 10;
        long diamond = yuan * 100;
        
        // 首充双倍
        if (resource.getTotalRecharge() == amountInFen) {
            gold *= 2;
            diamond *= 2;
        }
        
        resource.setGold(resource.getGold() + gold);
        resource.setDiamond(resource.getDiamond() + diamond);
        
        // 更新VIP
        updateVipLevel(resource);
        
        resourceRepository.save(resource);
        logger.info("用户 {} 充值 {} 分，获得黄金 {}, 钻石 {}", odUserId, amountInFen, gold, diamond);
    }
    
    /**
     * 更新VIP等级
     */
    private void updateVipLevel(UserResource resource) {
        long totalYuan = resource.getTotalRecharge() / 100;
        
        // VIP等级规则
        int[] vipThresholds = {0, 6, 30, 98, 198, 328, 648, 998, 1998, 3998, 6998, 9998, 19998};
        int vipLevel = 0;
        
        for (int i = vipThresholds.length - 1; i >= 0; i--) {
            if (totalYuan >= vipThresholds[i]) {
                vipLevel = i;
                break;
            }
        }
        
        resource.setVipLevel(vipLevel);
        resource.setVipExp(totalYuan);
    }
    
    /**
     * 更新武将数量
     */
    public void updateGeneralCount(String odUserId, int count) {
        UserResource resource = getUserResource(odUserId);
        resource.setGeneralCount(count);
        resourceRepository.save(resource);
    }
    
    /**
     * 增加声望
     */
    public void addFame(String odUserId, long amount) {
        UserResource resource = getUserResource(odUserId);
        resource.setFame(resource.getFame() + amount);
        
        // 更新爵位
        updateRank(resource);
        
        resourceRepository.save(resource);
    }
    
    /**
     * 更新爵位
     */
    private void updateRank(UserResource resource) {
        long fame = resource.getFame();
        String rank;
        
        if (fame >= 1000000) {
            rank = "王";
        } else if (fame >= 500000) {
            rank = "公";
        } else if (fame >= 200000) {
            rank = "侯";
        } else if (fame >= 100000) {
            rank = "伯";
        } else if (fame >= 50000) {
            rank = "子";
        } else if (fame >= 20000) {
            rank = "男";
        } else if (fame >= 10000) {
            rank = "士人";
        } else if (fame >= 5000) {
            rank = "平民";
        } else {
            rank = "白身";
        }
        
        resource.setRank(rank);
    }
    
    /**
     * 获取武将位信息
     */
    public java.util.Map<String, Object> getGeneralSlotInfo(String odUserId) {
        UserResource resource = getUserResource(odUserId);
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        int baseSlots = resource.getBaseGeneralSlots() != null ? resource.getBaseGeneralSlots() : 10;
        int purchasedSlots = resource.getPurchasedSlots() != null ? resource.getPurchasedSlots() : 0;
        int vipBonusSlots = getVipBonusSlots(resource.getVipLevel());
        int maxSlots = Math.min(50, baseSlots + purchasedSlots + vipBonusSlots);
        
        result.put("baseSlots", baseSlots);
        result.put("purchasedSlots", purchasedSlots);
        result.put("vipBonusSlots", vipBonusSlots);
        result.put("currentSlots", maxSlots);
        result.put("maxSlots", 50);
        result.put("canPurchase", maxSlots < 50);
        result.put("nextPurchaseCost", getNextPurchaseCost(purchasedSlots));
        
        return result;
    }
    
    /**
     * 购买武将位（每次购买1个）
     */
    public java.util.Map<String, Object> purchaseGeneralSlot(String odUserId) {
        UserResource resource = getUserResource(odUserId);
        
        int baseSlots = resource.getBaseGeneralSlots() != null ? resource.getBaseGeneralSlots() : 10;
        int purchasedSlots = resource.getPurchasedSlots() != null ? resource.getPurchasedSlots() : 0;
        int vipBonusSlots = getVipBonusSlots(resource.getVipLevel());
        int currentSlots = baseSlots + purchasedSlots + vipBonusSlots;
        
        // 检查是否已达上限
        if (currentSlots >= 50) {
            throw new BusinessException(400, "武将位已达上限50个");
        }
        
        // 计算价格（首次100，之后每次翻倍，但最高不超过10000）
        long cost = getNextPurchaseCost(purchasedSlots);
        
        // 检查并消耗黄金
        if (resource.getGold() < cost) {
            throw new BusinessException(400, "黄金不足，需要" + cost + "黄金");
        }
        
        resource.setGold(resource.getGold() - cost);
        resource.setPurchasedSlots(purchasedSlots + 1);
        
        // 更新最大武将数
        resource.setMaxGeneral(Math.min(50, baseSlots + purchasedSlots + 1 + vipBonusSlots));
        
        resourceRepository.save(resource);
        
        logger.info("用户 {} 购买了1个武将位，花费 {} 黄金，当前拥有 {} 个武将位", 
                   odUserId, cost, resource.getMaxGeneral());
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("cost", cost);
        result.put("newSlots", resource.getMaxGeneral());
        result.put("remainingGold", resource.getGold());
        result.put("nextPurchaseCost", getNextPurchaseCost(purchasedSlots + 1));
        
        return result;
    }
    
    /**
     * 获取下次购买武将位的价格
     */
    private long getNextPurchaseCost(int purchasedCount) {
        // 首次100，之后每次翻倍：100, 200, 400, 800...最高10000
        long cost = 100L * (1L << purchasedCount);
        return Math.min(10000, cost);
    }
    
    /**
     * 根据VIP等级获取额外武将位
     */
    private int getVipBonusSlots(Integer vipLevel) {
        if (vipLevel == null) return 0;
        // VIP每级增加2个武将位
        return vipLevel * 2;
    }
    
    /**
     * 检查是否可以招募新武将
     */
    public boolean canRecruitGeneral(String odUserId, int currentGeneralCount) {
        UserResource resource = getUserResource(odUserId);
        
        int baseSlots = resource.getBaseGeneralSlots() != null ? resource.getBaseGeneralSlots() : 10;
        int purchasedSlots = resource.getPurchasedSlots() != null ? resource.getPurchasedSlots() : 0;
        int vipBonusSlots = getVipBonusSlots(resource.getVipLevel());
        int maxSlots = Math.min(50, baseSlots + purchasedSlots + vipBonusSlots);
        
        return currentGeneralCount < maxSlots;
    }
    
    /**
     * 获取用户最大武将位数量
     */
    public int getMaxGeneralSlots(String odUserId) {
        UserResource resource = getUserResource(odUserId);
        
        int baseSlots = resource.getBaseGeneralSlots() != null ? resource.getBaseGeneralSlots() : 10;
        int purchasedSlots = resource.getPurchasedSlots() != null ? resource.getPurchasedSlots() : 0;
        int vipBonusSlots = getVipBonusSlots(resource.getVipLevel());
        
        return Math.min(50, baseSlots + purchasedSlots + vipBonusSlots);
    }
    
    /**
     * 初始化用户资源
     */
    public void initUserResource(String odUserId) {
        resourceRepository.initUserResource(odUserId);
        logger.info("初始化用户资源, userId: {}", odUserId);
    }
    
    /**
     * 保存用户资源
     */
    public void saveUserResource(UserResource resource) {
        resourceRepository.save(resource);
    }
    
    /**
     * 保存资源（别名方法）
     */
    public void saveResource(UserResource resource) {
        resourceRepository.save(resource);
    }
}
