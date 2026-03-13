package com.tencent.wxcloudrun.controller.campaign;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.CampaignProgress;
import com.tencent.wxcloudrun.service.campaign.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/campaign")
@RequiredArgsConstructor
public class CampaignController {
    
    private final CampaignService campaignService;

    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
    
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> getCampaignList(HttpServletRequest request) {
        try {
            String odUserId = getUserId(request);
            List<Map<String, Object>> campaigns = campaignService.getCampaignList(odUserId);
            return ApiResponse.success(campaigns);
        } catch (Exception e) {
            log.error("获取战役列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/detail/{campaignId}")
    public ApiResponse<Map<String, Object>> getCampaignDetail(
            HttpServletRequest request,
            @PathVariable String campaignId) {
        try {
            String odUserId = getUserId(request);
            Map<String, Object> detail = campaignService.getCampaignDetail(odUserId, campaignId);
            return ApiResponse.success(detail);
        } catch (Exception e) {
            log.error("获取战役详情失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> startCampaign(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = body.get("campaignId");
            String generalId = body.get("generalId");
            Map<String, Object> startResult = campaignService.startCampaign(odUserId, campaignId, generalId);
            return ApiResponse.success(startResult);
        } catch (Exception e) {
            log.error("开始战役失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/attack")
    public ApiResponse<CampaignProgress.BattleResult> attack(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = (String) body.get("campaignId");
            Boolean victory = (Boolean) body.get("victory");
            Integer troopsLost = body.get("troopsLost") != null ? ((Number) body.get("troopsLost")).intValue() : null;
            Integer remainingTroops = body.get("remainingTroops") != null ? ((Number) body.get("remainingTroops")).intValue() : null;
            
            CampaignProgress.BattleResult battleResult;
            if (victory != null) {
                battleResult = campaignService.reportBattleResult(odUserId, campaignId, victory, troopsLost, remainingTroops);
            } else {
                battleResult = campaignService.attack(odUserId, campaignId);
            }
            return ApiResponse.success(battleResult);
        } catch (Exception e) {
            log.error("进攻失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/replenish")
    public ApiResponse<Map<String, Object>> replenishTroops(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = body.get("campaignId");
            Map<String, Object> replenishResult = campaignService.replenishTroops(odUserId, campaignId);
            return ApiResponse.success(replenishResult);
        } catch (Exception e) {
            log.error("补充兵力失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/revive")
    public ApiResponse<Map<String, Object>> revive(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = body.get("campaignId");
            Map<String, Object> reviveResult = campaignService.revive(odUserId, campaignId);
            return ApiResponse.success(reviveResult);
        } catch (Exception e) {
            log.error("重生失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/pause")
    public ApiResponse<Map<String, Object>> pauseCampaign(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = body.get("campaignId");
            Map<String, Object> pauseResult = campaignService.pauseCampaign(odUserId, campaignId);
            return ApiResponse.success(pauseResult);
        } catch (Exception e) {
            log.error("暂停战役失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/resume")
    public ApiResponse<Map<String, Object>> resumeCampaign(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = body.get("campaignId");
            Map<String, Object> resumeResult = campaignService.resumeCampaign(odUserId, campaignId);
            return ApiResponse.success(resumeResult);
        } catch (Exception e) {
            log.error("继续战役失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/end")
    public ApiResponse<Map<String, Object>> endCampaign(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = body.get("campaignId");
            Map<String, Object> endResult = campaignService.endCampaign(odUserId, campaignId);
            return ApiResponse.success(endResult);
        } catch (Exception e) {
            log.error("结束战役失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/sweep")
    public ApiResponse<CampaignProgress.SweepResult> sweep(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String campaignId = (String) body.get("campaignId");
            Integer targetStage = body.get("targetStage") != null ? ((Number) body.get("targetStage")).intValue() : 7;
            CampaignProgress.SweepResult sweepResult = campaignService.sweep(odUserId, campaignId, targetStage);
            return ApiResponse.success(sweepResult);
        } catch (Exception e) {
            log.error("扫荡失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
