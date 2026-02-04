package com.tencent.wxcloudrun.service.alliance;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Alliance;
import com.tencent.wxcloudrun.model.Alliance.AllianceApplication;
import com.tencent.wxcloudrun.model.Alliance.AllianceMember;
import com.tencent.wxcloudrun.model.Alliance.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 联盟服务
 */
@Slf4j
@Service
public class AllianceService {
    
    // 联盟存储 allianceId -> Alliance
    private final Map<String, Alliance> allianceStore = new ConcurrentHashMap<>();
    
    // 用户联盟映射 odUserId -> allianceId
    private final Map<String, String> userAllianceMap = new ConcurrentHashMap<>();
    
    /**
     * 创建联盟
     */
    public Alliance createAlliance(String odUserId, String playerName, String allianceName, 
                                   String faction, Integer playerLevel, Long playerPower) {
        // 检查用户是否已加入联盟
        if (userAllianceMap.containsKey(odUserId)) {
            throw new BusinessException("您已加入联盟，请先退出当前联盟");
        }
        
        // 检查联盟名称是否已存在
        boolean nameExists = allianceStore.values().stream()
                .anyMatch(a -> a.getName().equals(allianceName));
        if (nameExists) {
            throw new BusinessException("联盟名称已存在");
        }
        
        // 创建联盟
        Alliance alliance = Alliance.create(allianceName, faction, odUserId, playerName, playerLevel, playerPower);
        allianceStore.put(alliance.getId(), alliance);
        userAllianceMap.put(odUserId, alliance.getId());
        
        log.info("用户 {} 创建了联盟: {}", odUserId, allianceName);
        return alliance;
    }
    
    /**
     * 获取联盟列表（按国家筛选）
     */
    public List<Alliance> getAllianceList(String faction) {
        return allianceStore.values().stream()
                .filter(a -> faction == null || faction.isEmpty() || a.getFaction().equals(faction))
                .sorted((a, b) -> Long.compare(b.getTotalPower(), a.getTotalPower()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取联盟详情
     */
    public Alliance getAllianceDetail(String allianceId) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        return alliance;
    }
    
    /**
     * 获取用户所在联盟
     */
    public Alliance getUserAlliance(String odUserId) {
        String allianceId = userAllianceMap.get(odUserId);
        if (allianceId == null) {
            return null;
        }
        return allianceStore.get(allianceId);
    }
    
    /**
     * 申请加入联盟
     */
    public void applyToJoin(String odUserId, String playerName, String allianceId, 
                           Integer playerLevel, Long playerPower) {
        // 检查用户是否已加入联盟
        if (userAllianceMap.containsKey(odUserId)) {
            throw new BusinessException("您已加入联盟，请先退出当前联盟");
        }
        
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 检查联盟是否已满
        if (alliance.getMemberCount() >= alliance.getMaxMembers()) {
            throw new BusinessException("联盟成员已满");
        }
        
        // 检查是否已申请
        boolean alreadyApplied = alliance.getApplications().stream()
                .anyMatch(a -> a.getOdUserId().equals(odUserId) && "pending".equals(a.getStatus()));
        if (alreadyApplied) {
            throw new BusinessException("您已申请过该联盟，请等待审核");
        }
        
        // 添加申请
        AllianceApplication application = AllianceApplication.builder()
                .odUserId(odUserId)
                .name(playerName)
                .level(playerLevel)
                .power(playerPower)
                .applyTime(System.currentTimeMillis())
                .status("pending")
                .build();
        alliance.getApplications().add(application);
        alliance.setUpdateTime(System.currentTimeMillis());
        
        log.info("用户 {} 申请加入联盟: {}", odUserId, alliance.getName());
    }
    
    /**
     * 审批申请
     */
    public void processApplication(String leaderId, String allianceId, String applicantId, boolean approve) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 检查权限（盟主或副盟主）
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(leaderId))
                .findFirst()
                .orElse(null);
        if (operator == null || 
            (!Position.LEADER.equals(operator.getPosition()) && !Position.VICE_LEADER.equals(operator.getPosition()))) {
            throw new BusinessException("您没有权限审批申请");
        }
        
        // 查找申请
        AllianceApplication application = alliance.getApplications().stream()
                .filter(a -> a.getOdUserId().equals(applicantId) && "pending".equals(a.getStatus()))
                .findFirst()
                .orElse(null);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        
        if (approve) {
            // 检查联盟是否已满
            if (alliance.getMemberCount() >= alliance.getMaxMembers()) {
                throw new BusinessException("联盟成员已满");
            }
            
            // 检查申请人是否已加入其他联盟
            if (userAllianceMap.containsKey(applicantId)) {
                application.setStatus("rejected");
                throw new BusinessException("该玩家已加入其他联盟");
            }
            
            // 添加成员
            AllianceMember newMember = AllianceMember.builder()
                    .odUserId(application.getOdUserId())
                    .name(application.getName())
                    .position(Position.MEMBER)
                    .level(application.getLevel())
                    .contribution(0L)
                    .power(application.getPower())
                    .joinTime(System.currentTimeMillis())
                    .lastOnlineTime(System.currentTimeMillis())
                    .build();
            alliance.getMembers().add(newMember);
            alliance.setMemberCount(alliance.getMemberCount() + 1);
            alliance.setTotalPower(alliance.getTotalPower() + (application.getPower() != null ? application.getPower() : 0L));
            userAllianceMap.put(applicantId, allianceId);
            
            application.setStatus("approved");
            log.info("用户 {} 加入联盟: {}", applicantId, alliance.getName());
        } else {
            application.setStatus("rejected");
            log.info("拒绝用户 {} 加入联盟: {}", applicantId, alliance.getName());
        }
        
        alliance.setUpdateTime(System.currentTimeMillis());
    }
    
    /**
     * 退出联盟
     */
    public void leaveAlliance(String odUserId) {
        String allianceId = userAllianceMap.get(odUserId);
        if (allianceId == null) {
            throw new BusinessException("您未加入任何联盟");
        }
        
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            userAllianceMap.remove(odUserId);
            throw new BusinessException("联盟不存在");
        }
        
        // 检查是否是盟主
        if (alliance.getLeaderId().equals(odUserId)) {
            throw new BusinessException("盟主不能退出联盟，请先转让盟主或解散联盟");
        }
        
        // 移除成员
        AllianceMember member = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(odUserId))
                .findFirst()
                .orElse(null);
        if (member != null) {
            alliance.getMembers().remove(member);
            alliance.setMemberCount(alliance.getMemberCount() - 1);
            alliance.setTotalPower(alliance.getTotalPower() - (member.getPower() != null ? member.getPower() : 0L));
        }
        userAllianceMap.remove(odUserId);
        alliance.setUpdateTime(System.currentTimeMillis());
        
        log.info("用户 {} 退出联盟: {}", odUserId, alliance.getName());
    }
    
    /**
     * 踢出成员
     */
    public void kickMember(String leaderId, String allianceId, String memberId) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 检查权限
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(leaderId))
                .findFirst()
                .orElse(null);
        if (operator == null || 
            (!Position.LEADER.equals(operator.getPosition()) && !Position.VICE_LEADER.equals(operator.getPosition()))) {
            throw new BusinessException("您没有权限踢出成员");
        }
        
        // 不能踢自己
        if (leaderId.equals(memberId)) {
            throw new BusinessException("不能踢出自己");
        }
        
        // 查找要踢出的成员
        AllianceMember member = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(memberId))
                .findFirst()
                .orElse(null);
        if (member == null) {
            throw new BusinessException("成员不存在");
        }
        
        // 副盟主不能踢盟主
        if (Position.LEADER.equals(member.getPosition())) {
            throw new BusinessException("不能踢出盟主");
        }
        
        // 副盟主不能踢副盟主
        if (Position.VICE_LEADER.equals(operator.getPosition()) && Position.VICE_LEADER.equals(member.getPosition())) {
            throw new BusinessException("副盟主不能踢出副盟主");
        }
        
        alliance.getMembers().remove(member);
        alliance.setMemberCount(alliance.getMemberCount() - 1);
        alliance.setTotalPower(alliance.getTotalPower() - (member.getPower() != null ? member.getPower() : 0L));
        userAllianceMap.remove(memberId);
        alliance.setUpdateTime(System.currentTimeMillis());
        
        log.info("用户 {} 被踢出联盟: {}", memberId, alliance.getName());
    }
    
    /**
     * 转让盟主
     */
    public void transferLeader(String currentLeaderId, String allianceId, String newLeaderId) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 检查当前用户是否是盟主
        if (!alliance.getLeaderId().equals(currentLeaderId)) {
            throw new BusinessException("只有盟主才能转让盟主");
        }
        
        // 查找新盟主
        AllianceMember newLeader = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(newLeaderId))
                .findFirst()
                .orElse(null);
        if (newLeader == null) {
            throw new BusinessException("该玩家不是联盟成员");
        }
        
        // 更新职位
        alliance.getMembers().forEach(m -> {
            if (m.getOdUserId().equals(currentLeaderId)) {
                m.setPosition(Position.MEMBER);
            } else if (m.getOdUserId().equals(newLeaderId)) {
                m.setPosition(Position.LEADER);
            }
        });
        
        alliance.setLeaderId(newLeaderId);
        alliance.setLeaderName(newLeader.getName());
        alliance.setUpdateTime(System.currentTimeMillis());
        
        log.info("联盟 {} 盟主从 {} 转让给 {}", alliance.getName(), currentLeaderId, newLeaderId);
    }
    
    /**
     * 设置成员职位
     */
    public void setMemberPosition(String leaderId, String allianceId, String memberId, String position) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 只有盟主能设置职位
        if (!alliance.getLeaderId().equals(leaderId)) {
            throw new BusinessException("只有盟主才能设置职位");
        }
        
        // 不能修改自己的职位
        if (leaderId.equals(memberId)) {
            throw new BusinessException("不能修改自己的职位");
        }
        
        // 查找成员
        AllianceMember member = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(memberId))
                .findFirst()
                .orElse(null);
        if (member == null) {
            throw new BusinessException("成员不存在");
        }
        
        // 不能设置为盟主
        if (Position.LEADER.equals(position)) {
            throw new BusinessException("请使用转让盟主功能");
        }
        
        member.setPosition(position);
        alliance.setUpdateTime(System.currentTimeMillis());
        
        log.info("设置 {} 的职位为: {}", memberId, position);
    }
    
    /**
     * 解散联盟
     */
    public void dissolveAlliance(String leaderId, String allianceId) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 只有盟主能解散
        if (!alliance.getLeaderId().equals(leaderId)) {
            throw new BusinessException("只有盟主才能解散联盟");
        }
        
        // 移除所有成员的联盟关系
        alliance.getMembers().forEach(m -> userAllianceMap.remove(m.getOdUserId()));
        
        // 删除联盟
        allianceStore.remove(allianceId);
        
        log.info("联盟 {} 已解散", alliance.getName());
    }
    
    /**
     * 修改公告
     */
    public void updateAnnouncement(String leaderId, String allianceId, String announcement) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 检查权限
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(leaderId))
                .findFirst()
                .orElse(null);
        if (operator == null || 
            (!Position.LEADER.equals(operator.getPosition()) && !Position.VICE_LEADER.equals(operator.getPosition()))) {
            throw new BusinessException("您没有权限修改公告");
        }
        
        alliance.setAnnouncement(announcement);
        alliance.setUpdateTime(System.currentTimeMillis());
        
        log.info("联盟 {} 公告已更新", alliance.getName());
    }
    
    /**
     * 获取申请列表
     */
    public List<AllianceApplication> getApplicationList(String leaderId, String allianceId) {
        Alliance alliance = allianceStore.get(allianceId);
        if (alliance == null) {
            throw new BusinessException("联盟不存在");
        }
        
        // 检查权限
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getOdUserId().equals(leaderId))
                .findFirst()
                .orElse(null);
        if (operator == null || 
            (!Position.LEADER.equals(operator.getPosition()) && !Position.VICE_LEADER.equals(operator.getPosition()))) {
            throw new BusinessException("您没有权限查看申请列表");
        }
        
        return alliance.getApplications().stream()
                .filter(a -> "pending".equals(a.getStatus()))
                .collect(Collectors.toList());
    }
    
    /**
     * 初始化测试数据
     */
    public void initTestData() {
        if (allianceStore.isEmpty()) {
            // 创建一些测试联盟
            createTestAlliance("kiss", "蜀", "嘉兴超度", 1);
            createTestAlliance("固定的", "魏", "嫩嫩", 3);
            createTestAlliance("——天璃ゞヤ", "吴", "天澜蓝色", 3);
            createTestAlliance("天宇♥璜", "蜀", "奇幸运", 1);
            createTestAlliance("黄雀联盟", "蜀", "元祖剑圣 风", 3);
            createTestAlliance("衰", "吴", "懂不凡红颜", 1);
            createTestAlliance("无所谓", "魏", "已返起", 6);
            createTestAlliance("54永远爱88", "吴", "爱情毁", 1);
        }
    }
    
    private void createTestAlliance(String name, String faction, String leaderName, int memberCount) {
        String leaderId = "test_leader_" + name;
        Alliance alliance = Alliance.create(name, faction, leaderId, leaderName, 60, 100000L);
        alliance.setMemberCount(memberCount);
        allianceStore.put(alliance.getId(), alliance);
        userAllianceMap.put(leaderId, alliance.getId());
    }
}
