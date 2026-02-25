package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.RechargeOrder;
import com.tencent.wxcloudrun.model.RechargeProduct;
import com.tencent.wxcloudrun.service.RechargeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值控制器
 */
@RestController
@RequestMapping("/recharge")
public class RechargeController {
    
    @Autowired
    private RechargeService rechargeService;
    
    /**
     * 获取充值商品列表
     */
    @GetMapping("/products")
    public ApiResponse<List<RechargeProduct>> getProducts() {
        List<RechargeProduct> products = rechargeService.getProducts();
        return ApiResponse.success(products);
    }
    
    /**
     * 创建充值订单
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createOrder(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String userId = String.valueOf(httpRequest.getAttribute("userId"));
        
        String productId = request.get("productId");
        String paymentMethod = request.get("paymentMethod");
        
        RechargeOrder order = rechargeService.createOrder(userId, productId, paymentMethod);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("amount", order.getAmount());
        result.put("productName", order.getProductName());
        
        // 根据支付方式生成支付参数
        switch (paymentMethod) {
            case RechargeOrder.PaymentMethod.WECHAT:
                result.put("payParams", rechargeService.generateWechatPayParams(order));
                break;
            case RechargeOrder.PaymentMethod.ALIPAY:
                result.put("payParams", rechargeService.generateAlipayParams(order));
                break;
            case RechargeOrder.PaymentMethod.UNIONPAY:
                result.put("payParams", rechargeService.generateUnionpayParams(order));
                break;
        }
        
        return ApiResponse.success(result);
    }
    
    /**
     * 查询订单状态
     */
    @GetMapping("/order/{orderId}")
    public ApiResponse<RechargeOrder> getOrder(@PathVariable String orderId) {
        RechargeOrder order = rechargeService.getOrder(orderId);
        return ApiResponse.success(order);
    }
    
    /**
     * 获取用户充值记录
     */
    @GetMapping("/orders")
    public ApiResponse<List<RechargeOrder>> getUserOrders(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        List<RechargeOrder> orders = rechargeService.getUserOrders(userId);
        return ApiResponse.success(orders);
    }
    
    /**
     * 微信支付回调
     * 实际项目中需要验证签名
     */
    @PostMapping("/callback/wechat")
    public String wechatCallback(@RequestBody Map<String, String> params) {
        String orderId = params.get("out_trade_no");
        String tradeNo = params.get("transaction_id");
        String resultCode = params.get("result_code");
        
        try {
            rechargeService.handleWechatCallback(orderId, tradeNo, "SUCCESS".equals(resultCode));
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";
        } catch (Exception e) {
            return "<xml><return_code><![CDATA[FAIL]]></return_code></xml>";
        }
    }
    
    /**
     * 支付宝回调
     * 实际项目中需要验证签名
     */
    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestParam Map<String, String> params) {
        String orderId = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        
        try {
            rechargeService.handleAlipayCallback(orderId, tradeNo, "TRADE_SUCCESS".equals(tradeStatus));
            return "success";
        } catch (Exception e) {
            return "fail";
        }
    }
    
    /**
     * 银联回调
     * 实际项目中需要验证签名
     */
    @PostMapping("/callback/unionpay")
    public String unionpayCallback(@RequestParam Map<String, String> params) {
        String orderId = params.get("orderId");
        String queryId = params.get("queryId");
        String respCode = params.get("respCode");
        
        try {
            rechargeService.handleUnionpayCallback(orderId, queryId, "00".equals(respCode));
            return "ok";
        } catch (Exception e) {
            return "fail";
        }
    }
    
    /**
     * 模拟支付成功（测试用）
     */
    @PostMapping("/mock/pay")
    public ApiResponse<Map<String, Object>> mockPay(@RequestBody Map<String, String> request) {
        String orderId = request.get("orderId");
        Map<String, Object> result = rechargeService.mockPaySuccess(orderId);
        return ApiResponse.success(result);
    }
}
