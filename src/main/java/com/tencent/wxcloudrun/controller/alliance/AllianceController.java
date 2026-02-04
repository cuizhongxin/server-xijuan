package com.tencent.wxcloudrun.controller.alliance;

import com.tencent.wxcloudrun.model.Alliance;
import com.tencent.wxcloudrun.service.alliance.AllianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联盟控制器
 */
@Slf4j
@RestController
@RequestMapping("/alliance")
@RequiredArgsConstructor
public class AllianceController {
    
    private final AllianceService allianceService;
    
    @PostConstruct
    public void init() {
        // 初始化测试数据
        allianceService.initTestData();
    }
    
    /**
     * 获取联盟列表
     */
    @GetMapping("/list")
    public Map<String, Object> getAllianceList(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestParam(required = false) String faction) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Alliance> alliances = allianceService.getAllianceList(faction);
            Alliance myAlliance = allianceService.getUserAlliance(odUserId);
            
            result.put("success", true);
            result.put("alliances", alliances);
            result.put("myAlliance", myAlliance);
            result.put("hasAlliance", myAlliance != null);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取我的联盟
     */
    @GetMapping("/my")
    public Map<String, Object> getMyAlliance(@RequestHeader("X-User-ID") String odUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Alliance alliance = allianceService.getUserAlliance(odUserId);
            if (alliance == null) {
                result.put("success", true);
                result.put("hasAlliance", false);
            } else {
                result.put("success", true);
                result.put("hasAlliance", true);
                result.put("alliance", alliance);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取联盟详情
     */
    @GetMapping("/detail/{allianceId}")
    public Map<String, Object> getAllianceDetail(
            @RequestHeader("X-User-ID") String odUserId,
            @PathVariable String allianceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Alliance alliance = allianceService.getAllianceDetail(allianceId);
            result.put("success", true);
            result.put("alliance", alliance);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 创建联盟
     */
    @PostMapping("/create")
    public Map<String, Object> createAlliance(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String allianceName = (String) body.get("name");
            String faction = (String) body.get("faction");
            String playerName = (String) body.get("playerName");
            Integer playerLevel = body.get("playerLevel") != null ? 
                    ((Number) body.get("playerLevel")).intValue() : 1;
            Long playerPower = body.get("playerPower") != null ? 
                    ((Number) body.get("playerPower")).longValue() : 0L;
            
            Alliance alliance = allianceService.createAlliance(odUserId, playerName, allianceName, 
                    faction, playerLevel, playerPower);
            
            result.put("success", true);
            result.put("alliance", alliance);
            result.put("message", "联盟创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 申请加入联盟
     */
    @PostMapping("/apply/{allianceId}")
    public Map<String, Object> applyToJoin(
            @RequestHeader("X-User-ID") String odUserId,
            @PathVariable String allianceId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String playerName = (String) body.get("playerName");
            Integer playerLevel = body.get("playerLevel") != null ? 
                    ((Number) body.get("playerLevel")).intValue() : 1;
            Long playerPower = body.get("playerPower") != null ? 
                    ((Number) body.get("playerPower")).longValue() : 0L;
            
            allianceService.applyToJoin(odUserId, playerName, allianceId, playerLevel, playerPower);
            
            result.put("success", true);
            result.put("message", "申请已提交，请等待审核");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 处理申请
     */
    @PostMapping("/process-application")
    public Map<String, Object> processApplication(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String allianceId = (String) body.get("allianceId");
            String applicantId = (String) body.get("applicantId");
            Boolean approve = (Boolean) body.get("approve");
            
            allianceService.processApplication(odUserId, allianceId, applicantId, approve);
            
            result.put("success", true);
            result.put("message", approve ? "已同意申请" : "已拒绝申请");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 退出联盟
     */
    @PostMapping("/leave")
    public Map<String, Object> leaveAlliance(@RequestHeader("X-User-ID") String odUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            allianceService.leaveAlliance(odUserId);
            result.put("success", true);
            result.put("message", "已退出联盟");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 踢出成员
     */
    @PostMapping("/kick")
    public Map<String, Object> kickMember(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String allianceId = (String) body.get("allianceId");
            String memberId = (String) body.get("memberId");
            
            allianceService.kickMember(odUserId, allianceId, memberId);
            
            result.put("success", true);
            result.put("message", "已踢出成员");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 转让盟主
     */
    @PostMapping("/transfer-leader")
    public Map<String, Object> transferLeader(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String allianceId = (String) body.get("allianceId");
            String newLeaderId = (String) body.get("newLeaderId");
            
            allianceService.transferLeader(odUserId, allianceId, newLeaderId);
            
            result.put("success", true);
            result.put("message", "盟主已转让");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 设置成员职位
     */
    @PostMapping("/set-position")
    public Map<String, Object> setMemberPosition(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String allianceId = (String) body.get("allianceId");
            String memberId = (String) body.get("memberId");
            String position = (String) body.get("position");
            
            allianceService.setMemberPosition(odUserId, allianceId, memberId, position);
            
            result.put("success", true);
            result.put("message", "职位设置成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 解散联盟
     */
    @PostMapping("/dissolve")
    public Map<String, Object> dissolveAlliance(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String allianceId = (String) body.get("allianceId");
            
            allianceService.dissolveAlliance(odUserId, allianceId);
            
            result.put("success", true);
            result.put("message", "联盟已解散");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 修改公告
     */
    @PostMapping("/update-announcement")
    public Map<String, Object> updateAnnouncement(
            @RequestHeader("X-User-ID") String odUserId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String allianceId = (String) body.get("allianceId");
            String announcement = (String) body.get("announcement");
            
            allianceService.updateAnnouncement(odUserId, allianceId, announcement);
            
            result.put("success", true);
            result.put("message", "公告已更新");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取申请列表
     */
    @GetMapping("/applications/{allianceId}")
    public Map<String, Object> getApplicationList(
            @RequestHeader("X-User-ID") String odUserId,
            @PathVariable String allianceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Alliance.AllianceApplication> applications = allianceService.getApplicationList(odUserId, allianceId);
            result.put("success", true);
            result.put("applications", applications);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
