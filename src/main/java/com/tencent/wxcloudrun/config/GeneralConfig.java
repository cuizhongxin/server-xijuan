package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.dao.GeneralQualityMapper;
import com.tencent.wxcloudrun.dao.GeneralSlotTraitMapper;
import com.tencent.wxcloudrun.dao.GeneralTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 武将配置 - 从数据库加载品质与模板（槽位 + 模板 + 特征）
 */
@Component
public class GeneralConfig {

    @Autowired
    private GeneralQualityMapper generalQualityMapper;
    @Autowired
    private GeneralTemplateMapper generalTemplateMapper;
    @Autowired
    private GeneralSlotTraitMapper generalSlotTraitMapper;

    /** 品质定义（启动时从 general_quality 加载并缓存） */
    private final Map<String, Quality> qualities = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        loadQualities();
    }

    private void loadQualities() {
        List<Map<String, Object>> rows = generalQualityMapper.findAll();
        if (rows == null) return;
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("code");
            String name = (String) row.get("name");
            String color = (String) row.get("color");
            Number mult = (Number) row.get("attrMultiplier");
            Number count = (Number) row.get("traitCount");
            qualities.put(code, new Quality(
                    code,
                    name != null ? name : "",
                    color != null ? color : "",
                    mult != null ? mult.doubleValue() : 1.0,
                    count != null ? count.intValue() : 0
            ));
        }
    }

    /**
     * 获取品质配置（与原有静态 QUALITIES 行为一致，供 RecruitService 等使用）
     */
    public Map<String, Quality> getQualities() {
        if (qualities.isEmpty()) {
            loadQualities();
        }
        return qualities;
    }

    /**
     * 按品质获取该品质下所有将领模板（从数据库 general_template + general_slot + general_slot_trait 查询）
     */
    public List<GeneralTemplate> getAllGeneralsByQuality(String quality) {
        List<Map<String, Object>> rows = generalTemplateMapper.listByQualityCode(quality);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return buildGeneralTemplates(rows);
    }

    /**
     * 按品质与可招募势力获取模板（本国 + 群 + 虚构），用于按国家限制招募
     */
    public List<GeneralTemplate> getRecruitableGeneralsByQuality(String quality, String playerFaction) {
        List<String> factions = Arrays.asList(playerFaction, "群", "虚构");
        List<Map<String, Object>> rows = generalTemplateMapper.listRecruitableByQualityAndFaction(quality, factions);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return buildGeneralTemplates(rows);
    }

    private List<GeneralTemplate> buildGeneralTemplates(List<Map<String, Object>> rows) {
        Set<Integer> slotIds = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object sid = row.get("slotId");
            if (sid != null) slotIds.add(((Number) sid).intValue());
        }
        List<Integer> slotIdList = new ArrayList<>(slotIds);
        List<Map<String, Object>> traitRows = generalSlotTraitMapper.findBySlotIds(slotIdList);
        Map<Integer, List<Trait>> traitsBySlotId = new HashMap<>();
        for (Map<String, Object> tr : traitRows != null ? traitRows : Collections.<Map<String, Object>>emptyList()) {
            Object sid = tr.get("slotId");
            String traitType = (String) tr.get("traitType");
            String traitValue = (String) tr.get("traitValue");
            if (sid == null || traitType == null) continue;
            int slotId = ((Number) sid).intValue();
            traitsBySlotId.computeIfAbsent(slotId, k -> new ArrayList<>()).add(parseTrait(traitType, traitValue));
        }

        List<GeneralTemplate> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("name");
            String faction = (String) row.get("faction");
            Object sid = row.get("slotId");
            String qualityCode = (String) row.get("qualityCode");
            String type = (String) row.get("type");
            String troopType = (String) row.get("troopType");
            if (name == null || qualityCode == null || type == null) continue;
            int slotId = sid != null ? ((Number) sid).intValue() : 0;
            List<Trait> traits = traitsBySlotId.getOrDefault(slotId, Collections.emptyList());
            result.add(new GeneralTemplate(name, qualityCode, faction != null ? faction : "群", type, troopType, traits));
        }
        return result;
    }

    private static Trait parseTrait(String traitType, String traitValue) {
        if (traitValue == null) traitValue = "";
        if ("special".equals(traitType)) {
            return new Trait(traitType, traitValue);
        }
        try {
            return new Trait(traitType, Integer.parseInt(traitValue.trim()));
        } catch (NumberFormatException e) {
            return new Trait(traitType, traitValue);
        }
    }

    // --------------- 内部类（保持与 RecruitService 等调用方兼容） ---------------

    public static class Quality {
        public String id;
        public String name;
        public String color;
        public double attrMultiplier;
        public int traitCount;

        public Quality(String id, String name, String color, double attrMultiplier, int traitCount) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.attrMultiplier = attrMultiplier;
            this.traitCount = traitCount;
        }
    }

    public static class GeneralTemplate {
        public String name;
        public String quality;
        public String faction;
        public String type;
        /** 兵种：步/骑/弓，来自槽位，招募时用此替代随机兵种 */
        public String troopType;
        public List<Trait> traits;

        public GeneralTemplate(String name, String quality, String faction, String type, List<Trait> traits) {
            this.name = name;
            this.quality = quality;
            this.faction = faction;
            this.type = type;
            this.troopType = null;
            this.traits = traits;
        }

        public GeneralTemplate(String name, String quality, String faction, String type, String troopType, List<Trait> traits) {
            this.name = name;
            this.quality = quality;
            this.faction = faction;
            this.type = type;
            this.troopType = troopType;
            this.traits = traits;
        }
    }

    public static class Trait {
        public String type;
        public Object value;

        public Trait(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
}
