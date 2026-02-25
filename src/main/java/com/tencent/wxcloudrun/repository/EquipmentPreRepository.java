package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.EquipmentPreMapper;
import com.tencent.wxcloudrun.model.EquipmentPre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 装备模板仓库 - 启动时加载全部模板到内存缓存
 */
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

    public List<EquipmentPre> findBySource(String source) {
        return cache.values().stream()
                .filter(p -> source.equals(p.getSource()))
                .collect(Collectors.toList());
    }

    public List<EquipmentPre> findByLevel(Integer level) {
        return cache.values().stream()
                .filter(p -> level.equals(p.getLevel()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有可手工制作的装备模板
     */
    public List<EquipmentPre> findCraftable() {
        return cache.values().stream()
                .filter(p -> "手工制作".equals(p.getSource()))
                .sorted(Comparator.comparingInt(EquipmentPre::getLevel).thenComparingInt(EquipmentPre::getId))
                .collect(Collectors.toList());
    }

    /**
     * 按套装名获取模板
     */
    public List<EquipmentPre> findBySetName(String setName) {
        return cache.values().stream()
                .filter(p -> setName.equals(p.getSetName()))
                .collect(Collectors.toList());
    }
}
