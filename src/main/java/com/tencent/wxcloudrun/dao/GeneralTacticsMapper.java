package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface GeneralTacticsMapper {
    
    Map<String, Object> findByGeneralId(@Param("generalId") String generalId);
    
    void upsert(@Param("generalId") String generalId, @Param("learnedData") String learnedData,
                @Param("equippedData") String equippedData);
}
