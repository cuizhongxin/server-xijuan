package com.tencent.wxcloudrun.service;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.RechargeOrderMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.RechargeOrder;
import com.tencent.wxcloudrun.model.RechargeProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
        
        rechargeOrderMapper.upsert(orderId, odUserId, order.getStatus(), JSON.toJSONString(order), order.getCreateTime());
        logger.info("创建充值订单: {}, 用户: {}, 商品: {}, 支付方式: {}", orderId, odUserId, productId, paymentMethod);
        return order;
    }
    
    public RechargeOrder getOrder(String orderId) {
        String data = rechargeOrderMapper.findById(orderId);
        if (data == null) return null;
        return JSON.parseObject(data, RechargeOrder.class);
    }
    
    public List<RechargeOrder> getUserOrders(String odUserId) {
        List<Map<String, Object>> rows = rechargeOrderMapper.findByUserId(odUserId);
        List<RechargeOrder> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String data = (String) row.get("data");
                if (data != null) { result.add(JSON.parseObject(data, RechargeOrder.class)); }
            }
        }
        return result;
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
        RechargeOrder order = getOrder(orderId);
        if (order == null) { throw new BusinessException(400, "订单不存在"); }
        if (!RechargeOrder.Status.PENDING.equals(order.getStatus())) { throw new BusinessException(400, "订单状态异常"); }
        
        order.setTradeNo(tradeNo);
        order.setUpdateTime(System.currentTimeMillis());
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        
        if (success) {
            order.setStatus(RechargeOrder.Status.PAID);
            order.setPayTime(System.currentTimeMillis());
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
        
        rechargeOrderMapper.upsert(orderId, order.getOdUserId(), order.getStatus(), JSON.toJSONString(order), order.getCreateTime());
        return result;
    }
    
    public Map<String, Object> generateWechatPayParams(RechargeOrder order) {
        Map<String, Object> params = new HashMap<>();
        String appId = "wx8f7d0509e671ceb4";
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String prepayId = "wx" + System.currentTimeMillis();
        
        params.put("appId", appId); params.put("timeStamp", timeStamp); params.put("nonceStr", nonceStr);
        params.put("package", "prepay_id=" + prepayId); params.put("signType", "RSA");
        params.put("paySign", generatePaySign(appId, timeStamp, nonceStr, "prepay_id=" + prepayId));
        params.put("totalFee", order.getAmount()); params.put("outTradeNo", order.getId());
        params.put("total_fee", order.getAmount()); params.put("out_trade_no", order.getId());
        params.put("TOTAL_FEE", order.getAmount()); params.put("OUT_TRADE_NO", order.getId());
        params.put("orderId", order.getId()); params.put("body", order.getProductName()); params.put("amount", order.getAmount());
        return params;
    }
    
    private String generatePaySign(String appId, String timeStamp, String nonceStr, String packageVal) {
        return "MOCK_SIGN_" + System.currentTimeMillis();
    }
    
    public Map<String, String> generateAlipayParams(RechargeOrder order) {
        Map<String, String> params = new HashMap<>();
        params.put("orderString", "alipay_sdk=java&app_id=xxx&biz_content={\"out_trade_no\":\"" + order.getId() + "\",\"total_amount\":\"" + (order.getAmount() / 100.0) + "\"}");
        return params;
    }
    
    public Map<String, String> generateUnionpayParams(RechargeOrder order) {
        Map<String, String> params = new HashMap<>();
        params.put("tn", "MOCK_TN_" + order.getId()); params.put("mode", "00");
        return params;
    }
    
    public Map<String, Object> mockPaySuccess(String orderId) {
        RechargeOrder order = getOrder(orderId);
        if (order == null) { throw new BusinessException(400, "订单不存在"); }
        return handlePaymentCallback(orderId, "MOCK_" + System.currentTimeMillis(), order.getPaymentMethod(), true);
    }
}
