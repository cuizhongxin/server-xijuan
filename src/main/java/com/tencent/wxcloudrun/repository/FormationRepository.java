package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.FormationMapper;
import com.tencent.wxcloudrun.model.Formation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 阵型数据仓库（数据库存储）
 */
@Repository
public class FormationRepository {
    
    @Autowired
    private FormationMapper formationMapper;
    
    /**
     * 根据用户ID查找阵型
     */
    public Formation findByUserId(String odUserId) {
        return formationMapper.findByUserId(odUserId);
    }
    
    /**
     * 根据ID查找阵型
     */
    public Formation findById(String id) {
        return formationMapper.findById(id);
    }
    
    /**
     * 初始化用户阵型（6个空槽位）
     */
    public Formation initFormation(String odUserId) {
        String formationId = "formation_" + UUID.randomUUID().toString().substring(0, 8);
        
        List<Formation.FormationSlot> slots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            slots.add(Formation.FormationSlot.builder()
                .index(i)
                .generalId(null)
                .generalName(null)
                .quality(null)
                .avatar(null)
                .mobility(0)
                .build());
        }
        
        Formation formation = Formation.builder()
            .id(formationId)
            .odUserId(odUserId)
            .name("默认阵型")
            .slots(slots)
            .active(true)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
        
        formationMapper.upsertFormation(formation);
        formationMapper.insertSlots(formationId, slots);
        return formation;
    }
    
    /**
     * 保存阵型
     */
    public Formation save(Formation formation) {
        formation.setUpdateTime(System.currentTimeMillis());
        formationMapper.upsertFormation(formation);
        formationMapper.deleteSlotsByFormationId(formation.getId());
        if (formation.getSlots() != null && !formation.getSlots().isEmpty()) {
            formationMapper.insertSlots(formation.getId(), formation.getSlots());
        }
        return formation;
    }
}
