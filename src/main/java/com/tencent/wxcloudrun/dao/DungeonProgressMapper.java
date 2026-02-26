package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.DungeonProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DungeonProgressMapper {
    
    DungeonProgress findByUserIdAndDungeonId(@Param("userId") String userId, @Param("dungeonId") String dungeonId);
    
    List<DungeonProgress> findByUserId(@Param("userId") String userId);
    
    void upsert(DungeonProgress progress);
    
    void deleteByUserId(@Param("userId") String userId);
}
