package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Production;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductionMapper {
    
    Production findByUserId(@Param("odUserId") String odUserId);
    
    void upsert(Production production);
}
