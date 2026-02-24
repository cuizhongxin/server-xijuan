package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.SecretRealmConfigMapper;
import com.tencent.wxcloudrun.dao.SecretRealmRewardMapper;
import com.tencent.wxcloudrun.dao.SecretRealmPityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Repository
public class SecretRealmConfigRepository {

    @Autowired
    private SecretRealmConfigMapper configMapper;

    @Autowired
    private SecretRealmRewardMapper rewardMapper;

    @Autowired
    private SecretRealmPityMapper pityMapper;

    // ==================== 秘境配置 ====================

    public List<Map<String, Object>> findAllRealms() {
        return configMapper.findAll();
    }

    public Map<String, Object> findRealmById(String id) {
        return configMapper.findById(id);
    }

    // ==================== 奖励配置 ====================

    public List<Map<String, Object>> findRewardsByRealmId(String realmId) {
        return rewardMapper.findByRealmId(realmId);
    }

    public List<Map<String, Object>> findEquipmentsByRealmId(String realmId) {
        return rewardMapper.findByRealmIdAndType(realmId, "equipment");
    }

    public List<Map<String, Object>> findItemsByRealmId(String realmId) {
        return rewardMapper.findByRealmIdAndType(realmId, "item");
    }

    // ==================== 保底计数 ====================

    public Map<String, Object> findPity(String userId, String realmId) {
        return pityMapper.findByUserAndRealm(userId, realmId);
    }

    public void savePity(String userId, String realmId,
                         int countSinceEquip, int totalExploreCount, int totalEquipCount,
                         long lastEquipTime, int dailyCount) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        pityMapper.upsert(userId, realmId, countSinceEquip, totalExploreCount,
                totalEquipCount, lastEquipTime, dailyCount, today, System.currentTimeMillis());
    }

    // ==================== 工具方法 ====================

    public static int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Long) return ((Long) v).intValue();
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    public static long getLong(Map<String, Object> m, String key, long def) {
        Object v = m.get(key);
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        if (v instanceof Number) return ((Number) v).longValue();
        return def;
    }

    public static double getDouble(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        if (v instanceof Double) return (Double) v;
        if (v instanceof BigDecimal) return ((BigDecimal) v).doubleValue();
        if (v instanceof Number) return ((Number) v).doubleValue();
        return def;
    }

    public static String getString(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : def;
    }
}
