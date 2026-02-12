package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DungeonProgressMapper {
    
    String findByUserIdAndDungeonId(@Param("userId") String userId, @Param("dungeonId") String dungeonId);
    
    List<Map<String, Object>> findByUserId(@Param("userId") String userId);
    
    void upsert(@Param("userId") String userId, @Param("dungeonId") String dungeonId,
                @Param("data") String data, @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);
    
    void deleteByUserId(@Param("userId") String userId);
}
