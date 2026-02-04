package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 充值订单模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOrder {
    
    private String id;              // 订单ID
    private String odUserId;        // 用户ID
    
    // 订单信息
    private Long amount;            // 充值金额（分）
    private String productId;       // 商品ID
    private String productName;     // 商品名称
    
    // 支付信息
    private String paymentMethod;   // 支付方式: WECHAT, ALIPAY, UNIONPAY
    private String tradeNo;         // 第三方交易号
    
    // 状态
    private String status;          // PENDING, PAID, FAILED, REFUNDED
    
    // 获得的道具
    private Long goldAmount;        // 获得黄金数量
    private Long diamondAmount;     // 获得钻石数量
    private String bonusItems;      // 额外赠送物品（JSON）
    
    // 时间戳
    private Long createTime;
    private Long payTime;
    private Long updateTime;
    
    /**
     * 订单状态枚举
     */
    public static class Status {
        public static final String PENDING = "PENDING";   // 待支付
        public static final String PAID = "PAID";         // 已支付
        public static final String FAILED = "FAILED";     // 支付失败
        public static final String REFUNDED = "REFUNDED"; // 已退款
    }
    
    /**
     * 支付方式枚举
     */
    public static class PaymentMethod {
        public static final String WECHAT = "WECHAT";     // 微信支付
        public static final String ALIPAY = "ALIPAY";     // 支付宝
        public static final String UNIONPAY = "UNIONPAY"; // 银联
    }
}
