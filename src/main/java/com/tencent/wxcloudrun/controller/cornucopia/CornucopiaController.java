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

    @PostMapping("/buy")
    public ApiResponse<Map<String, Object>> buy(@RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        int count = 1;
        if (body.containsKey("count")) {
            Object c = body.get("count");
            count = c instanceof Number ? ((Number) c).intValue() : Integer.parseInt(String.valueOf(c));
        }
        return ApiResponse.success(cornucopiaService.buyTicket(getUserId(request), count));
    }

    @PostMapping("/draw")
    public ApiResponse<Map<String, Object>> draw(HttpServletRequest request) {
        String userId = getUserId(request);
        int serverId = 1;
        if (userId != null && userId.contains("_")) {
            try { serverId = Integer.parseInt(userId.substring(userId.lastIndexOf('_') + 1)); } catch (Exception ignored) {}
        }
        return ApiResponse.success(cornucopiaService.executeDraw(serverId));
    }
}
