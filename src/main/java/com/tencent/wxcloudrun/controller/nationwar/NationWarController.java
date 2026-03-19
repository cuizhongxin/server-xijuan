package com.tencent.wxcloudrun.controller.nationwar;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.NationWar;
import com.tencent.wxcloudrun.model.NationWar.WarBattle;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 国战控制器
 */
@RestController
@RequestMapping("/nationwar")
public class NationWarController {
    
    @Autowired
    private NationWarService nationWarService;
    
    /**
     * 获取国战地图
     */
    @GetMapping("/map")
    public ApiResponse<Map<String, Object>> getWarMap(HttpServletRequest request) {
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
        
        return ApiResponse.success(result);
    }
    
    /**
     * 检查是否已选择国家
     */
    @GetMapping("/has-nation")
    public ApiResponse<Map<String, Object>> hasNation(HttpServletRequest request) {
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
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取玩家可选国家列表（仅魏、蜀、吴；汉/群雄为NPC不在此列）
     */
    @GetMapping("/nations")
    public ApiResponse<Map<String, Object>> getNations(HttpServletRequest request) {
        List<NationWar.Nation> nations = nationWarService.getPlayerSelectableNations();
        
        Map<String, Object> result = new HashMap<>();
        result.put("nations", nations);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 选择国家
     */
    @PostMapping("/select-nation")
    public ApiResponse<Map<String, Object>> selectNation(
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
        
        return ApiResponse.success(result);
    }
    
    /**
     * 检查是否可以转国
     */
    @GetMapping("/can-change-nation")
    public ApiResponse<Map<String, Object>> canChangeNation(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        
        Map<String, Object> result = nationWarService.checkCanChangeNation(odUserId);
        return ApiResponse.success(result);
    }
    
    /**
     * 转国
     */
    @PostMapping("/change-nation")
    public ApiResponse<Map<String, Object>> changeNation(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String newNationId = body.get("nationId");
        
        Map<String, Object> result = nationWarService.changeNation(odUserId, newNationId);
        return ApiResponse.success(result);
    }
    
    /**
     * 报名国战
     */
    @PostMapping("/signup")
    public ApiResponse<Map<String, Object>> signUp(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String playerName = (String) body.get("playerName");
        Integer level = (Integer) body.get("level");
        Integer power = (Integer) body.get("power");
        String targetCityId = (String) body.get("targetCityId");
        
        Map<String, Object> result = nationWarService.signUp(
            odUserId, playerName, level, power, targetCityId);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取城市国战状态
     */
    @GetMapping("/city/{cityId}/status")
    public ApiResponse<Map<String, Object>> getCityWarStatus(
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
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取国战详情
     */
    @GetMapping("/war/{warId}")
    public ApiResponse<Map<String, Object>> getWarDetail(
            HttpServletRequest request,
            @PathVariable String warId) {
        Map<String, Object> result = nationWarService.getWarStatus(warId);
        return ApiResponse.success(result);
    }
    
    /**
     * 获取活跃国战列表
     */
    @GetMapping("/active")
    public ApiResponse<Map<String, Object>> getActiveWars(HttpServletRequest request) {
        List<NationWar> wars = nationWarService.getActiveWars();
        
        Map<String, Object> result = new HashMap<>();
        result.put("wars", wars);
        result.put("count", wars.size());
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取国战历史
     */
    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> getWarHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "20") int limit) {
        List<NationWar> history = nationWarService.getWarHistory(limit);
        
        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("count", history.size());
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取玩家军功
     */
    @GetMapping("/merit")
    public ApiResponse<Map<String, Object>> getPlayerMerit(HttpServletRequest request) {
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
        
        return ApiResponse.success(result);
    }
    
    /**
     * 军功兑换白银
     */
    @PostMapping("/exchange")
    public ApiResponse<Map<String, Object>> exchangeMerit(
            HttpServletRequest request,
            @RequestBody Map<String, Integer> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        Integer meritAmount = body.get("meritAmount");
        
        if (meritAmount == null || meritAmount <= 0) {
            return ApiResponse.error(400, "兑换数量无效");
        }
        
        Map<String, Object> result = nationWarService.exchangeMerit(odUserId, meritAmount);
        return ApiResponse.success(result);
    }
    
    /**
     * 手动触发国战开始（测试用）
     */
    @PostMapping("/start/{warId}")
    public ApiResponse<Map<String, Object>> startWar(
            HttpServletRequest request,
            @PathVariable String warId) {
        nationWarService.startWarBattle(warId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "国战已开始");
        
        return ApiResponse.success(result);
    }

    @GetMapping("/battle-detail/{warId}/{battleId}")
    public ApiResponse<Map<String, Object>> getBattleDetail(
            @PathVariable String warId,
            @PathVariable String battleId) {
        NationWar war = nationWarService.getTodayWar(warId.contains("_") ? warId.split("_", 2)[1] : warId);
        if (war == null) return ApiResponse.error("国战不存在");
        WarBattle found = null;
        if (war.getBattles() != null) {
            for (WarBattle b : war.getBattles()) {
                if (battleId.equals(b.getBattleId())) { found = b; break; }
            }
        }
        if (found == null) return ApiResponse.error("战报不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("battle", found);
        if (found.getBattleReportJson() != null) {
            result.put("battleReport", JSON.parseObject(found.getBattleReportJson()));
        }
        return ApiResponse.success(result);
    }
}
