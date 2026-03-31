package com.tencent.wxcloudrun.controller.nationwar;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.NationWar;
import com.tencent.wxcloudrun.model.NationWar.NationWarSession;
import com.tencent.wxcloudrun.model.NationWar.WarBattle;
import com.tencent.wxcloudrun.service.PlayerNameResolver;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/nationwar")
public class NationWarController {

    @Autowired private NationWarService nationWarService;
    @Autowired private PlayerNameResolver playerNameResolver;

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

        Calendar cal = Calendar.getInstance();
        int nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        int signUpStart = 19 * 60 + 45;
        int signUpEnd = 20 * 60;
        int battleEnd = 20 * 60 + 40;

        String timeStatus;
        if (nowMinutes >= signUpStart && nowMinutes < signUpEnd) {
            timeStatus = "signup";
        } else if (nowMinutes >= signUpEnd && nowMinutes < battleEnd) {
            timeStatus = "fighting";
        } else if (nowMinutes >= battleEnd) {
            timeStatus = "finished";
        } else {
            timeStatus = "preparing";
        }
        result.put("timeStatus", timeStatus);
        result.put("signUpTime", "19:45 - 20:00");
        result.put("battleTime", "20:00 - 20:40");

        NationWarSession session = nationWarService.getActiveSession();
        if (session != null) {
            result.put("sessionPhase", session.getPhase().name());
            result.put("nationTargets", session.getNationTargets());
            result.put("currentRound", session.getCurrentRound());
        }

        return ApiResponse.success(result);
    }

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

    @GetMapping("/nations")
    public ApiResponse<Map<String, Object>> getNations(HttpServletRequest request) {
        List<NationWar.Nation> nations = nationWarService.getPlayerSelectableNations();
        Map<String, Object> result = new HashMap<>();
        result.put("nations", nations);
        return ApiResponse.success(result);
    }

    @PostMapping("/select-nation")
    public ApiResponse<Map<String, Object>> selectNation(HttpServletRequest request,
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

    @GetMapping("/can-change-nation")
    public ApiResponse<Map<String, Object>> canChangeNation(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(nationWarService.checkCanChangeNation(odUserId));
    }

    @PostMapping("/change-nation")
    public ApiResponse<Map<String, Object>> changeNation(HttpServletRequest request,
                                                          @RequestBody Map<String, String> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(nationWarService.changeNation(odUserId, body.get("nationId")));
    }

    @PostMapping("/signup")
    public ApiResponse<Map<String, Object>> signUp(HttpServletRequest request,
                                                    @RequestBody Map<String, Object> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        String playerName = playerNameResolver.resolve(odUserId);
        Integer level = body.get("level") != null ? ((Number) body.get("level")).intValue() : null;
        Integer power = body.get("power") != null ? ((Number) body.get("power")).intValue() : null;
        String targetCityId = (String) body.get("targetCityId");
        return ApiResponse.success(nationWarService.signUp(odUserId, playerName, level, power, targetCityId));
    }

    // ==================== 新接口: 加入城市战场 ====================

    @PostMapping("/join-city")
    public ApiResponse<Map<String, Object>> joinCity(HttpServletRequest request,
                                                      @RequestBody Map<String, Object> body) {
        try {
            String odUserId = String.valueOf(request.getAttribute("userId"));
            String playerName = playerNameResolver.resolve(odUserId);
            String cityId = (String) body.get("cityId");
            Integer level = body.get("level") != null ? ((Number) body.get("level")).intValue() : 30;
            Integer power = body.get("power") != null ? ((Number) body.get("power")).intValue() : 5000;
            return ApiResponse.success(nationWarService.joinCity(odUserId, playerName, level, power, cityId));
        } catch (Exception e) {
            log.error("加入城市战场异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 新接口: 切换城市 ====================

    @PostMapping("/switch-city")
    public ApiResponse<Map<String, Object>> switchCity(HttpServletRequest request,
                                                        @RequestBody Map<String, Object> body) {
        try {
            String odUserId = String.valueOf(request.getAttribute("userId"));
            String newCityId = (String) body.get("cityId");
            return ApiResponse.success(nationWarService.switchCity(odUserId, newCityId));
        } catch (Exception e) {
            log.error("切换城市异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 新接口: 战斗状态轮询 ====================

    @GetMapping("/battle-state")
    public ApiResponse<Map<String, Object>> getBattleState(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(nationWarService.getBattleState(odUserId));
    }

    // ==================== 新接口: 会话总览 ====================

    @GetMapping("/session")
    public ApiResponse<Map<String, Object>> getSession(HttpServletRequest request) {
        return ApiResponse.success(nationWarService.getSessionOverview());
    }

    // ==================== 原有接口 ====================

    @GetMapping("/city/{cityId}/status")
    public ApiResponse<Map<String, Object>> getCityWarStatus(HttpServletRequest request,
                                                              @PathVariable String cityId) {
        NationWar war = nationWarService.getTodayWar(cityId);
        Map<String, Object> result = new HashMap<>();
        if (war != null) {
            result.put("hasWar", true);
            result.put("war", war);
            result.put("attackerCount", war.getAttackers() != null ? war.getAttackers().size() : 0);
            result.put("defenderCount", war.getDefenders() != null ? war.getDefenders().size() : 0);
        } else {
            result.put("hasWar", false);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/war/{warId}")
    public ApiResponse<Map<String, Object>> getWarDetail(HttpServletRequest request,
                                                          @PathVariable String warId) {
        return ApiResponse.success(nationWarService.getWarStatus(warId));
    }

    @GetMapping("/active")
    public ApiResponse<Map<String, Object>> getActiveWars(HttpServletRequest request) {
        List<NationWar> wars = nationWarService.getActiveWars();
        Map<String, Object> result = new HashMap<>();
        result.put("wars", wars);
        result.put("count", wars.size());
        return ApiResponse.success(result);
    }

    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> getWarHistory(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "20") int limit) {
        List<NationWar> history = nationWarService.getWarHistory(limit);
        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("count", history.size());
        return ApiResponse.success(result);
    }

    @GetMapping("/merit")
    public ApiResponse<Map<String, Object>> getPlayerMerit(HttpServletRequest request) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        int merit = nationWarService.getPlayerMerit(odUserId);
        String playerNation = nationWarService.getPlayerNation(odUserId);
        NationWar.WarMap map = nationWarService.getWarMap();
        double exchangeRate = 1.0;
        if (playerNation != null) {
            exchangeRate = map.getNations().stream()
                    .filter(n -> n.getId().equals(playerNation)).findFirst()
                    .map(NationWar.Nation::getMeritExchangeRate).orElse(1.0);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("merit", merit);
        result.put("nation", playerNation);
        result.put("exchangeRate", exchangeRate);
        result.put("silverPerMerit", (int)(10 * exchangeRate));
        return ApiResponse.success(result);
    }

    @PostMapping("/exchange")
    public ApiResponse<Map<String, Object>> exchangeMerit(HttpServletRequest request,
                                                           @RequestBody Map<String, Integer> body) {
        String odUserId = String.valueOf(request.getAttribute("userId"));
        Integer meritAmount = body.get("meritAmount");
        if (meritAmount == null || meritAmount <= 0) return ApiResponse.error(400, "兑换数量无效");
        return ApiResponse.success(nationWarService.exchangeMerit(odUserId, meritAmount));
    }

    // ==================== 测试接口 ====================

    @PostMapping("/start/{warId}")
    public ApiResponse<Map<String, Object>> startWar(HttpServletRequest request,
                                                      @PathVariable String warId) {
        nationWarService.startWarBattle(warId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "国战已开始");
        return ApiResponse.success(result);
    }

    @PostMapping("/quick-test")
    public ApiResponse<Map<String, Object>> quickTest(HttpServletRequest request,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        try {
            String odUserId = String.valueOf(request.getAttribute("userId"));
            String playerName = playerNameResolver.resolve(odUserId);
            String targetCityId = (body != null && body.get("targetCityId") != null) ? (String) body.get("targetCityId") : null;
            int npcCount = (body != null && body.get("npcCount") != null) ? ((Number) body.get("npcCount")).intValue() : 5;
            return ApiResponse.success(nationWarService.quickTest(odUserId, playerName, targetCityId, npcCount));
        } catch (Exception e) {
            log.error("国战一键测试异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/force-signup/{cityId}")
    public ApiResponse<Map<String, Object>> forceSignUp(HttpServletRequest request,
                                                         @PathVariable String cityId) {
        try {
            String odUserId = String.valueOf(request.getAttribute("userId"));
            String playerName = playerNameResolver.resolve(odUserId);
            return ApiResponse.success(nationWarService.signUp(odUserId, playerName, 50, 20000, cityId));
        } catch (Exception e) {
            log.error("强制报名异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/reset-map")
    public ApiResponse<Map<String, Object>> resetMap() {
        try {
            nationWarService.resetMap();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "国战地图已重置为初始状态");
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("重置国战地图异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/start-registration")
    public ApiResponse<Map<String, Object>> startRegistration() {
        try {
            nationWarService.startRegistration();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "已手动开启国战报名");
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("手动开启报名异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/force-start-battle")
    public ApiResponse<Map<String, Object>> forceStartBattle() {
        try {
            nationWarService.finalizeRegistrationAndStartBattle();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "已手动截止报名并开战");
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("手动开战异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/force-tick")
    public ApiResponse<Map<String, Object>> forceTick() {
        try {
            nationWarService.battleTick();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "已手动执行一轮结算");
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("手动结算异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/force-finish")
    public ApiResponse<Map<String, Object>> forceFinish() {
        try {
            nationWarService.finishAllBattles();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "已手动结束国战");
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("手动结束异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/battle-detail/{warId}/{battleId}")
    public ApiResponse<Map<String, Object>> getBattleDetail(@PathVariable String warId,
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
