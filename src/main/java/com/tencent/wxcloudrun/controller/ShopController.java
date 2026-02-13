package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.Shop;
import com.tencent.wxcloudrun.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商城控制器
 */
@RestController
@RequestMapping("/shop")
public class ShopController {
    
    @Autowired
    private ShopService shopService;
    
    /**
     * 获取商品列表
     * GET /shop/goods?tab=enhance
     */
    @GetMapping("/goods")
    public ApiResponse<List<Shop>> getShopGoods(
            @RequestParam(required = false) String tab) {
        List<Shop> result = shopService.getShopGoods(tab);
        return ApiResponse.success(result);
    }
    
    /**
     * 购买商品
     * POST /shop/buy
     * body: { goodsId: 1, count: 1 }
     */
    @PostMapping("/buy")
    public ApiResponse<Map<String, Object>> buyGoods(
            @RequestAttribute("userId") Long userIdLong,
            @RequestBody Map<String, Object> request) {
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        // 解析参数
        Long goodsId;
        Object goodsIdObj = request.get("goodsId");
        if (goodsIdObj instanceof Number) {
            goodsId = ((Number) goodsIdObj).longValue();
        } else {
            goodsId = Long.parseLong(String.valueOf(goodsIdObj));
        }
        
        int count = 1;
        if (request.containsKey("count")) {
            Object countObj = request.get("count");
            if (countObj instanceof Number) {
                count = ((Number) countObj).intValue();
            } else {
                count = Integer.parseInt(String.valueOf(countObj));
            }
        }
        
        Map<String, Object> result = shopService.buyGoods(userId, goodsId, count);
        return ApiResponse.success(result);
    }
}
