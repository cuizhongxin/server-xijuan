package com.tencent.wxcloudrun.controller.supply;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.supply.SupplyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/supply")
public class SupplyController {

    private static final Logger logger = LoggerFactory.getLogger(SupplyController.class);

    @Autowired
    private SupplyService supplyService;

    private String getUserId(HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("userId");
        return uid != null ? String.valueOf(uid) : null;
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        return ApiResponse.success(supplyService.getSupplyInfo(getUserId(request)));
    }

    @PostMapping("/roll")
    public ApiResponse<Map<String, Object>> rollGrade(HttpServletRequest request) {
        return ApiResponse.success(supplyService.rollGrade(getUserId(request)));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refreshGrade(HttpServletRequest request) {
        return ApiResponse.success(supplyService.refreshGrade(getUserId(request)));
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> startTransport(HttpServletRequest request) {
        return ApiResponse.success(supplyService.startTransport(getUserId(request)));
    }

    @PostMapping("/speedup")
    public ApiResponse<Map<String, Object>> speedUp(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        int gold = body.get("gold") != null ? Integer.parseInt(String.valueOf(body.get("gold"))) : 0;
        return ApiResponse.success(supplyService.speedUp(getUserId(request), gold));
    }

    @PostMapping("/collect")
    public ApiResponse<Map<String, Object>> collect(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        long transportId = body.get("transportId") != null
                ? Long.parseLong(String.valueOf(body.get("transportId"))) : 0;
        return ApiResponse.success(supplyService.collectTransport(getUserId(request), transportId));
    }

    @GetMapping("/map")
    public ApiResponse<Map<String, Object>> getMap(HttpServletRequest request) {
        return ApiResponse.success(supplyService.getMapTransports(getUserId(request)));
    }

    @PostMapping("/rob")
    public ApiResponse<Map<String, Object>> rob(@RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        long transportId = body.get("transportId") != null
                ? Long.parseLong(String.valueOf(body.get("transportId"))) : 0;
        String generalId = (String) body.get("generalId");
        logger.info("抢夺军需, userId={}, transportId={}", getUserId(request), transportId);
        return ApiResponse.success(supplyService.robTransport(getUserId(request), transportId, generalId));
    }

    @PostMapping("/buy-token")
    public ApiResponse<Map<String, Object>> buyToken(@RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        int count = body.get("count") != null ? Integer.parseInt(String.valueOf(body.get("count"))) : 1;
        return ApiResponse.success(supplyService.buyToken(getUserId(request), count));
    }

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> getRecords(
            @RequestParam(defaultValue = "attack") String type,
            HttpServletRequest request) {
        return ApiResponse.success(supplyService.getRecords(getUserId(request), type));
    }
}
