package com.tencent.wxcloudrun.controller.alliance;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.AllianceWar;
import com.tencent.wxcloudrun.model.AllianceWar.WarBattle;
import com.tencent.wxcloudrun.model.AllianceWar.WarParticipant;
import com.tencent.wxcloudrun.service.alliance.AllianceWarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
    
    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
    
    /**
     * 获取盟战状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getWarStatus(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            Map<String, Object> status = allianceWarService.getWarStatus(userId);
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取战争状态异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 报名参战
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(request);
            String playerName = (String) body.get("playerName");
            Integer level = body.get("level") != null ? ((Number) body.get("level")).intValue() : 1;
            Long power = body.get("power") != null ? ((Number) body.get("power")).longValue() : 10000L;
            
            WarParticipant participant = allianceWarService.register(userId, playerName, level, power);
            
            Map<String, Object> data = new HashMap<>();
            data.put("participant", participant);
            data.put("message", "报名成功，您的编号是: " + participant.getPlayerNumber());
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("报名联盟战异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 取消报名
     */
    @PostMapping("/cancel")
    public ApiResponse<Map<String, Object>> cancelRegister(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            allianceWarService.cancelRegister(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "已取消报名");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("取消报名异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取参战玩家列表
     */
    @GetMapping("/participants")
    public ApiResponse<Map<String, Object>> getParticipants() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("participants", allianceWarService.getParticipants());
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取参战玩家列表异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取对战记录
     */
    @GetMapping("/battles")
    public ApiResponse<Map<String, Object>> getBattles(HttpServletRequest request) {
        try {
            String userId = getUserId(request);
            Map<String, Object> data = new HashMap<>();
            data.put("myBattles", allianceWarService.getBattleHistory(userId));
            data.put("allBattles", allianceWarService.getAllBattles());
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取对战记录异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取排名
     */
    @GetMapping("/rankings")
    public ApiResponse<Map<String, Object>> getRankings() {
        try {
            AllianceWar war = allianceWarService.getTodayWar();
            Map<String, Object> data = new HashMap<>();
            data.put("allianceRanks", war.getAllianceRanks());
            data.put("playerRanks", war.getPlayerRanks());
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取排名异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 手动触发报名（测试用）
     */
    @PostMapping("/trigger-registration")
    public ApiResponse<Map<String, Object>> triggerRegistration() {
        try {
            allianceWarService.triggerWarStart();
            Map<String, Object> data = new HashMap<>();
            data.put("message", "已开启报名");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("触发报名异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 手动触发战斗（测试用）
     */
    @PostMapping("/trigger-battle")
    public ApiResponse<Map<String, Object>> triggerBattle() {
        try {
            allianceWarService.triggerBattleStart();
            Map<String, Object> data = new HashMap<>();
            data.put("message", "战斗已开始");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("触发战斗异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 重置盟战（测试用）
     */
    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> resetWar() {
        try {
            allianceWarService.resetWar();
            Map<String, Object> data = new HashMap<>();
            data.put("message", "盟战已重置");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("重置盟战异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/battle-detail/{battleId}")
    public ApiResponse<Map<String, Object>> getBattleDetail(@PathVariable String battleId) {
        try {
            java.util.List<WarBattle> battles = allianceWarService.getAllBattles();
            WarBattle found = null;
            for (WarBattle b : battles) {
                if (battleId.equals(b.getId())) { found = b; break; }
            }
            if (found == null) return ApiResponse.error("战报不存在");
            Map<String, Object> result = new HashMap<>();
            result.put("battle", found);
            if (found.getBattleReportJson() != null) {
                result.put("battleReport", JSON.parseObject(found.getBattleReportJson()));
            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取盟战战报异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
