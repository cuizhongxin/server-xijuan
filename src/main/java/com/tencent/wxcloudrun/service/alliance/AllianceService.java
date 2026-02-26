package com.tencent.wxcloudrun.service.alliance;

import com.tencent.wxcloudrun.dao.AllianceMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Alliance;
import com.tencent.wxcloudrun.model.Alliance.AllianceApplication;
import com.tencent.wxcloudrun.model.Alliance.AllianceMember;
import com.tencent.wxcloudrun.model.Alliance.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 联盟服务（数据库存储）
 */
@Slf4j
@Service
public class AllianceService {
    
    @Autowired
    private AllianceMapper allianceMapper;
    
    private Alliance loadAlliance(String allianceId) {
        return allianceMapper.findById(allianceId);
    }
    
    private void saveAlliance(Alliance alliance) {
        allianceMapper.upsertAlliance(alliance);
    }
    
    private List<Alliance> loadAllAlliances() {
        List<Alliance> result = allianceMapper.findAll();
        return result != null ? result : new ArrayList<>();
    }
    
    public Alliance createAlliance(String userId, String playerName, String allianceName,
                                   String faction, Integer playerLevel, Long playerPower) {
        if (allianceMapper.userAllianceExists(userId) > 0) {
            throw new BusinessException("您已加入联盟，请先退出当前联盟");
        }
        boolean nameExists = loadAllAlliances().stream().anyMatch(a -> a.getName().equals(allianceName));
        if (nameExists) {
            throw new BusinessException("联盟名称已存在");
        }
        
        Alliance alliance = Alliance.create(allianceName, userId, playerName, playerLevel);
        saveAlliance(alliance);
        if (alliance.getMembers() != null) {
            for (AllianceMember m : alliance.getMembers()) {
                allianceMapper.insertMember(alliance.getId(), m);
            }
        }
        allianceMapper.upsertUserAlliance(userId, alliance.getId());
        
        log.info("用户 {} 创建了联盟: {}", userId, allianceName);
        return alliance;
    }
    
    public List<Alliance> getAllianceList(String faction) {
        return loadAllAlliances().stream()
                .sorted((a, b) -> Integer.compare(
                    b.getMembers() != null ? b.getMembers().size() : 0,
                    a.getMembers() != null ? a.getMembers().size() : 0))
                .collect(Collectors.toList());
    }
    
    public Alliance getAllianceDetail(String allianceId) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        return alliance;
    }
    
    public Alliance getUserAlliance(String userId) {
        String allianceId = allianceMapper.findAllianceIdByUserId(userId);
        if (allianceId == null) { return null; }
        return loadAlliance(allianceId);
    }
    
    public void applyToJoin(String userId, String playerName, String allianceId,
                           Integer playerLevel, Long playerPower) {
        if (allianceMapper.userAllianceExists(userId) > 0) {
            throw new BusinessException("您已加入联盟，请先退出当前联盟");
        }
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        int memberCount = alliance.getMembers() != null ? alliance.getMembers().size() : 0;
        if (memberCount >= alliance.getMaxMembers()) { throw new BusinessException("联盟成员已满"); }
        
        boolean alreadyApplied = alliance.getApplications() != null && alliance.getApplications().stream()
                .anyMatch(a -> a.getUserId().equals(userId) && "pending".equals(a.getStatus()));
        if (alreadyApplied) { throw new BusinessException("您已申请过该联盟，请等待审核"); }
        
        AllianceApplication application = AllianceApplication.builder()
                .userId(userId).userName(playerName).userLevel(playerLevel)
                .applyTime(System.currentTimeMillis()).status("pending").build();
        allianceMapper.insertApplication(allianceId, application);
        alliance.setUpdateTime(System.currentTimeMillis());
        saveAlliance(alliance);
        log.info("用户 {} 申请加入联盟: {}", userId, alliance.getName());
    }
    
    public void processApplication(String leaderId, String allianceId, String applicantId, boolean approve) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getUserId().equals(leaderId)).findFirst().orElse(null);
        if (operator == null || (!Role.LEADER.equals(operator.getRole()) && !Role.OFFICER.equals(operator.getRole()))) {
            throw new BusinessException("您没有权限审批申请");
        }
        AllianceApplication application = alliance.getApplications().stream()
                .filter(a -> a.getUserId().equals(applicantId) && "pending".equals(a.getStatus())).findFirst().orElse(null);
        if (application == null) { throw new BusinessException("申请不存在"); }
        
        if (approve) {
            int memberCount = alliance.getMembers() != null ? alliance.getMembers().size() : 0;
            if (memberCount >= alliance.getMaxMembers()) { throw new BusinessException("联盟成员已满"); }
            if (allianceMapper.userAllianceExists(applicantId) > 0) {
                allianceMapper.updateApplicationStatus(allianceId, applicantId, "rejected");
                throw new BusinessException("该玩家已加入其他联盟");
            }
            AllianceMember newMember = AllianceMember.builder()
                    .userId(application.getUserId()).name(application.getUserName()).role(Role.MEMBER)
                    .level(application.getUserLevel()).contribution(0L)
                    .joinTime(System.currentTimeMillis()).build();
            allianceMapper.insertMember(allianceId, newMember);
            allianceMapper.upsertUserAlliance(applicantId, allianceId);
            allianceMapper.updateApplicationStatus(allianceId, applicantId, "approved");
            log.info("用户 {} 加入联盟: {}", applicantId, alliance.getName());
        } else {
            allianceMapper.updateApplicationStatus(allianceId, applicantId, "rejected");
            log.info("拒绝用户 {} 加入联盟: {}", applicantId, alliance.getName());
        }
        alliance.setUpdateTime(System.currentTimeMillis());
        saveAlliance(alliance);
    }
    
    public void leaveAlliance(String userId) {
        String allianceId = allianceMapper.findAllianceIdByUserId(userId);
        if (allianceId == null) { throw new BusinessException("您未加入任何联盟"); }
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { allianceMapper.deleteUserAlliance(userId); throw new BusinessException("联盟不存在"); }
        if (alliance.getLeaderId().equals(userId)) { throw new BusinessException("盟主不能退出联盟，请先转让盟主或解散联盟"); }
        
        allianceMapper.deleteMember(allianceId, userId);
        allianceMapper.deleteUserAlliance(userId);
        alliance.setUpdateTime(System.currentTimeMillis());
        saveAlliance(alliance);
        log.info("用户 {} 退出联盟: {}", userId, alliance.getName());
    }
    
    public void kickMember(String leaderId, String allianceId, String memberId) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getUserId().equals(leaderId)).findFirst().orElse(null);
        if (operator == null || (!Role.LEADER.equals(operator.getRole()) && !Role.OFFICER.equals(operator.getRole()))) {
            throw new BusinessException("您没有权限踢出成员");
        }
        if (leaderId.equals(memberId)) { throw new BusinessException("不能踢出自己"); }
        AllianceMember member = alliance.getMembers().stream()
                .filter(m -> m.getUserId().equals(memberId)).findFirst().orElse(null);
        if (member == null) { throw new BusinessException("成员不存在"); }
        if (Role.LEADER.equals(member.getRole())) { throw new BusinessException("不能踢出盟主"); }
        if (Role.OFFICER.equals(operator.getRole()) && Role.OFFICER.equals(member.getRole())) {
            throw new BusinessException("副盟主不能踢出副盟主");
        }
        allianceMapper.deleteMember(allianceId, memberId);
        allianceMapper.deleteUserAlliance(memberId);
        alliance.setUpdateTime(System.currentTimeMillis());
        saveAlliance(alliance);
        log.info("用户 {} 被踢出联盟: {}", memberId, alliance.getName());
    }
    
    public void transferLeader(String currentLeaderId, String allianceId, String newLeaderId) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        if (!alliance.getLeaderId().equals(currentLeaderId)) { throw new BusinessException("只有盟主才能转让盟主"); }
        AllianceMember newLeader = alliance.getMembers().stream()
                .filter(m -> m.getUserId().equals(newLeaderId)).findFirst().orElse(null);
        if (newLeader == null) { throw new BusinessException("该玩家不是联盟成员"); }
        for (AllianceMember m : alliance.getMembers()) {
            if (m.getUserId().equals(currentLeaderId)) { m.setRole(Role.MEMBER); allianceMapper.insertMember(allianceId, m); }
            else if (m.getUserId().equals(newLeaderId)) { m.setRole(Role.LEADER); allianceMapper.insertMember(allianceId, m); }
        }
        alliance.setLeaderId(newLeaderId);
        alliance.setLeaderName(newLeader.getName());
        alliance.setUpdateTime(System.currentTimeMillis());
        saveAlliance(alliance);
        log.info("联盟 {} 盟主从 {} 转让给 {}", alliance.getName(), currentLeaderId, newLeaderId);
    }
    
    public void setMemberPosition(String leaderId, String allianceId, String memberId, String role) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        if (!alliance.getLeaderId().equals(leaderId)) { throw new BusinessException("只有盟主才能设置职位"); }
        if (leaderId.equals(memberId)) { throw new BusinessException("不能修改自己的职位"); }
        AllianceMember member = alliance.getMembers().stream()
                .filter(m -> m.getUserId().equals(memberId)).findFirst().orElse(null);
        if (member == null) { throw new BusinessException("成员不存在"); }
        if (Role.LEADER.equals(role)) { throw new BusinessException("请使用转让盟主功能"); }
        member.setRole(role);
        allianceMapper.insertMember(allianceId, member);
        alliance.setUpdateTime(System.currentTimeMillis());
        saveAlliance(alliance);
        log.info("设置 {} 的职位为: {}", memberId, role);
    }
    
    public void dissolveAlliance(String leaderId, String allianceId) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        if (!alliance.getLeaderId().equals(leaderId)) { throw new BusinessException("只有盟主才能解散联盟"); }
        allianceMapper.deleteUserAllianceByAllianceId(allianceId);
        allianceMapper.deleteMembersByAllianceId(allianceId);
        allianceMapper.deleteApplicationsByAllianceId(allianceId);
        allianceMapper.deleteById(allianceId);
        log.info("联盟 {} 已解散", alliance.getName());
    }
    
    public void updateAnnouncement(String leaderId, String allianceId, String notice) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getUserId().equals(leaderId)).findFirst().orElse(null);
        if (operator == null || (!Role.LEADER.equals(operator.getRole()) && !Role.OFFICER.equals(operator.getRole()))) {
            throw new BusinessException("您没有权限修改公告");
        }
        alliance.setNotice(notice);
        alliance.setUpdateTime(System.currentTimeMillis());
        saveAlliance(alliance);
        log.info("联盟 {} 公告已更新", alliance.getName());
    }
    
    public List<AllianceApplication> getApplicationList(String leaderId, String allianceId) {
        Alliance alliance = loadAlliance(allianceId);
        if (alliance == null) { throw new BusinessException("联盟不存在"); }
        AllianceMember operator = alliance.getMembers().stream()
                .filter(m -> m.getUserId().equals(leaderId)).findFirst().orElse(null);
        if (operator == null || (!Role.LEADER.equals(operator.getRole()) && !Role.OFFICER.equals(operator.getRole()))) {
            throw new BusinessException("您没有权限查看申请列表");
        }
        return alliance.getApplications().stream().filter(a -> "pending".equals(a.getStatus())).collect(Collectors.toList());
    }
    
    public void initTestData() {
        List<Alliance> existing = loadAllAlliances();
        if (existing.isEmpty()) {
            createTestAlliance("kiss", "嘉兴超度", 1);
            createTestAlliance("固定的", "嫩嫩", 3);
            createTestAlliance("——天璃ゞヤ", "天澜蓝色", 3);
            createTestAlliance("天宇♥璜", "奇幸运", 1);
            createTestAlliance("黄雀联盟", "元祖剑圣 风", 3);
            createTestAlliance("衰", "懂不凡红颜", 1);
            createTestAlliance("无所谓", "已返起", 6);
            createTestAlliance("54永远爱88", "爱情毁", 1);
        }
    }
    
    private void createTestAlliance(String name, String leaderName, int memberCount) {
        String leaderId = "test_leader_" + name;
        Alliance alliance = Alliance.create(name, leaderId, leaderName, 60);
        saveAlliance(alliance);
        if (alliance.getMembers() != null) {
            for (AllianceMember m : alliance.getMembers()) {
                allianceMapper.insertMember(alliance.getId(), m);
            }
        }
        allianceMapper.upsertUserAlliance(leaderId, alliance.getId());
    }
}
