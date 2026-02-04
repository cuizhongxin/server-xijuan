package com.tencent.wxcloudrun.repository;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户材料数据仓库（内存存储）
 */
@Repository
public class UserMaterialRepository {
    
    // 使用ConcurrentHashMap存储用户材料数据
    // key: userId, value: Map<materialId, count>
    private final Map<String, Map<String, Integer>> materialStore = new ConcurrentHashMap<>();
    
    /**
     * 获取用户的所有材料
     */
    public Map<String, Integer> getUserMaterials(String userId) {
        return materialStore.getOrDefault(userId, new HashMap<>());
    }
    
    /**
     * 获取用户指定材料的数量
     */
    public int getMaterialCount(String userId, String materialId) {
        Map<String, Integer> userMaterials = materialStore.get(userId);
        if (userMaterials == null) {
            return 0;
        }
        return userMaterials.getOrDefault(materialId, 0);
    }
    
    /**
     * 增加材料
     */
    public void addMaterial(String userId, String materialId, int count) {
        materialStore.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        Map<String, Integer> userMaterials = materialStore.get(userId);
        userMaterials.merge(materialId, count, Integer::sum);
    }
    
    /**
     * 减少材料
     */
    public boolean consumeMaterial(String userId, String materialId, int count) {
        Map<String, Integer> userMaterials = materialStore.get(userId);
        if (userMaterials == null) {
            return false;
        }
        
        int currentCount = userMaterials.getOrDefault(materialId, 0);
        if (currentCount < count) {
            return false;
        }
        
        userMaterials.put(materialId, currentCount - count);
        return true;
    }
    
    /**
     * 批量消耗材料
     */
    public boolean consumeMaterials(String userId, Map<String, Integer> materials) {
        // 先检查所有材料是否足够
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (getMaterialCount(userId, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        
        // 消耗材料
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            consumeMaterial(userId, entry.getKey(), entry.getValue());
        }
        
        return true;
    }
    
    /**
     * 设置用户材料数量
     */
    public void setMaterial(String userId, String materialId, int count) {
        materialStore.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        materialStore.get(userId).put(materialId, count);
    }
    
    /**
     * 初始化用户材料（新用户）
     */
    public void initUserMaterials(String userId) {
        Map<String, Integer> materials = new ConcurrentHashMap<>();
        
        // 初始材料
        materials.put("WOOD_COMMON", 100);
        materials.put("METAL_IRON", 100);
        materials.put("PAPER_COMMON", 50);
        materials.put("CLOTH_COTTON", 50);
        materials.put("LEATHER_COMMON", 30);
        materials.put("GEM_JADE", 10);
        
        materialStore.put(userId, materials);
    }
    
    /**
     * 删除用户所有材料
     */
    public void deleteByUserId(String userId) {
        materialStore.remove(userId);
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        materialStore.clear();
    }
}


