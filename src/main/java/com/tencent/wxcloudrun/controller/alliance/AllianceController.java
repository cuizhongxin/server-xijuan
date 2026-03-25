package com.tencent.wxcloudrun.controller.alliance;

import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.model.Alliance;
import com.tencent.wxcloudrun.service.alliance.AllianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
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
    
    private String getUserId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("userId"));
    }
    
    @PostConstruct
    public void init() {
        allianceService.initTestData();
    }
    
    /**
     * 获取联盟列表
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getAllianceList(
            HttpServletRequest request,
            @RequestParam(required = false) String faction) {
        try {
            String odUserId = getUserId(request);
            List<Alliance> alliances = allianceService.getAllianceList(odUserId, faction);
            Alliance myAlliance = allianceService.getUserAlliance(odUserId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("alliances", alliances);
            data.put("myAlliance", myAlliance);
            data.put("hasAlliance", myAlliance != null);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取联盟列表异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取我的联盟
     */
    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> getMyAlliance(HttpServletRequest request) {
        try {
            String odUserId = getUserId(request);
            Alliance alliance = allianceService.getUserAlliance(odUserId);
            Map<String, Object> data = new HashMap<>();
            if (alliance == null) {
                data.put("hasAlliance", false);
            } else {
                data.put("hasAlliance", true);
                data.put("alliance", alliance);
            }
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取我的联盟异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取联盟详情
     */
    @GetMapping("/detail/{allianceId}")
    public ApiResponse<Map<String, Object>> getAllianceDetail(
            HttpServletRequest request,
            @PathVariable String allianceId) {
        try {
            Alliance alliance = allianceService.getAllianceDetail(allianceId);
            Map<String, Object> data = new HashMap<>();
            data.put("alliance", alliance);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取联盟详情异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 创建联盟
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createAlliance(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String allianceName = (String) body.get("name");
            String faction = (String) body.get("faction");
            String playerName = (String) body.get("playerName");
            Integer playerLevel = body.get("playerLevel") != null ? 
                    ((Number) body.get("playerLevel")).intValue() : 1;
            Long playerPower = body.get("playerPower") != null ? 
                    ((Number) body.get("playerPower")).longValue() : 0L;
            
            Alliance alliance = allianceService.createAlliance(odUserId, playerName, allianceName, 
                    faction, playerLevel, playerPower);
            
            Map<String, Object> data = new HashMap<>();
            data.put("alliance", alliance);
            data.put("message", "联盟创建成功");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("创建联盟异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 申请加入联盟
     */
    @PostMapping("/apply/{allianceId}")
    public ApiResponse<Map<String, Object>> applyToJoin(
            HttpServletRequest request,
            @PathVariable String allianceId,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String playerName = (String) body.get("playerName");
            Integer playerLevel = body.get("playerLevel") != null ? 
                    ((Number) body.get("playerLevel")).intValue() : 1;
            Long playerPower = body.get("playerPower") != null ? 
                    ((Number) body.get("playerPower")).longValue() : 0L;
            
            String faction = (String) body.get("faction");
            allianceService.applyToJoin(odUserId, playerName, allianceId, playerLevel, playerPower, faction);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "申请已提交，请等待审核");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("申请加入联盟异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 处理申请
     */
    @PostMapping("/process-application")
    public ApiResponse<Map<String, Object>> processApplication(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String allianceId = (String) body.get("allianceId");
            String applicantId = (String) body.get("applicantId");
            Boolean approve = (Boolean) body.get("approve");
            
            allianceService.processApplication(odUserId, allianceId, applicantId, approve);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", approve ? "已同意申请" : "已拒绝申请");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("处理申请异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 退出联盟
     */
    @PostMapping("/leave")
    public ApiResponse<Map<String, Object>> leaveAlliance(HttpServletRequest request) {
        try {
            String odUserId = getUserId(request);
            allianceService.leaveAlliance(odUserId);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "已退出联盟");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("退出联盟异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 踢出成员
     */
    @PostMapping("/kick")
    public ApiResponse<Map<String, Object>> kickMember(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String allianceId = (String) body.get("allianceId");
            String memberId = (String) body.get("memberId");
            
            allianceService.kickMember(odUserId, allianceId, memberId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "已踢出成员");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("踢出成员异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 转让盟主
     */
    @PostMapping("/transfer-leader")
    public ApiResponse<Map<String, Object>> transferLeader(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String allianceId = (String) body.get("allianceId");
            String newLeaderId = (String) body.get("newLeaderId");
            
            allianceService.transferLeader(odUserId, allianceId, newLeaderId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "盟主已转让");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("转让盟主异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 设置成员职位
     */
    @PostMapping("/set-position")
    public ApiResponse<Map<String, Object>> setMemberPosition(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String allianceId = (String) body.get("allianceId");
            String memberId = (String) body.get("memberId");
            String position = (String) body.get("position");
            
            allianceService.setMemberPosition(odUserId, allianceId, memberId, position);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "职位设置成功");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("设置成员职位异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 解散联盟
     */
    @PostMapping("/dissolve")
    public ApiResponse<Map<String, Object>> dissolveAlliance(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String allianceId = (String) body.get("allianceId");
            
            allianceService.dissolveAlliance(odUserId, allianceId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "联盟已解散");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("解散联盟异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 修改公告
     */
    @PostMapping("/update-announcement")
    public ApiResponse<Map<String, Object>> updateAnnouncement(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String odUserId = getUserId(request);
            String allianceId = (String) body.get("allianceId");
            String announcement = (String) body.get("announcement");
            
            allianceService.updateAnnouncement(odUserId, allianceId, announcement);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "公告已更新");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("修改公告异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取申请列表
     */
    @GetMapping("/applications/{allianceId}")
    public ApiResponse<Map<String, Object>> getApplicationList(
            HttpServletRequest request,
            @PathVariable String allianceId) {
        try {
            String odUserId = getUserId(request);
            List<Alliance.AllianceApplication> applications = allianceService.getApplicationList(odUserId, allianceId);
            Map<String, Object> data = new HashMap<>();
            data.put("applications", applications);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取申请列表异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 弹劾盟主
     */
    @PostMapping("/impeach")
    public ApiResponse<Map<String, Object>> impeachLeader(HttpServletRequest request) {
        try {
            String odUserId = getUserId(request);
            allianceService.impeachLeader(odUserId);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "弹劾成功，您已成为新盟主");
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("弹劾盟主异常", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
