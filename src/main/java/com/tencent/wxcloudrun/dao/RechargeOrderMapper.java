package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.RechargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RechargeOrderMapper {
    
    RechargeOrder findById(@Param("id") String id);
    
    List<RechargeOrder> findByUserId(@Param("odUserId") String odUserId);
    
    void upsert(RechargeOrder order);
}
