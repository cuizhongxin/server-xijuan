package com.tencent.wxcloudrun.controller.signin;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.signin.SignInService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/sign-in")
public class SignInController {

    private static final Logger logger = LoggerFactory.getLogger(SignInController.class);

    @Autowired
    private SignInService signInService;

    /**
     * 获取签到信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getSignInInfo(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(signInService.getSignInInfo(userId));
    }

    /**
     * 每日签到
     */
    @PostMapping("/sign")
    public ApiResponse<Map<String, Object>> doSignIn(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(signInService.doSignIn(userId));
    }

    /**
     * 补签
     */
    @PostMapping("/makeup")
    public ApiResponse<Map<String, Object>> doMakeupSign(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        String userId = getUserId(request);
        int day;
        Object dayObj = body.get("day");
        if (dayObj instanceof Number) {
            day = ((Number) dayObj).intValue();
        } else {
            day = Integer.parseInt(String.valueOf(dayObj));
        }
        return ApiResponse.success(signInService.doMakeupSign(userId, day));
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
}
