package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Formation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FormationMapper {
    
    Formation findById(@Param("id") String id);
    
    Formation findByUserId(@Param("odUserId") String odUserId);
    
    void upsertFormation(Formation formation);
    
    void deleteSlotsByFormationId(@Param("formationId") String formationId);
    
    void insertSlots(@Param("formationId") String formationId, @Param("slots") List<Formation.FormationSlot> slots);
}
