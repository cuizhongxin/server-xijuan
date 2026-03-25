package com.tencent.wxcloudrun.controller.alliance;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.alliance.AllianceBossService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alliance-boss")
public class AllianceBossController {

    private static final Logger logger = LoggerFactory.getLogger(AllianceBossController.class);

    @Autowired
    private AllianceBossService allianceBossService;

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            Map<String, Object> data = allianceBossService.getInfo(userId);
            return ApiResponse.success(data);
        } catch (Exception e) {
            logger.error("获取联盟Boss信息异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/feed")
    public ApiResponse<Map<String, Object>> feed(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            int amount = 1;
            if (body.containsKey("amount")) {
                amount = ((Number) body.get("amount")).intValue();
            }
            return ApiResponse.success(allianceBossService.feed(userId, amount));
        } catch (Exception e) {
            logger.error("投喂联盟Boss异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/feed-equip")
    @SuppressWarnings("unchecked")
    public ApiResponse<Map<String, Object>> feedWithEquipment(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            List<String> equipmentIds = (List<String>) body.get("equipmentIds");
            return ApiResponse.success(allianceBossService.feedWithEquipment(userId, equipmentIds));
        } catch (Exception e) {
            logger.error("装备喂养联盟Boss异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/call")
    public ApiResponse<Map<String, Object>> call(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(allianceBossService.call(userId));
        } catch (Exception e) {
            logger.error("召唤联盟Boss异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/attack")
    public ApiResponse<Map<String, Object>> attack(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            return ApiResponse.success(allianceBossService.attack(userId));
        } catch (Exception e) {
            logger.error("攻击联盟Boss异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> getRecords(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            Map<String, Object> data = new HashMap<>();
            data.put("records", allianceBossService.getRecords(userId));
            return ApiResponse.success(data);
        } catch (Exception e) {
            logger.error("获取联盟Boss记录异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/rankings")
    public ApiResponse<Map<String, Object>> getRankings(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            Map<String, Object> data = new HashMap<>();
            data.put("rankings", allianceBossService.getRankings(userId));
            return ApiResponse.success(data);
        } catch (Exception e) {
            logger.error("获取联盟Boss排行异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
