package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.EquipmentPreMapper;
import com.tencent.wxcloudrun.model.EquipmentPre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class EquipmentPreRepository {

    @Autowired
    private EquipmentPreMapper mapper;

    private Map<Integer, EquipmentPre> cache = new HashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        List<EquipmentPre> all = mapper.findAll();
        cache.clear();
        if (all != null) {
            for (EquipmentPre pre : all) {
                cache.put(pre.getId(), pre);
            }
        }
    }

    public EquipmentPre findById(Integer id) {
        return cache.get(id);
    }

    public List<EquipmentPre> findAll() {
        return new ArrayList<>(cache.values());
    }

    public List<EquipmentPre> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return ids.stream()
                .map(cache::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<EquipmentPre> findByColor(Integer color) {
        return cache.values().stream()
                .filter(p -> color.equals(p.getColor()))
                .sorted(Comparator.comparingInt(EquipmentPre::getNeedLevel).thenComparingInt(EquipmentPre::getId))
                .collect(Collectors.toList());
    }

    public List<EquipmentPre> findBySuitId(Integer suitId) {
        return cache.values().stream()
                .filter(p -> suitId.equals(p.getSuitId()))
                .sorted(Comparator.comparingInt(EquipmentPre::getType))
                .collect(Collectors.toList());
    }

    public List<EquipmentPre> findByNeedLevel(Integer needLevel) {
        return cache.values().stream()
                .filter(p -> p.getNeedLevel() != null && p.getNeedLevel() <= needLevel)
                .sorted(Comparator.comparingInt(EquipmentPre::getNeedLevel).reversed()
                        .thenComparing(Comparator.comparingInt(EquipmentPre::getColor).reversed()))
                .collect(Collectors.toList());
    }

    @Deprecated
    public List<EquipmentPre> findByLevel(Integer level) {
        return findByNeedLevel(level);
    }

    public List<EquipmentPre> findBySetName(String setName) {
        return cache.values().stream()
                .filter(p -> setName.equals(p.getSuitName()))
                .collect(Collectors.toList());
    }
}
