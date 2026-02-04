package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.RechargeOrder;
import com.tencent.wxcloudrun.model.RechargeProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 充值服务
 */
@Service
public class RechargeService {
    
    private static final Logger logger = LoggerFactory.getLogger(RechargeService.class);
    
    @Autowired
    private UserResourceService resourceService;
    
    // 充值商品列表
    private final List<RechargeProduct> products = new ArrayList<>();
    
    // 订单存储
    private final Map<String, RechargeOrder> orderStore = new ConcurrentHashMap<>();
    
    public RechargeService() {
        initProducts();
    }
    
    /**
     * 初始化充值商品
     */
    private void initProducts() {
        products.add(RechargeProduct.builder()
                .id("recharge_6")
                .name("小礼包")
                .description("首充特惠")
                .icon("💰")
                .price(600L)
                .originalPrice(600L)
                .goldAmount(60L)
                .diamondAmount(600L)
                .hot(true)
                .recommended(true)
                .discount(0)
                .sortOrder(1)
                .enabled(true)
                .build());
        
        products.add(RechargeProduct.builder()
                .id("recharge_30")
                .name("中礼包")
                .description("超值优惠")
                .icon("💎")
                .price(3000L)
                .originalPrice(3000L)
                .goldAmount(300L)
                .diamondAmount(3500L)
                .bonusItems("[{\"type\":\"recruitToken\",\"amount\":5}]")
                .hot(false)
                .recommended(true)
                .discount(0)
                .sortOrder(2)
                .enabled(true)
                .build());
        
        products.add(RechargeProduct.builder()
                .id("recharge_68")
                .name("大礼包")
                .description("热销推荐")
                .icon("👑")
                .price(6800L)
                .originalPrice(6800L)
                .goldAmount(680L)
                .diamondAmount(8000L)
                .bonusItems("[{\"type\":\"recruitToken\",\"amount\":10},{\"type\":\"advancedRecruitToken\",\"amount\":2}]")
                .hot(true)
                .recommended(true)
                .discount(0)
                .sortOrder(3)
                .enabled(true)
                .build());
        
        products.add(RechargeProduct.builder()
                .id("recharge_128")
                .name("豪华礼包")
                .description("物超所值")
                .icon("🏆")
                .price(12800L)
                .originalPrice(12800L)
                .goldAmount(1280L)
                .diamondAmount(16000L)
                .bonusItems("[{\"type\":\"recruitToken\",\"amount\":20},{\"type\":\"advancedRecruitToken\",\"amount\":5}]")
                .hot(false)
                .recommended(false)
                .discount(0)
                .sortOrder(4)
                .enabled(true)
                .build());
        
        products.add(RechargeProduct.builder()
                .id("recharge_328")
                .name("至尊礼包")
                .description("尊贵之选")
                .icon("💠")
                .price(32800L)
                .originalPrice(32800L)
                .goldAmount(3280L)
                .diamondAmount(42000L)
                .bonusItems("[{\"type\":\"recruitToken\",\"amount\":50},{\"type\":\"advancedRecruitToken\",\"amount\":15}]")
                .hot(false)
                .recommended(false)
                .discount(0)
                .sortOrder(5)
                .enabled(true)
                .build());
        
        products.add(RechargeProduct.builder()
                .id("recharge_648")
                .name("王者礼包")
                .description("王者专属")
                .icon("🌟")
                .price(64800L)
                .originalPrice(64800L)
                .goldAmount(6480L)
                .diamondAmount(90000L)
                .bonusItems("[{\"type\":\"recruitToken\",\"amount\":100},{\"type\":\"advancedRecruitToken\",\"amount\":30},{\"type\":\"orangeGeneralBox\",\"amount\":1}]")
                .hot(true)
                .recommended(false)
                .discount(0)
                .sortOrder(6)
                .enabled(true)
                .build());
    }
    
    /**
     * 获取充值商品列表
     */
    public List<RechargeProduct> getProducts() {
        List<RechargeProduct> enabled = new ArrayList<>();
        for (RechargeProduct p : products) {
            if (p.getEnabled()) {
                enabled.add(p);
            }
        }
        enabled.sort((a, b) -> a.getSortOrder() - b.getSortOrder());
        return enabled;
    }
    
    /**
     * 获取商品详情
     */
    public RechargeProduct getProduct(String productId) {
        for (RechargeProduct p : products) {
            if (p.getId().equals(productId)) {
                return p;
            }
        }
        return null;
    }
    
    /**
     * 创建充值订单
     */
    public RechargeOrder createOrder(String odUserId, String productId, String paymentMethod) {
        RechargeProduct product = getProduct(productId);
        if (product == null || !product.getEnabled()) {
            throw new BusinessException(400, "商品不存在或已下架");
        }
        
        // 生成订单号
        String orderId = "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        RechargeOrder order = RechargeOrder.builder()
                .id(orderId)
                .odUserId(odUserId)
                .amount(product.getPrice())
                .productId(productId)
                .productName(product.getName())
                .paymentMethod(paymentMethod)
                .status(RechargeOrder.Status.PENDING)
                .goldAmount(product.getGoldAmount())
                .diamondAmount(product.getDiamondAmount())
                .bonusItems(product.getBonusItems())
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();
        
        orderStore.put(orderId, order);
        logger.info("创建充值订单: {}, 用户: {}, 商品: {}, 支付方式: {}", orderId, odUserId, productId, paymentMethod);
        
        return order;
    }
    
    /**
     * 获取订单
     */
    public RechargeOrder getOrder(String orderId) {
        return orderStore.get(orderId);
    }
    
    /**
     * 获取用户订单列表
     */
    public List<RechargeOrder> getUserOrders(String odUserId) {
        List<RechargeOrder> userOrders = new ArrayList<>();
        for (RechargeOrder order : orderStore.values()) {
            if (odUserId.equals(order.getOdUserId())) {
                userOrders.add(order);
            }
        }
        userOrders.sort((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        return userOrders;
    }
    
    /**
     * 处理微信支付回调
     */
    public Map<String, Object> handleWechatCallback(String orderId, String tradeNo, boolean success) {
        return handlePaymentCallback(orderId, tradeNo, RechargeOrder.PaymentMethod.WECHAT, success);
    }
    
    /**
     * 处理支付宝回调
     */
    public Map<String, Object> handleAlipayCallback(String orderId, String tradeNo, boolean success) {
        return handlePaymentCallback(orderId, tradeNo, RechargeOrder.PaymentMethod.ALIPAY, success);
    }
    
    /**
     * 处理银联回调
     */
    public Map<String, Object> handleUnionpayCallback(String orderId, String tradeNo, boolean success) {
        return handlePaymentCallback(orderId, tradeNo, RechargeOrder.PaymentMethod.UNIONPAY, success);
    }
    
    /**
     * 统一处理支付回调
     */
    private Map<String, Object> handlePaymentCallback(String orderId, String tradeNo, String paymentMethod, boolean success) {
        RechargeOrder order = orderStore.get(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        
        if (!RechargeOrder.Status.PENDING.equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态异常");
        }
        
        order.setTradeNo(tradeNo);
        order.setUpdateTime(System.currentTimeMillis());
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        
        if (success) {
            order.setStatus(RechargeOrder.Status.PAID);
            order.setPayTime(System.currentTimeMillis());
            
            // 发放道具
            resourceService.handleRecharge(order.getOdUserId(), order.getAmount(), paymentMethod);
            
            result.put("success", true);
            result.put("goldAmount", order.getGoldAmount());
            result.put("diamondAmount", order.getDiamondAmount());
            
            logger.info("支付成功: 订单 {}, 用户 {}, 金额 {} 分", orderId, order.getOdUserId(), order.getAmount());
        } else {
            order.setStatus(RechargeOrder.Status.FAILED);
            result.put("success", false);
            
            logger.warn("支付失败: 订单 {}", orderId);
        }
        
        orderStore.put(orderId, order);
        return result;
    }
    
    /**
     * 生成微信支付参数
     * 
     * 正式环境需要:
     * 1. 调用微信统一下单API获取prepay_id
     * 2. 使用商户API密钥生成签名
     */
    public Map<String, Object> generateWechatPayParams(RechargeOrder order) {
        Map<String, Object> params = new HashMap<>();
        
        // 小程序AppID (需要替换为真实的)
        String appId = "wx8f7d0509e671ceb4";
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        
        // 正式环境需要调用微信统一下单API获取prepay_id
        // 这里模拟返回
        String prepayId = "wx" + System.currentTimeMillis();
        
        // ========== 前端 wx.requestPayment 需要的参数 ==========
        params.put("appId", appId);
        params.put("timeStamp", timeStamp);
        params.put("nonceStr", nonceStr);
        params.put("package", "prepay_id=" + prepayId);
        params.put("signType", "RSA");
        params.put("paySign", generatePaySign(appId, timeStamp, nonceStr, "prepay_id=" + prepayId));
        
        // ========== 统一下单API需要的参数（兼容不同格式） ==========
        // 驼峰格式
        params.put("totalFee", order.getAmount());
        params.put("outTradeNo", order.getId());
        // 下划线格式（微信官方格式）
        params.put("total_fee", order.getAmount());
        params.put("out_trade_no", order.getId());
        // 大写下划线格式（某些SDK使用）
        params.put("TOTAL_FEE", order.getAmount());
        params.put("OUT_TRADE_NO", order.getId());
        
        // 其他常用参数
        params.put("orderId", order.getId());
        params.put("body", order.getProductName());
        params.put("amount", order.getAmount());
        
        return params;
    }
    
    /**
     * 生成支付签名
     * 正式环境需要使用商户API密钥进行签名
     */
    private String generatePaySign(String appId, String timeStamp, String nonceStr, String packageVal) {
        // 正式环境签名算法 (RSA-SHA256):
        // 1. 构造签名串: 
        //    appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + package + "\n"
        // 2. 使用商户私钥进行RSA-SHA256签名
        // 3. 对签名结果进行Base64编码
        
        // 模拟签名（测试环境）
        return "MOCK_SIGN_" + System.currentTimeMillis();
    }
    
    /**
     * 生成支付宝支付参数（模拟）
     * 实际项目中需要调用支付宝SDK
     */
    public Map<String, String> generateAlipayParams(RechargeOrder order) {
        Map<String, String> params = new HashMap<>();
        params.put("orderString", "alipay_sdk=java&app_id=xxx&biz_content={\"out_trade_no\":\"" + order.getId() + "\",\"total_amount\":\"" + (order.getAmount() / 100.0) + "\"}");
        return params;
    }
    
    /**
     * 生成银联支付参数（模拟）
     * 实际项目中需要调用银联SDK
     */
    public Map<String, String> generateUnionpayParams(RechargeOrder order) {
        Map<String, String> params = new HashMap<>();
        params.put("tn", "MOCK_TN_" + order.getId());
        params.put("mode", "00"); // 正式环境
        return params;
    }
    
    /**
     * 模拟支付成功（测试用）
     */
    public Map<String, Object> mockPaySuccess(String orderId) {
        RechargeOrder order = orderStore.get(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        
        return handlePaymentCallback(orderId, "MOCK_" + System.currentTimeMillis(), order.getPaymentMethod(), true);
    }
}
