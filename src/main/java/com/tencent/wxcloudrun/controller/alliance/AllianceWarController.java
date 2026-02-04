package com.tencent.wxcloudrun.controller.alliance;

import com.tencent.wxcloudrun.model.AllianceWar;
import com.tencent.wxcloudrun.model.AllianceWar.WarParticipant;
import com.tencent.wxcloudrun.service.alliance.AllianceWarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 联盟战控制器
 */
@Slf4j
@RestController
@RequestMapping("/alliance-war")
@RequiredArgsConstructor
public class AllianceWarController {
    
    private final AllianceWarService allianceWarService;
    
    /**
     * 获取盟战状态
     */
    @GetMapping("/status")
    public Map<String, Object> getWarStatus(@RequestHeader("X-User-ID") String odUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> status = allianceWarService.getWarStatus(odUserId);
            result.put("success", true);
            result.putAll(status);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 报名参战
     */
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String playerName = (String) body.get("playerName");
            Integer level = body.get("level") != null ? ((Number) body.get("level")).intValue() : 1;
            Long power = body.get("power") != null ? ((Number) body.get("power")).longValue() : 10000L;
            
            WarParticipant participant = allianceWarService.register(odUserId, playerName, level, power);
            
            result.put("success", true);
            result.put("participant", participant);
            result.put("message", "报名成功，您的编号是: " + participant.getPlayerNumber());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 取消报名
     */
    @PostMapping("/cancel")
    public Map<String, Object> cancelRegister(@RequestHeader("X-User-ID") String odUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            allianceWarService.cancelRegister(odUserId);
            result.put("success", true);
            result.put("message", "已取消报名");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取参战玩家列表
     */
    @GetMapping("/participants")
    public Map<String, Object> getParticipants() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("participants", allianceWarService.getParticipants());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取对战记录
     */
    @GetMapping("/battles")
    public Map<String, Object> getBattles(@RequestHeader("X-User-ID") String odUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("myBattles", allianceWarService.getBattleHistory(odUserId));
            result.put("allBattles", allianceWarService.getAllBattles());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取排名
     */
    @GetMapping("/rankings")
    public Map<String, Object> getRankings() {
        Map<String, Object> result = new HashMap<>();
        try {
            AllianceWar war = allianceWarService.getTodayWar();
            result.put("success", true);
            result.put("allianceRanks", war.getAllianceRanks());
            result.put("playerRanks", war.getPlayerRanks());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 手动触发报名（测试用）
     */
    @PostMapping("/trigger-registration")
    public Map<String, Object> triggerRegistration() {
        Map<String, Object> result = new HashMap<>();
        try {
            allianceWarService.triggerWarStart();
            result.put("success", true);
            result.put("message", "已开启报名");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 手动触发战斗（测试用）
     */
    @PostMapping("/trigger-battle")
    public Map<String, Object> triggerBattle() {
        Map<String, Object> result = new HashMap<>();
        try {
            allianceWarService.triggerBattleStart();
            result.put("success", true);
            result.put("message", "战斗已开始");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 重置盟战（测试用）
     */
    @PostMapping("/reset")
    public Map<String, Object> resetWar() {
        Map<String, Object> result = new HashMap<>();
        try {
            allianceWarService.resetWar();
            result.put("success", true);
            result.put("message", "盟战已重置");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
