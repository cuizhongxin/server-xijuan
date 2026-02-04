package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.Formation;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阵型数据仓库
 */
@Repository
public class FormationRepository {
    
    private final Map<String, Formation> formationStorage = new ConcurrentHashMap<>();
    
    /**
     * 根据用户ID查找阵型
     */
    public Formation findByUserId(String odUserId) {
        return formationStorage.values().stream()
            .filter(f -> odUserId.equals(f.getOdUserId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 根据ID查找阵型
     */
    public Formation findById(String id) {
        return formationStorage.get(id);
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
        
        formationStorage.put(formationId, formation);
        return formation;
    }
    
    /**
     * 保存阵型
     */
    public Formation save(Formation formation) {
        formation.setUpdateTime(System.currentTimeMillis());
        formationStorage.put(formation.getId(), formation);
        return formation;
    }
}

