package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Alliance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AllianceMapper {
    
    Alliance findById(@Param("id") String id);
    
    List<Alliance> findAll();
    
    void upsertAlliance(Alliance alliance);
    
    void deleteById(@Param("id") String id);
    
    void deleteMembersByAllianceId(@Param("allianceId") String allianceId);
    
    void deleteApplicationsByAllianceId(@Param("allianceId") String allianceId);
    
    void insertMember(@Param("allianceId") String allianceId, @Param("m") Alliance.AllianceMember m);
    
    void insertApplication(@Param("allianceId") String allianceId, @Param("a") Alliance.AllianceApplication a);
    
    void updateApplicationStatus(@Param("allianceId") String allianceId, @Param("userId") String userId, @Param("status") String status);
    
    void deleteMember(@Param("allianceId") String allianceId, @Param("userId") String userId);
    
    // 用户联盟映射
    String findAllianceIdByUserId(@Param("userId") String userId);
    
    void upsertUserAlliance(@Param("userId") String userId, @Param("allianceId") String allianceId);
    
    void deleteUserAlliance(@Param("userId") String userId);
    
    void deleteUserAllianceByAllianceId(@Param("allianceId") String allianceId);
    
    int userAllianceExists(@Param("userId") String userId);
}
