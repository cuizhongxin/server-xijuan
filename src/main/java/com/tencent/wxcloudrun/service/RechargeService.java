package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.RechargeOrderMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.RechargeOrder;
import com.tencent.wxcloudrun.model.RechargeProduct;
import com.tencent.wxcloudrun.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 充值服务（数据库存储）
 */
@Service
public class RechargeService {
    
    private static final Logger logger = LoggerFactory.getLogger(RechargeService.class);
    
    @Autowired
    private UserResourceService resourceService;
    
    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;
    
    // 充值商品列表（只读配置，保留内存）
    private final List<RechargeProduct> products = new ArrayList<>();
    
    public RechargeService() {
        initProducts();
    }
    
    private void initProducts() {
        products.add(RechargeProduct.builder().id("recharge_6").name("小礼包").description("首充特惠").icon("💰").price(600L).originalPrice(600L).goldAmount(60L).diamondAmount(600L).hot(true).recommended(true).discount(0).sortOrder(1).enabled(true).build());
        products.add(RechargeProduct.builder().id("recharge_30").name("中礼包").description("超值优惠").icon("💎").price(3000L).originalPrice(3000L).goldAmount(300L).diamondAmount(3500L).bonusItems("[{\"type\":\"recruitToken\",\"amount\":5}]").hot(false).recommended(true).discount(0).sortOrder(2).enabled(true).build());
        products.add(RechargeProduct.builder().id("recharge_68").name("大礼包").description("热销推荐").icon("👑").price(6800L).originalPrice(6800L).goldAmount(680L).diamondAmount(8000L).bonusItems("[{\"type\":\"recruitToken\",\"amount\":10},{\"type\":\"advancedRecruitToken\",\"amount\":2}]").hot(true).recommended(true).discount(0).sortOrder(3).enabled(true).build());
        products.add(RechargeProduct.builder().id("recharge_128").name("豪华礼包").description("物超所值").icon("🏆").price(12800L).originalPrice(12800L).goldAmount(1280L).diamondAmount(16000L).bonusItems("[{\"type\":\"recruitToken\",\"amount\":20},{\"type\":\"advancedRecruitToken\",\"amount\":5}]").hot(false).recommended(false).discount(0).sortOrder(4).enabled(true).build());
        products.add(RechargeProduct.builder().id("recharge_328").name("至尊礼包").description("尊贵之选").icon("💠").price(32800L).originalPrice(32800L).goldAmount(3280L).diamondAmount(42000L).bonusItems("[{\"type\":\"recruitToken\",\"amount\":50},{\"type\":\"advancedRecruitToken\",\"amount\":15}]").hot(false).recommended(false).discount(0).sortOrder(5).enabled(true).build());
        products.add(RechargeProduct.builder().id("recharge_648").name("王者礼包").description("王者专属").icon("🌟").price(64800L).originalPrice(64800L).goldAmount(6480L).diamondAmount(90000L).bonusItems("[{\"type\":\"recruitToken\",\"amount\":100},{\"type\":\"advancedRecruitToken\",\"amount\":30},{\"type\":\"orangeGeneralBox\",\"amount\":1}]").hot(true).recommended(false).discount(0).sortOrder(6).enabled(true).build());
    }
    
    public List<RechargeProduct> getProducts() {
        List<RechargeProduct> enabled = new ArrayList<>();
        for (RechargeProduct p : products) { if (p.getEnabled()) { enabled.add(p); } }
        enabled.sort((a, b) -> a.getSortOrder() - b.getSortOrder());
        return enabled;
    }
    
    public RechargeProduct getProduct(String productId) {
        for (RechargeProduct p : products) { if (p.getId().equals(productId)) { return p; } }
        return null;
    }
    
    public RechargeOrder createOrder(String odUserId, String productId, String paymentMethod) {
        RechargeProduct product = getProduct(productId);
        if (product == null || !product.getEnabled()) { throw new BusinessException(400, "商品不存在或已下架"); }
        
        String orderId = "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        RechargeOrder order = RechargeOrder.builder()
                .id(orderId).odUserId(odUserId).amount(product.getPrice()).productId(productId)
                .productName(product.getName()).paymentMethod(paymentMethod).status(RechargeOrder.Status.PENDING)
                .goldAmount(product.getGoldAmount()).diamondAmount(product.getDiamondAmount())
                .bonusItems(product.getBonusItems()).createTime(System.currentTimeMillis()).updateTime(System.currentTimeMillis())
                .build();
        
        rechargeOrderMapper.upsert(order);
        logger.info("创建充值订单: {}, 用户: {}, 商品: {}, 支付方式: {}", orderId, odUserId, productId, paymentMethod);
        return order;
    }
    
    public RechargeOrder getOrder(String orderId) {
        return rechargeOrderMapper.findById(orderId);
    }
    
    public List<RechargeOrder> getUserOrders(String odUserId) {
        List<RechargeOrder> result = rechargeOrderMapper.findByUserId(odUserId);
        return result != null ? result : new ArrayList<>();
    }
    
    public Map<String, Object> handleWechatCallback(String orderId, String tradeNo, boolean success) {
        return handlePaymentCallback(orderId, tradeNo, RechargeOrder.PaymentMethod.WECHAT, success);
    }
    
    public Map<String, Object> handleAlipayCallback(String orderId, String tradeNo, boolean success) {
        return handlePaymentCallback(orderId, tradeNo, RechargeOrder.PaymentMethod.ALIPAY, success);
    }
    
    public Map<String, Object> handleUnionpayCallback(String orderId, String tradeNo, boolean success) {
        return handlePaymentCallback(orderId, tradeNo, RechargeOrder.PaymentMethod.UNIONPAY, success);
    }
    
    private Map<String, Object> handlePaymentCallback(String orderId, String tradeNo, String paymentMethod, boolean success) {
        RechargeOrder order = rechargeOrderMapper.findById(orderId);
        if (order == null) { throw new BusinessException(400, "订单不存在"); }
        if (!RechargeOrder.Status.PENDING.equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态异常，当前状态: " + order.getStatus());
        }
        
        order.setTradeNo(tradeNo);
        order.setPayTime(System.currentTimeMillis());
        order.setUpdateTime(System.currentTimeMillis());
        
        Map<String, Object> result = new HashMap<>();
        if (success) {
            order.setStatus(RechargeOrder.Status.PAID);
            rechargeOrderMapper.upsert(order);
            
            // 发放道具
            if (order.getGoldAmount() != null && order.getGoldAmount() > 0) {
                resourceService.addGold(order.getOdUserId(), order.getGoldAmount().intValue());
            }
            if (order.getDiamondAmount() != null && order.getDiamondAmount() > 0) {
                resourceService.addDiamond(order.getOdUserId(), order.getDiamondAmount().intValue());
            }
            
            // 累计充值金额并更新VIP等级
            updateVipLevel(order.getOdUserId(), order.getAmount());

            result.put("success", true);
            result.put("order", order);
            result.put("message", "充值成功");
            logger.info("充值成功: orderId={}, userId={}, amount={}", orderId, order.getOdUserId(), order.getAmount());
        } else {
            order.setStatus(RechargeOrder.Status.FAILED);
            rechargeOrderMapper.upsert(order);
            result.put("success", false);
            result.put("message", "支付失败");
            logger.warn("充值失败: orderId={}", orderId);
        }
        return result;
    }
    
    public Map<String, Object> mockPayment(String orderId) {
        RechargeOrder order = rechargeOrderMapper.findById(orderId);
        if (order == null) { throw new BusinessException(400, "订单不存在"); }
        return handlePaymentCallback(orderId, "MOCK_" + System.currentTimeMillis(), order.getPaymentMethod(), true);
    }
    
    public Map<String, String> generateWechatPayParams(RechargeOrder order) {
        Map<String, String> params = new HashMap<>();
        params.put("appId", "wx_mock_appid");
        params.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
        params.put("package", "prepay_id=mock_" + order.getId());
        params.put("signType", "MD5");
        params.put("paySign", "mock_sign");
        return params;
    }
    
    public Map<String, String> generateAlipayParams(RechargeOrder order) {
        Map<String, String> params = new HashMap<>();
        params.put("orderString", "mock_alipay_order_" + order.getId());
        return params;
    }
    
    public Map<String, String> generateUnionpayParams(RechargeOrder order) {
        Map<String, String> params = new HashMap<>();
        params.put("tn", "mock_unionpay_tn_" + order.getId());
        return params;
    }

    // VIP等级阈值（分）：0, 6元, 30元, 98元, 198元, 328元, 648元, 998元, 1998元, 6000元, 20000元
    private static final long[] VIP_THRESHOLDS = {0, 600, 3000, 9800, 19800, 32800, 64800, 99800, 199800, 600000, 2000000};

    private void updateVipLevel(String odUserId, long amountFen) {
        UserResource resource = resourceService.getUserResource(odUserId);
        long totalRecharge = (resource.getTotalRecharge() != null ? resource.getTotalRecharge() : 0) + amountFen;
        resource.setTotalRecharge(totalRecharge);

        int newVipLevel = 0;
        for (int i = VIP_THRESHOLDS.length - 1; i >= 1; i--) {
            if (totalRecharge >= VIP_THRESHOLDS[i]) {
                newVipLevel = i;
                break;
            }
        }
        resource.setVipLevel(newVipLevel);
        resource.setUpdateTime(System.currentTimeMillis());
        resourceService.saveResource(resource);
        logger.info("用户 {} 累计充值 {}分，VIP等级更新为 {}", odUserId, totalRecharge, newVipLevel);
    }

    // ════════════════════════ 首充绑金 ════════════════════════
    // 每个产品只能领一次首充绑金，金额 = 该产品获得的黄金数
    private final Map<String, Set<String>> firstRechargeClaimed = new ConcurrentHashMap<>();

    public Map<String, Object> getFirstRechargeInfo(String userId) {
        Set<String> claimed = firstRechargeClaimed.getOrDefault(userId, Collections.emptySet());
        List<RechargeOrder> paidOrders = getUserOrders(userId);
        Set<String> paidProducts = new HashSet<>();
        for (RechargeOrder o : paidOrders) {
            if (RechargeOrder.Status.PAID.equals(o.getStatus())) paidProducts.add(o.getProductId());
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (RechargeProduct p : getProducts()) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", p.getId());
            item.put("name", p.getName());
            item.put("gold", p.getGoldAmount());
            item.put("boundGold", p.getGoldAmount());
            boolean hasPaid = paidProducts.contains(p.getId());
            boolean hasClaimed = claimed.contains(p.getId());
            item.put("status", hasClaimed ? "claimed" : (hasPaid ? "canClaim" : "notPaid"));
            items.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("hint", "主公,首次充值任意黄金即可获得等额绑金奖励!");
        return result;
    }

    public Map<String, Object> claimFirstRechargeBonus(String userId, String productId) {
        Set<String> claimed = firstRechargeClaimed.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        if (claimed.contains(productId)) throw new BusinessException(400, "活动奖励已领取!");

        List<RechargeOrder> orders = getUserOrders(userId);
        boolean hasPaid = false;
        for (RechargeOrder o : orders) {
            if (o.getProductId().equals(productId) && RechargeOrder.Status.PAID.equals(o.getStatus())) {
                hasPaid = true; break;
            }
        }
        if (!hasPaid) throw new BusinessException(400, "还没达到领取条件,领取奖励失败!");

        RechargeProduct product = getProduct(productId);
        if (product == null) throw new BusinessException(400, "商品不存在");

        long bonus = product.getGoldAmount();
        resourceService.addBoundGold(userId, bonus);
        claimed.add(productId);
        logger.info("用户 {} 领取首充绑金: productId={}, bonus={}", userId, productId, bonus);

        Map<String, Object> result = new HashMap<>();
        result.put("boundGold", bonus);
        result.put("message", "领取奖励成功!");
        return result;
    }

    // ════════════════════════ 充值活动（自定义配置） ════════════════════════
    // 活动档位：累充满 X 黄金 → 送道具/装备列表
    private static final List<Map<String, Object>> ACTIVITY_TIERS;
    static {
        ACTIVITY_TIERS = new ArrayList<>();
        ACTIVITY_TIERS.add(buildTier(1, 100,  "累充100黄金",  new String[][]{{"绑金", "100"}, {"初级粮食", "50"}}));
        ACTIVITY_TIERS.add(buildTier(2, 500,  "累充500黄金",  new String[][]{{"绑金", "300"}, {"高级招贤令", "2"}, {"3级强化石", "5"}}));
        ACTIVITY_TIERS.add(buildTier(3, 1000, "累充1000黄金", new String[][]{{"绑金", "800"}, {"高级招贤令", "5"}, {"品质石", "3"}, {"蓝色装备箱", "1"}}));
        ACTIVITY_TIERS.add(buildTier(4, 5000, "累充5000黄金", new String[][]{{"绑金", "3000"}, {"高级招贤令", "15"}, {"5级强化石", "10"}, {"紫色装备箱", "1"}}));
        ACTIVITY_TIERS.add(buildTier(5, 10000,"累充10000黄金",new String[][]{{"绑金", "8000"}, {"高级招贤令", "30"}, {"6级强化石", "5"}, {"橙色装备箱", "1"}, {"橙色将魂", "50"}}));
    }

    private static Map<String, Object> buildTier(int id, int threshold, String name, String[][] rewards) {
        Map<String, Object> tier = new HashMap<>();
        tier.put("id", id);
        tier.put("threshold", threshold);
        tier.put("name", name);
        List<Map<String, Object>> rewardList = new ArrayList<>();
        for (String[] r : rewards) {
            Map<String, Object> ri = new HashMap<>();
            ri.put("name", r[0]);
            ri.put("amount", Integer.parseInt(r[1]));
            rewardList.add(ri);
        }
        tier.put("rewards", rewardList);
        return tier;
    }

    private static final String ACTIVITY_NAME = "充值活动";
    private static final String ACTIVITY_START = "2026-01-01";
    private static final String ACTIVITY_END   = "2026-12-31";

    private final Map<String, Set<Integer>> activityClaimed = new ConcurrentHashMap<>();

    public Map<String, Object> getRechargeActivityInfo(String userId) {
        UserResource resource = resourceService.getUserResource(userId);
        long totalRechargeGold = (resource.getTotalRecharge() != null ? resource.getTotalRecharge() : 0) / 100;

        Set<Integer> claimed = activityClaimed.getOrDefault(userId, Collections.emptySet());

        List<Map<String, Object>> tiers = new ArrayList<>();
        for (Map<String, Object> t : ACTIVITY_TIERS) {
            Map<String, Object> tier = new HashMap<>(t);
            int id = (int) t.get("id");
            int threshold = (int) t.get("threshold");
            boolean reached = totalRechargeGold >= threshold;
            boolean isClaimed = claimed.contains(id);
            tier.put("status", isClaimed ? "claimed" : (reached ? "canClaim" : "locked"));
            tiers.add(tier);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("activityName", ACTIVITY_NAME);
        result.put("startDate", ACTIVITY_START);
        result.put("endDate", ACTIVITY_END);
        result.put("totalRechargeGold", totalRechargeGold);
        result.put("tiers", tiers);
        result.put("hint", String.format("主公,在%s至%s期间,充值额度累积满指定黄金即可领取丰厚奖励!", ACTIVITY_START, ACTIVITY_END));
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> claimActivityReward(String userId, int tierId) {
        Set<Integer> claimed = activityClaimed.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        if (claimed.contains(tierId)) throw new BusinessException(400, "活动奖励已领取!");

        Map<String, Object> tierCfg = null;
        for (Map<String, Object> t : ACTIVITY_TIERS) {
            if ((int) t.get("id") == tierId) { tierCfg = t; break; }
        }
        if (tierCfg == null) throw new BusinessException(400, "活动档位不存在");

        UserResource resource = resourceService.getUserResource(userId);
        long totalGold = (resource.getTotalRecharge() != null ? resource.getTotalRecharge() : 0) / 100;
        int threshold = (int) tierCfg.get("threshold");
        if (totalGold < threshold) throw new BusinessException(400, "还没达到领取条件,领取奖励失败!");

        List<Map<String, Object>> rewards = (List<Map<String, Object>>) tierCfg.get("rewards");
        for (Map<String, Object> r : rewards) {
            String name = (String) r.get("name");
            int amount = (int) r.get("amount");
            if ("绑金".equals(name)) {
                resourceService.addBoundGold(userId, amount);
            }
        }

        claimed.add(tierId);
        logger.info("用户 {} 领取活动奖励: tierId={}, threshold={}", userId, tierId, threshold);

        Map<String, Object> result = new HashMap<>();
        result.put("rewards", rewards);
        result.put("message", "领取奖励成功!");
        return result;
    }
}