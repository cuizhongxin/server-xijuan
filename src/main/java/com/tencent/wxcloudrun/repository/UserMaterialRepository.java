package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.UserMaterialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户材料数据仓库（数据库存储）
 */
@Repository
public class UserMaterialRepository {
    
    @Autowired
    private UserMaterialMapper userMaterialMapper;
    
    /**
     * 获取用户的所有材料
     */
    public Map<String, Integer> getUserMaterials(String userId) {
        List<Map<String, Object>> rows = userMaterialMapper.findByUserId(userId);
        Map<String, Integer> result = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String materialId = (String) row.get("materialId");
                Integer count = ((Number) row.get("count")).intValue();
                result.put(materialId, count);
            }
        }
        return result;
    }
    
    /**
     * 获取用户指定材料的数量
     */
    public int getMaterialCount(String userId, String materialId) {
        Integer count = userMaterialMapper.findCount(userId, materialId);
        return count != null ? count : 0;
    }
    
    /**
     * 增加材料
     */
    public void addMaterial(String userId, String materialId, int count) {
        int currentCount = getMaterialCount(userId, materialId);
        userMaterialMapper.upsert(userId, materialId, currentCount + count);
    }
    
    /**
     * 减少材料
     */
    public boolean consumeMaterial(String userId, String materialId, int count) {
        int currentCount = getMaterialCount(userId, materialId);
        if (currentCount < count) {
            return false;
        }
        userMaterialMapper.upsert(userId, materialId, currentCount - count);
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
        userMaterialMapper.upsert(userId, materialId, count);
    }
    
    /**
     * 初始化用户材料（新用户）
     */
    public void initUserMaterials(String userId) {
        userMaterialMapper.upsert(userId, "WOOD_COMMON", 100);
        userMaterialMapper.upsert(userId, "METAL_IRON", 100);
        userMaterialMapper.upsert(userId, "PAPER_COMMON", 50);
        userMaterialMapper.upsert(userId, "CLOTH_COTTON", 50);
        userMaterialMapper.upsert(userId, "LEATHER_COMMON", 30);
        userMaterialMapper.upsert(userId, "GEM_JADE", 10);
    }
    
    /**
     * 删除用户所有材料
     */
    public void deleteByUserId(String userId) {
        userMaterialMapper.deleteByUserId(userId);
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        // 数据库模式下不支持清空全表，忽略此操作
    }
}
