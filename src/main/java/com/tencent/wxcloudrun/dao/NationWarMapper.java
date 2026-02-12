package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface NationWarMapper {
    
    String findById(@Param("id") String id);
    
    List<Map<String, Object>> findAll();
    
    void upsert(@Param("id") String id, @Param("warDate") String warDate, @Param("data") String data);
    
    // 玩家国籍
    String findPlayerNation(@Param("odUserId") String odUserId);
    
    void upsertPlayerNation(@Param("odUserId") String odUserId, @Param("nation") String nation);
    
    int playerNationExists(@Param("odUserId") String odUserId);
    
    // 玩家军功
    Integer findPlayerMerit(@Param("odUserId") String odUserId);
    
    void upsertPlayerMerit(@Param("odUserId") String odUserId, @Param("merit") int merit);
}
