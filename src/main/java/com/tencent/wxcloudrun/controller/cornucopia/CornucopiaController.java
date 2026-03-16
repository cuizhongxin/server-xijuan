package com.tencent.wxcloudrun.controller.cornucopia;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.cornucopia.CornucopiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/cornucopia")
public class CornucopiaController {

    @Autowired
    private CornucopiaService cornucopiaService;

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        return ApiResponse.success(cornucopiaService.getInfo(getUserId(request)));
    }

    @PostMapping("/draw")
    public ApiResponse<Map<String, Object>> draw(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        int count = 1;
        if (body.containsKey("count")) {
            Object countObj = body.get("count");
            if (countObj instanceof Number) {
                count = ((Number) countObj).intValue();
            } else {
                count = Integer.parseInt(String.valueOf(countObj));
            }
        }
        return ApiResponse.success(cornucopiaService.draw(getUserId(request), count));
    }
}
