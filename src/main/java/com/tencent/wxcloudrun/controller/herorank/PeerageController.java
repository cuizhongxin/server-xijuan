package com.tencent.wxcloudrun.controller.herorank;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.herorank.PeerageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/peerage")
public class PeerageController {

    @Autowired
    private PeerageService peerageService;

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        String userId = getUserId(request);
        return ApiResponse.success(peerageService.getPeerageInfo(userId));
    }

    @PostMapping("/upgrade-soldier")
    public ApiResponse<Map<String, Object>> upgradeSoldier(HttpServletRequest request,
                                                            @RequestBody Map<String, Object> body) {
        String userId = getUserId(request);
        String generalId = (String) body.get("generalId");
        int targetTier = body.get("targetTier") instanceof Number
                ? ((Number) body.get("targetTier")).intValue() : 1;
        String troopCategory = (String) body.get("troopCategory");
        return ApiResponse.success(peerageService.upgradeSoldierTier(userId, generalId, targetTier, troopCategory));
    }

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
}
