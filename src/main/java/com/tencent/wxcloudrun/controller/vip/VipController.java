package com.tencent.wxcloudrun.controller.vip;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.vip.VipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/vip")
public class VipController {

    @Autowired
    private VipService vipService;

    /** 获取VIP信息（等级、礼包列表、领取状态） */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(vipService.getVipInfo(userId));
    }

    /** 领取VIP礼包 */
    @PostMapping("/claim")
    public ApiResponse<Map<String, Object>> claim(HttpServletRequest request,
                                                    @RequestBody Map<String, Object> body) {
        String userId = String.valueOf(request.getAttribute("userId"));
        int level = ((Number) body.get("level")).intValue();
        return ApiResponse.success(vipService.claimGift(userId, level));
    }

    /** 开启宝箱 */
    @PostMapping("/openChest")
    public ApiResponse<Map<String, Object>> openChest(HttpServletRequest request,
                                                       @RequestBody Map<String, Object> body) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String chestItemId = (String) body.get("chestItemId");
        return ApiResponse.success(vipService.openChest(userId, chestItemId));
    }

    /** 自选套装部件 */
    @PostMapping("/selectEquipment")
    public ApiResponse<Map<String, Object>> selectEquipment(HttpServletRequest request,
                                                              @RequestBody Map<String, Object> body) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String setName = (String) body.get("setName");
        String partName = (String) body.get("partName");
        return ApiResponse.success(vipService.selectEquipment(userId, setName, partName));
    }
}
