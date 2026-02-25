package com.tencent.wxcloudrun.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.tencent.wxcloudrun.dao.UserLearnedTacticsMapper;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/tactics")
public class TacticsController {

    private static final Logger logger = LoggerFactory.getLogger(TacticsController.class);

    @Autowired
    private GeneralRepository generalRepository;
    
    @Autowired
    private UserResourceService userResourceService;
    
    @Autowired
    private UserLearnedTacticsMapper userLearnedTacticsMapper;

    private Set<String> loadLearnedTactics(String userId) {
        String data = userLearnedTacticsMapper.findByUserId(userId);
        if (data == null || data.isEmpty()) {
            return new HashSet<>();
        }
        try {
            return JSON.parseObject(data, new TypeReference<Set<String>>() {});
        } catch (Exception e) {
            logger.error("解析用户 {} 已学习兵法数据失败", userId, e);
            return new HashSet<>();
        }
    }

    private void saveLearnedTactics(String userId, Set<String> learned) {
        userLearnedTacticsMapper.upsert(userId, JSON.toJSONString(learned));
    }

    /**
     * 获取用户已学习的兵法列表
     */
    @GetMapping("/learned")
    public ApiResponse<List<String>> getLearnedTactics(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        Set<String> learned = loadLearnedTactics(userId);
        return ApiResponse.success(new ArrayList<>(learned));
    }

    /**
     * 学习兵法
     */
    @PostMapping("/learn")
    public ApiResponse<Map<String, Object>> learnTactics(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        String tacticsId = (String) body.get("tacticsId");
        Integer paperCost = body.get("paperCost") != null ? Integer.parseInt(body.get("paperCost").toString()) : 0;
        Integer woodCost = body.get("woodCost") != null ? Integer.parseInt(body.get("woodCost").toString()) : 0;
        Integer silverCost = body.get("silverCost") != null ? Integer.parseInt(body.get("silverCost").toString()) : 0;
        
        logger.info("用户 {} 学习兵法 {}, 消耗: 纸张={}, 木材={}, 银两={}", 
            userId, tacticsId, paperCost, woodCost, silverCost);
        
        // 检查是否已学习
        Set<String> learned = loadLearnedTactics(userId);
        if (learned.contains(tacticsId)) {
            return ApiResponse.error(400, "已学习过此兵法");
        }
        
        // 检查并扣除资源
        UserResource resource = userResourceService.getUserResource(userId);
        if (resource.getPaper() < paperCost) {
            return ApiResponse.error(400, "纸张不足");
        }
        if (resource.getWood() < woodCost) {
            return ApiResponse.error(400, "木材不足");
        }
        if (resource.getSilver() < silverCost) {
            return ApiResponse.error(400, "银两不足");
        }
        
        // 扣除资源
        resource.setPaper(resource.getPaper() - paperCost);
        resource.setWood(resource.getWood() - woodCost);
        resource.setSilver(resource.getSilver() - silverCost);
        userResourceService.saveResource(resource);
        
        // 添加到已学习列表并持久化
        learned.add(tacticsId);
        saveLearnedTactics(userId, learned);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("tacticsId", tacticsId);
        result.put("remainingPaper", resource.getPaper());
        result.put("remainingWood", resource.getWood());
        result.put("remainingSilver", resource.getSilver());
        
        return ApiResponse.success(result);
    }

    /**
     * 装备兵法/阵法（一个武将只能装备一个）
     */
    @PostMapping("/equip")
    public ApiResponse<General> equipTactics(@RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        String generalId = (String) body.get("generalId");
        String tacticsId = (String) body.get("tacticsId");
        String tacticsName = (String) body.get("tacticsName");
        String tacticsType = (String) body.get("tacticsType");
        @SuppressWarnings("unchecked")
        Map<String, Object> effect = (Map<String, Object>) body.get("effect");
        
        logger.info("用户 {} 给武将 {} 装备兵法 {}", userId, generalId, tacticsName);
        
        General general = generalRepository.findById(generalId);
        if (general == null) {
            return ApiResponse.error(404, "武将不存在");
        }
        
        if (!userId.equals(general.getUserId())) {
            return ApiResponse.error(403, "武将不属于该用户");
        }
        
        // 设置兵法（只能装备一个，替换原有的）
        General.Tactics tactics = new General.Tactics();
        Map<String, Object> tacticsInfo = new HashMap<>();
        tacticsInfo.put("id", tacticsId);
        tacticsInfo.put("name", tacticsName);
        tacticsInfo.put("type", tacticsType);
        tacticsInfo.put("effect", effect);
        tactics.setPrimary(tacticsInfo);
        tactics.setSecondary(null); // 清空副槽位，因为只能装备一个
        
        // 扩展：直接在 general 上存储便于访问
        general.setTactics(tactics);
        generalRepository.update(general);
        
        return ApiResponse.success(general);
    }

    /**
     * 卸下兵法
     */
    @PostMapping("/unequip")
    public ApiResponse<General> unequipTactics(@RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        
        String generalId = (String) body.get("generalId");
        
        logger.info("用户 {} 给武将 {} 卸下兵法", userId, generalId);
        
        General general = generalRepository.findById(generalId);
        if (general == null) {
            return ApiResponse.error(404, "武将不存在");
        }
        
        if (!userId.equals(general.getUserId())) {
            return ApiResponse.error(403, "武将不属于该用户");
        }
        
        general.setTactics(null);
        generalRepository.update(general);
        
        return ApiResponse.success(general);
    }
}
