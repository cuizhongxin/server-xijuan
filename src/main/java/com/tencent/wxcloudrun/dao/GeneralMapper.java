package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.General;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GeneralMapper {
    
    General findById(@Param("id") String id);
    
    List<General> findByUserId(@Param("userId") String userId);
    
    void upsert(General general);
    
    void deleteById(@Param("id") String id);
    
    void deleteByUserId(@Param("userId") String userId);
    
    int countByUserId(@Param("userId") String userId);
}
