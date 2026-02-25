package com.tencent.wxcloudrun.controller.market;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.market.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    @GetMapping("/browse")
    public ApiResponse<Map<String, Object>> browse(HttpServletRequest request,
                                                    @RequestParam(defaultValue = "all") String itemType,
                                                    @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.success(marketService.browse(itemType, page));
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myListings(HttpServletRequest request) {
        return ApiResponse.success(marketService.myListings(getUserId(request)));
    }

    @PostMapping("/list-equipment")
    public ApiResponse<Map<String, Object>> listEquipment(HttpServletRequest request,
                                                           @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        String equipmentId = (String) body.get("equipmentId");
        long price = body.get("price") instanceof Number ? ((Number) body.get("price")).longValue() : 0;
        return ApiResponse.success(marketService.listEquipment(userId, equipmentId, price));
    }

    @PostMapping("/list-item")
    public ApiResponse<Map<String, Object>> listItem(HttpServletRequest request,
                                                      @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        String itemId = (String) body.get("itemId");
        int count = body.get("count") instanceof Number ? ((Number) body.get("count")).intValue() : 1;
        long price = body.get("price") instanceof Number ? ((Number) body.get("price")).longValue() : 0;
        return ApiResponse.success(marketService.listItem(userId, itemId, count, price));
    }

    @PostMapping("/buy")
    public ApiResponse<Map<String, Object>> buy(HttpServletRequest request,
                                                 @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        long listingId = body.get("listingId") instanceof Number ? ((Number) body.get("listingId")).longValue() : 0;
        return ApiResponse.success(marketService.buy(userId, listingId));
    }

    @PostMapping("/cancel")
    public ApiResponse<Map<String, Object>> cancel(HttpServletRequest request,
                                                    @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        long listingId = body.get("listingId") instanceof Number ? ((Number) body.get("listingId")).longValue() : 0;
        return ApiResponse.success(marketService.cancel(userId, listingId));
    }

    @GetMapping("/logs")
    public ApiResponse<List<Map<String, Object>>> tradeLogs(HttpServletRequest request) {
        return ApiResponse.success(marketService.getTradeLogs(getUserId(request)));
    }

    private String getUserId(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        return userIdLong != null ? String.valueOf(userIdLong) : null;
    }
}
