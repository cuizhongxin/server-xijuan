package com.tencent.wxcloudrun.controller.nationwar;

import com.tencent.wxcloudrun.model.NationWar;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 国战控制器
 */
@RestController
@RequestMapping("/api/nationwar")
public class NationWarController {
    
    @Autowired
    private NationWarService nationWarService;
    
    /**
     * 获取国战地图
     */
    @GetMapping("/map")
    public ResponseEntity<?> getWarMap(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        
        NationWar.WarMap map = nationWarService.getWarMap();
        String playerNation = nationWarService.getPlayerNation(odUserId);
        int playerMerit = nationWarService.getPlayerMerit(odUserId);
        List<NationWar.City> attackableCities = nationWarService.getAttackableCities(odUserId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("map", map);
        result.put("playerNation", playerNation);
        result.put("playerMerit", playerMerit);
        result.put("attackableCities", attackableCities);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查是否已选择国家
     */
    @GetMapping("/has-nation")
    public ResponseEntity<?> hasNation(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        
        boolean hasNation = nationWarService.hasSelectedNation(odUserId);
        String nation = nationWarService.getPlayerNation(odUserId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("hasNation", hasNation);
        result.put("nation", nation);
        
        if (nation != null) {
            NationWar.Nation nationInfo = nationWarService.getNation(nation);
            if (nationInfo != null) {
                result.put("nationName", nationInfo.getName());
                result.put("nationColor", nationInfo.getColor());
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取所有国家列表（用于选择）
     */
    @GetMapping("/nations")
    public ResponseEntity<?> getNations(HttpServletRequest request) {
        List<NationWar.Nation> nations = nationWarService.getAllNations();
        
        Map<String, Object> result = new HashMap<>();
        result.put("nations", nations);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 选择国家
     */
    @PostMapping("/select-nation")
    public ResponseEntity<?> selectNation(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String nationId = body.get("nationId");
        
        nationWarService.setPlayerNation(odUserId, nationId);
        
        NationWar.Nation nation = nationWarService.getNation(nationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("nation", nationId);
        result.put("nationName", nation != null ? nation.getName() : nationId);
        result.put("message", "选择国家成功，欢迎加入" + (nation != null ? nation.getName() : nationId) + "国！");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查是否可以转国
     */
    @GetMapping("/can-change-nation")
    public ResponseEntity<?> canChangeNation(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        
        Map<String, Object> result = nationWarService.checkCanChangeNation(odUserId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 转国
     */
    @PostMapping("/change-nation")
    public ResponseEntity<?> changeNation(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String newNationId = body.get("nationId");
        
        Map<String, Object> result = nationWarService.changeNation(odUserId, newNationId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 报名国战
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String playerName = (String) body.get("playerName");
        Integer level = (Integer) body.get("level");
        Integer power = (Integer) body.get("power");
        String targetCityId = (String) body.get("targetCityId");
        
        Map<String, Object> result = nationWarService.signUp(
            odUserId, playerName, level, power, targetCityId);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取城市国战状态
     */
    @GetMapping("/city/{cityId}/status")
    public ResponseEntity<?> getCityWarStatus(
            HttpServletRequest request,
            @PathVariable String cityId) {
        NationWar war = nationWarService.getTodayWar(cityId);
        
        Map<String, Object> result = new HashMap<>();
        if (war != null) {
            result.put("hasWar", true);
            result.put("war", war);
            result.put("attackerCount", war.getAttackers().size());
            result.put("defenderCount", war.getDefenders().size());
        } else {
            result.put("hasWar", false);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取国战详情
     */
    @GetMapping("/war/{warId}")
    public ResponseEntity<?> getWarDetail(
            HttpServletRequest request,
            @PathVariable String warId) {
        Map<String, Object> result = nationWarService.getWarStatus(warId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取活跃国战列表
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveWars(HttpServletRequest request) {
        List<NationWar> wars = nationWarService.getActiveWars();
        
        Map<String, Object> result = new HashMap<>();
        result.put("wars", wars);
        result.put("count", wars.size());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取国战历史
     */
    @GetMapping("/history")
    public ResponseEntity<?> getWarHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "20") int limit) {
        List<NationWar> history = nationWarService.getWarHistory(limit);
        
        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("count", history.size());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取玩家军功
     */
    @GetMapping("/merit")
    public ResponseEntity<?> getPlayerMerit(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        
        int merit = nationWarService.getPlayerMerit(odUserId);
        String playerNation = nationWarService.getPlayerNation(odUserId);
        
        // 获取兑换比例
        NationWar.WarMap map = nationWarService.getWarMap();
        double exchangeRate = 1.0;
        if (playerNation != null) {
            exchangeRate = map.getNations().stream()
                .filter(n -> n.getId().equals(playerNation))
                .findFirst()
                .map(NationWar.Nation::getMeritExchangeRate)
                .orElse(1.0);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("merit", merit);
        result.put("nation", playerNation);
        result.put("exchangeRate", exchangeRate);
        result.put("silverPerMerit", (int)(10 * exchangeRate));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 军功兑换白银
     */
    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeMerit(
            HttpServletRequest request,
            @RequestBody Map<String, Integer> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        Integer meritAmount = body.get("meritAmount");
        
        if (meritAmount == null || meritAmount <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "兑换数量无效");
            return ResponseEntity.badRequest().body(error);
        }
        
        Map<String, Object> result = nationWarService.exchangeMerit(odUserId, meritAmount);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动触发国战开始（测试用）
     */
    @PostMapping("/start/{warId}")
    public ResponseEntity<?> startWar(
            HttpServletRequest request,
            @PathVariable String warId) {
        nationWarService.startWarBattle(warId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "国战已开始");
        
        return ResponseEntity.ok(result);
    }
}
