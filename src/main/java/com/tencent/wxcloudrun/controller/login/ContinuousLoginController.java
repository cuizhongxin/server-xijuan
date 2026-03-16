package com.tencent.wxcloudrun.controller.login;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.login.ContinuousLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/continuous-login")
public class ContinuousLoginController {

    @Autowired
    private ContinuousLoginService continuousLoginService;

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        return ApiResponse.success(continuousLoginService.getInfo(getUserId(request)));
    }

    @PostMapping("/checkin")
    public ApiResponse<Map<String, Object>> checkin(HttpServletRequest request) {
        return ApiResponse.success(continuousLoginService.checkin(getUserId(request)));
    }

    @PostMapping("/claim")
    public ApiResponse<Map<String, Object>> claim(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        int day;
        Object dayObj = body.get("day");
        if (dayObj instanceof Number) {
            day = ((Number) dayObj).intValue();
        } else {
            day = Integer.parseInt(String.valueOf(dayObj));
        }
        return ApiResponse.success(continuousLoginService.claim(getUserId(request), day));
    }
}
