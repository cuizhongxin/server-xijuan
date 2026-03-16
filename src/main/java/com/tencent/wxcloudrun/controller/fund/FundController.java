package com.tencent.wxcloudrun.controller.fund;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.fund.FundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/fund")
public class FundController {

    private static final Logger logger = LoggerFactory.getLogger(FundController.class);

    @Autowired
    private FundService fundService;

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        try {
            return ApiResponse.success(fundService.getInfo(getUserId(request)));
        } catch (Exception e) {
            logger.error("获取基金信息异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/purchase")
    public ApiResponse<Map<String, Object>> purchase(HttpServletRequest request) {
        try {
            return ApiResponse.success(fundService.purchase(getUserId(request)));
        } catch (Exception e) {
            logger.error("购买基金异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/claim")
    public ApiResponse<Map<String, Object>> claim(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        try {
            int rewardLevel;
            Object levelObj = body.get("rewardId");
            if (levelObj == null) levelObj = body.get("rewardLevel");
            if (levelObj instanceof Number) {
                rewardLevel = ((Number) levelObj).intValue();
            } else {
                rewardLevel = Integer.parseInt(String.valueOf(levelObj));
            }
            return ApiResponse.success(fundService.claim(getUserId(request), rewardLevel));
        } catch (Exception e) {
            logger.error("领取基金奖励异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
