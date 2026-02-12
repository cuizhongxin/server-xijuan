package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FormationMapper {
    
    String findById(@Param("id") String id);
    
    String findByUserId(@Param("odUserId") String odUserId);
    
    void upsert(@Param("id") String id, @Param("odUserId") String odUserId, @Param("data") String data,
                @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);
}
