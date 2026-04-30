package com.tencent.wxcloudrun.service.simulation;

import com.tencent.wxcloudrun.dao.SimulationConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationConfigService {

    private final SimulationConfigMapper simulationConfigMapper;

    public SimulationConfigService(SimulationConfigMapper simulationConfigMapper) {
        this.simulationConfigMapper = simulationConfigMapper;
    }

    public Map<Integer, Map<String, Object>> loadProfileMap() {
        List<Map<String, Object>> list = simulationConfigMapper.findAllServerProfiles();
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : list) {
            int serverId = intVal(row.get("serverId"), 0);
            if (serverId <= 0) continue;
            result.put(serverId, row);
        }
        return result;
    }

    public Map<String, Object> getServerConfig(int serverId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverId", serverId);
        result.put("profile", simulationConfigMapper.findServerProfile(serverId));
        result.put("chatTemplates", simulationConfigMapper.findChatTemplatesByServer(serverId));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertServerConfig(int serverId, Map<String, Object> profile, List<Map<String, Object>> chatTemplates) {
        long now = System.currentTimeMillis();

        if (profile != null && !profile.isEmpty()) {
            simulationConfigMapper.upsertServerProfile(
                    serverId,
                    str(profile.get("activityProfile"), "medium"),
                    doubleObj(profile.get("pveMultiplier"), 1.0),
                    doubleObj(profile.get("pvpMultiplier"), 1.0),
                    doubleObj(profile.get("economyMultiplier"), 1.0),
                    doubleObj(profile.get("productionMultiplier"), 1.0),
                    doubleObj(profile.get("socialMultiplier"), 1.0),
                    doubleObj(profile.get("growthMultiplier"), 1.0),
                    doubleObj(profile.get("chatMultiplier"), 1.0),
                    doubleObj(profile.get("daytimeProductionMultiplier"), 1.35),
                    doubleObj(profile.get("nightPvpMultiplier"), 1.45),
                    boolObj(profile.get("enabled"), true),
                    now
            );
        }

        if (chatTemplates != null) {
            simulationConfigMapper.deleteChatTemplatesByServer(serverId);
            for (Map<String, Object> row : chatTemplates) {
                String content = str(row.get("content"), "");
                if (content.isEmpty()) continue;
                simulationConfigMapper.insertChatTemplate(
                        serverId,
                        str(row.get("channel"), "world"),
                        content,
                        doubleObj(row.get("weight"), 1.0),
                        boolObj(row.get("enabled"), true),
                        now
                );
            }
        }

        return getServerConfig(serverId);
    }

    public void seedDefaultChatTemplatesIfMissing(int serverId, String[] templates) {
        List<Map<String, Object>> existing = simulationConfigMapper.findChatTemplatesByServer(serverId);
        if (existing != null && !existing.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (String t : templates) {
            if (t == null || t.trim().isEmpty()) continue;
            simulationConfigMapper.insertChatTemplate(serverId, "world", t.trim(), 1.0, true, now);
        }
    }

    public List<String> loadEnabledWorldTemplates(int serverId) {
        List<Map<String, Object>> rows = simulationConfigMapper.findChatTemplatesByServer(serverId);
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (!boolVal(row.get("enabled"), true)) continue;
            String channel = str(row.get("channel"), "world");
            if (!"world".equalsIgnoreCase(channel)) continue;
            String content = str(row.get("content"), "");
            if (!content.isEmpty()) result.add(content);
        }
        return result;
    }

    private int intVal(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private String str(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? def : s;
    }

    private Boolean boolObj(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean) return (Boolean) v;
        String s = String.valueOf(v).trim();
        if ("1".equals(s) || "true".equalsIgnoreCase(s)) return true;
        if ("0".equals(s) || "false".equalsIgnoreCase(s)) return false;
        return def;
    }

    private boolean boolVal(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean) return (Boolean) v;
        String s = String.valueOf(v);
        if ("1".equals(s) || "true".equalsIgnoreCase(s)) return true;
        if ("0".equals(s) || "false".equalsIgnoreCase(s)) return false;
        return def;
    }

    private Double doubleObj(Object v, double def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
