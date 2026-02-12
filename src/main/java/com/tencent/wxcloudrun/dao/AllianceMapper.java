package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AllianceMapper {
    
    String findById(@Param("id") String id);
    
    List<Map<String, Object>> findAll();
    
    void upsert(@Param("id") String id, @Param("name") String name, @Param("leaderId") String leaderId,
                @Param("data") String data, @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);
    
    void deleteById(@Param("id") String id);
    
    // 用户联盟映射
    String findAllianceIdByUserId(@Param("odUserId") String odUserId);
    
    void upsertUserAlliance(@Param("odUserId") String odUserId, @Param("allianceId") String allianceId);
    
    void deleteUserAlliance(@Param("odUserId") String odUserId);
    
    void deleteUserAllianceByAllianceId(@Param("allianceId") String allianceId);
    
    int userAllianceExists(@Param("odUserId") String odUserId);
}
