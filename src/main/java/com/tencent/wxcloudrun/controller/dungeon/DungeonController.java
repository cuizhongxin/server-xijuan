package com.tencent.wxcloudrun.controller.dungeon;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.dto.DungeonRequest;
import com.tencent.wxcloudrun.model.Dungeon;
import com.tencent.wxcloudrun.model.DungeonProgress;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.dungeon.DungeonService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.level.LevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 副本控制器
 */
@RestController
@RequestMapping("/dungeon")
public class DungeonController {
    
    private static final Logger logger = LoggerFactory.getLogger(DungeonController.class);
    
    @Autowired
    private DungeonService dungeonService;
    
    @Autowired
    private GeneralRepository generalRepository;
    
    @Autowired
    private LevelService levelService;
    
    @Autowired
    private GeneralService generalService;
    
    /**
     * 获取所有副本
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Dungeon>> getAllDungeons(HttpServletRequest request) {
        logger.info("获取所有副本列表");
        
        Map<String, Dungeon> dungeons = dungeonService.getAllDungeons();
        
        return ApiResponse.success(dungeons);
    }
    
    /**
     * 获取已解锁的副本
     */
    @GetMapping("/unlocked")
    public ApiResponse<List<Dungeon>> getUnlockedDungeons(@RequestParam Integer playerLevel,
                                                         HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("获取已解锁副本, userId: {}, playerLevel: {}", userId, playerLevel);
        
        List<Dungeon> dungeons = dungeonService.getUnlockedDungeons(playerLevel);
        
        return ApiResponse.success(dungeons);
    }
    
    /**
     * 获取副本详情
     */
    @GetMapping("/{dungeonId}")
    public ApiResponse<Map<String, Object>> getDungeonDetail(@PathVariable String dungeonId,
                                                             HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("获取副本详情, userId: {}, dungeonId: {}", userId, dungeonId);
        
        Dungeon dungeon = dungeonService.getDungeonDetail(dungeonId);
        DungeonProgress progress = dungeonService.getUserProgress(userId, dungeonId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("dungeon", dungeon);
        result.put("progress", progress);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取用户副本进度
     */
    @GetMapping("/progress/{dungeonId}")
    public ApiResponse<DungeonProgress> getUserProgress(@PathVariable String dungeonId,
                                                        HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("获取用户副本进度, userId: {}, dungeonId: {}", userId, dungeonId);
        
        DungeonProgress progress = dungeonService.getUserProgress(userId, dungeonId);
        
        return ApiResponse.success(progress);
    }
    
    /**
     * 获取用户所有副本进度
     */
    @GetMapping("/progress/all")
    public ApiResponse<List<DungeonProgress>> getAllProgress(HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("获取用户所有副本进度, userId: {}", userId);
        
        List<DungeonProgress> progressList = dungeonService.getUserAllProgress(userId);
        
        return ApiResponse.success(progressList);
    }
    
    /**
     * 进入副本
     */
    @PostMapping("/enter")
    public ApiResponse<Map<String, Object>> enterDungeon(@RequestBody DungeonRequest.EnterRequest req,
                                                         HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("进入副本, userId: {}, dungeonId: {}, playerLevel: {}", userId, req.getDungeonId(), req.getPlayerLevel());
        
        DungeonProgress progressBefore = dungeonService.getUserProgress(userId, req.getDungeonId());
        boolean isResume = !progressBefore.getCleared()
                && progressBefore.getDefeatedNpcs() != null 
                && !progressBefore.getDefeatedNpcs().isEmpty();
        
        int remainingEntries = dungeonService.enterDungeon(userId, req.getDungeonId(), req.getPlayerLevel(), req.getCurrentStamina());
        
        Dungeon dungeon = dungeonService.getDungeonDetail(req.getDungeonId());
        DungeonProgress progress = dungeonService.getUserProgress(userId, req.getDungeonId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("remainingEntries", remainingEntries);
        result.put("staminaCost", isResume ? 0 : dungeon.getStaminaCost());
        result.put("progress", progress);
        result.put("resumed", isResume);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 挑战NPC
     */
    @PostMapping("/challenge")
    public ApiResponse<DungeonService.BattleResult> challengeNpc(@RequestBody DungeonRequest.ChallengeRequest req,
                                                                  HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("挑战NPC, userId: {}, dungeonId: {}, npcIndex: {}, generalId: {}", 
                   userId, req.getDungeonId(), req.getNpcIndex(), req.getGeneralId());
        
        // 获取玩家武将
        General playerGeneral = generalRepository.findById(req.getGeneralId());
        if (playerGeneral == null) {
            return ApiResponse.error(400, "武将不存在");
        }
        
        if (!userId.equals(playerGeneral.getUserId())) {
            return ApiResponse.error(403, "无权使用此武将");
        }
        
        DungeonService.BattleResult result = dungeonService.challengeNpc(
            userId, req.getDungeonId(), req.getNpcIndex(), playerGeneral, req.getPlayerLevel());
        
        return ApiResponse.success(result);
    }
    
    /**
     * 重置副本进度
     */
    @PostMapping("/reset")
    public ApiResponse<DungeonProgress> resetProgress(@RequestBody DungeonRequest.ResetRequest req,
                                                      HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("重置副本进度, userId: {}, dungeonId: {}", userId, req.getDungeonId());
        
        DungeonProgress progress = dungeonService.resetProgress(userId, req.getDungeonId());
        
        return ApiResponse.success(progress);
    }
    
    /**
     * 放弃副本挑战（重置进度并退还挑战次数）
     */
    @PostMapping("/abandon")
    public ApiResponse<DungeonProgress> abandonDungeon(@RequestBody DungeonRequest.ResetRequest req,
                                                       HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        logger.info("放弃副本挑战, userId: {}, dungeonId: {}", userId, req.getDungeonId());
        
        DungeonProgress progress = dungeonService.abandonDungeon(userId, req.getDungeonId());
        
        return ApiResponse.success(progress);
    }
    
    /**
     * 战斗胜利结算 - 获取经验（主公和武将都获得经验）
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/victory")
    public ApiResponse<Map<String, Object>> battleVictory(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        Long userIdLong = (Long) request.getAttribute("userId");
        String userId = userIdLong != null ? String.valueOf(userIdLong) : null;
        
        String dungeonId = (String) body.get("dungeonId");
        int npcIndex = body.get("npcIndex") != null ? Integer.parseInt(body.get("npcIndex").toString()) : 1;
        String npcName = (String) body.get("npcName");
        int baseExp = body.get("baseExp") != null ? Integer.parseInt(body.get("baseExp").toString()) : 100;
        List<String> generalIds = body.get("generalIds") != null ? (List<String>) body.get("generalIds") : new ArrayList<>();
        
        logger.info("战斗胜利结算, userId: {}, dungeonId: {}, npcIndex: {}, npcName: {}, baseExp: {}, 参战武将数: {}", 
                   userId, dungeonId, npcIndex, npcName, baseExp, generalIds.size());
        
        // 判断是否首次击败（简化处理，这里假设都不是首次）
        boolean isFirstDefeat = false;
        
        // 给主公经验奖励
        Map<String, Object> expResult = levelService.addDungeonExp(userId, baseExp, isFirstDefeat);
        
        // 给参战武将加经验（武将经验是主公经验的80%）
        int generalExp = (int)(baseExp * 0.8);
        List<Map<String, Object>> generalExpResults = new ArrayList<>();
        
        if (!generalIds.isEmpty()) {
            generalExpResults = generalService.addBattleExpToGenerals(generalIds, generalExp);
            logger.info("给 {} 个武将加经验 {}", generalIds.size(), generalExp);
        }
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        // 主公经验
        result.put("expGained", baseExp);
        result.put("bonusExp", expResult.get("bonusExp"));
        result.put("totalExpGained", expResult.get("totalExpGained"));
        result.put("levelUp", expResult.get("levelUp"));
        result.put("levelsGained", expResult.get("levelsGained"));
        result.put("currentLevel", expResult.get("currentLevel"));
        result.put("currentLevelExp", expResult.get("currentLevelExp"));
        result.put("expToNextLevel", expResult.get("expToNextLevel"));
        result.put("npcName", npcName);
        
        // 武将经验
        result.put("generalExpGained", generalExp);
        result.put("generalExpResults", generalExpResults);
        
        logger.info("用户 {} 击败 {} 获得经验 {}, 当前等级 {}", userId, npcName, baseExp, expResult.get("currentLevel"));
        
        return ApiResponse.success(result);
    }
}

