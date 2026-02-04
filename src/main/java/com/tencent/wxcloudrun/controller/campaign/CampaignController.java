package com.tencent.wxcloudrun.controller.campaign;

import com.tencent.wxcloudrun.model.CampaignProgress;
import com.tencent.wxcloudrun.service.campaign.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/campaign")
@RequiredArgsConstructor
public class CampaignController {
    
    private final CampaignService campaignService;
    
    /**
     * 获取战役列表
     */
    @GetMapping("/list")
    public Map<String, Object> getCampaignList(@RequestHeader("X-User-ID") String odUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> campaigns = campaignService.getCampaignList(odUserId);
            result.put("success", true);
            result.put("campaigns", campaigns);
        } catch (Exception e) {
            log.error("获取战役列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取战役详情
     */
    @GetMapping("/detail/{campaignId}")
    public Map<String, Object> getCampaignDetail(
            @RequestHeader("X-User-ID") String odUserId,
            @PathVariable String campaignId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> detail = campaignService.getCampaignDetail(odUserId, campaignId);
            result.put("success", true);
            result.putAll(detail);
        } catch (Exception e) {
            log.error("获取战役详情失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 开始战役
     */
    @PostMapping("/start")
    public Map<String, Object> startCampaign(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = body.get("campaignId");
            String generalId = body.get("generalId");
            Map<String, Object> startResult = campaignService.startCampaign(odUserId, campaignId, generalId);
            result.put("success", true);
            result.putAll(startResult);
        } catch (Exception e) {
            log.error("开始战役失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 进攻
     */
    @PostMapping("/attack")
    public Map<String, Object> attack(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = body.get("campaignId");
            CampaignProgress.BattleResult battleResult = campaignService.attack(odUserId, campaignId);
            result.put("success", true);
            result.put("battleResult", battleResult);
        } catch (Exception e) {
            log.error("进攻失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 补充兵力
     */
    @PostMapping("/replenish")
    public Map<String, Object> replenishTroops(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = body.get("campaignId");
            Map<String, Object> replenishResult = campaignService.replenishTroops(odUserId, campaignId);
            result.put("success", true);
            result.putAll(replenishResult);
        } catch (Exception e) {
            log.error("补充兵力失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 重生
     */
    @PostMapping("/revive")
    public Map<String, Object> revive(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = body.get("campaignId");
            Map<String, Object> reviveResult = campaignService.revive(odUserId, campaignId);
            result.put("success", true);
            result.putAll(reviveResult);
        } catch (Exception e) {
            log.error("重生失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 暂停战役
     */
    @PostMapping("/pause")
    public Map<String, Object> pauseCampaign(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = body.get("campaignId");
            Map<String, Object> pauseResult = campaignService.pauseCampaign(odUserId, campaignId);
            result.put("success", true);
            result.putAll(pauseResult);
        } catch (Exception e) {
            log.error("暂停战役失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 继续战役
     */
    @PostMapping("/resume")
    public Map<String, Object> resumeCampaign(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = body.get("campaignId");
            Map<String, Object> resumeResult = campaignService.resumeCampaign(odUserId, campaignId);
            result.put("success", true);
            result.putAll(resumeResult);
        } catch (Exception e) {
            log.error("继续战役失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 结束战役
     */
    @PostMapping("/end")
    public Map<String, Object> endCampaign(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = body.get("campaignId");
            Map<String, Object> endResult = campaignService.endCampaign(odUserId, campaignId);
            result.put("success", true);
            result.putAll(endResult);
        } catch (Exception e) {
            log.error("结束战役失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 扫荡
     */
    @PostMapping("/sweep")
    public Map<String, Object> sweep(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String campaignId = (String) body.get("campaignId");
            Integer targetStage = (Integer) body.get("targetStage");
            if (targetStage == null) targetStage = 7;
            
            CampaignProgress.SweepResult sweepResult = campaignService.sweep(odUserId, campaignId, targetStage);
            result.put("success", true);
            result.put("sweepResult", sweepResult);
        } catch (Exception e) {
            log.error("扫荡失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
