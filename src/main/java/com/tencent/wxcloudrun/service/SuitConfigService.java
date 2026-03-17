package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.SuitConfigMapper;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class SuitConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SuitConfigService.class);

    @Autowired
    private SuitConfigMapper suitConfigMapper;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private List<Map<String, Object>> allSuits = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            allSuits = suitConfigMapper.findAll();
            if (allSuits == null) allSuits = new ArrayList<>();
            logger.info("加载套装配置 {} 条", allSuits.size());
        } catch (Exception e) {
            logger.warn("加载suit_config失败(表可能不存在): {}", e.getMessage());
        }
    }

    public void reload() {
        init();
    }

    /**
     * 按装备的 setInfo.setId 统计各套装件数，再从 suit_config 按名称查加成
     */
    private Map<String, Integer> countEquippedSets(List<Equipment> equips) {
        Map<String, Integer> setCounts = new HashMap<>();
        for (Equipment eq : equips) {
            Equipment.SetInfo si = eq.getSetInfo();
            if (si != null && si.getSetId() != null && !si.getSetId().isEmpty()) {
                setCounts.merge(si.getSetId(), 1, Integer::sum);
            }
        }
        return setCounts;
    }

    private Map<String, Object> findSuitBySetId(String setId) {
        for (Map<String, Object> suit : allSuits) {
            String suitName = getStr(suit, "name");
            if (suitName != null && (suitName.equals(setId) || suitName.equals(setId + "套装"))) {
                return suit;
            }
        }
        return null;
    }

    /**
     * 计算武将的套装加成（从suit_config查询，按装备setId匹配套装名称）
     */
    public SuitBonus calculateSuitBonus(String generalId) {
        SuitBonus result = new SuitBonus();
        List<Equipment> equips = equipmentRepository.findEquippedByGeneralId(generalId);
        if (equips == null || equips.isEmpty()) return result;

        Map<String, Integer> setCounts = countEquippedSets(equips);

        for (Map.Entry<String, Integer> entry : setCounts.entrySet()) {
            String setId = entry.getKey();
            int count = entry.getValue();
            if (count < 3) continue;

            Map<String, Object> suit = findSuitBySetId(setId);
            if (suit == null) continue;

            String suitName = getStr(suit, "name");
            int genAtt = getInt(suit, "gen_att");
            int genDef = getInt(suit, "gen_def");
            int genFor = getInt(suit, "gen_for");
            int genLeader = getInt(suit, "gen_leader");
            int armyLife = getInt(suit, "army_life");
            int armySp = getInt(suit, "army_sp");
            int armyHit = getInt(suit, "army_hit");
            int armyMis = getInt(suit, "army_mis");

            SuitEffect effect = new SuitEffect();
            effect.suitName = suitName;
            effect.matchCount = count;
            effect.total = 6;

            effect.threeActive = true;
            result.attack += genAtt;
            result.defense += genDef;
            effect.threeDesc = buildDesc(genAtt, genDef, 0, 0, 0, 0, 0, 0);

            if (count >= 6) {
                effect.sixActive = true;
                result.valor += genFor;
                result.command += genLeader;
                result.hp += armyLife;
                result.mobility += armySp;
                result.hit += armyHit;
                result.dodge += armyMis;
                effect.sixDesc = buildSecondaryDesc(genFor, genLeader, armyLife, armySp, armyHit, armyMis);
            }

            result.activeEffects.add(effect);
        }

        return result;
    }

    /**
     * 计算武将装备+套装的总加成
     */
    public Map<String, Integer> calculateTotalEquipBonus(String generalId) {
        Map<String, Integer> bonus = new HashMap<>();
        bonus.put("attack", 0); bonus.put("defense", 0); bonus.put("valor", 0);
        bonus.put("command", 0); bonus.put("hp", 0); bonus.put("mobility", 0);
        bonus.put("dodge", 0); bonus.put("hit", 0);

        List<Equipment> equips = equipmentRepository.findEquippedByGeneralId(generalId);
        if (equips == null) return bonus;

        for (Equipment eq : equips) {
            if (eq.getBaseAttributes() != null) {
                Equipment.Attributes a = eq.getBaseAttributes();
                bonus.merge("attack", safe(a.getAttack()), Integer::sum);
                bonus.merge("defense", safe(a.getDefense()), Integer::sum);
                bonus.merge("valor", safe(a.getValor()), Integer::sum);
                bonus.merge("command", safe(a.getCommand()), Integer::sum);
                bonus.merge("hp", safe(a.getHp()), Integer::sum);
                bonus.merge("mobility", safe(a.getMobility()), Integer::sum);
                if (a.getDodge() != null) bonus.merge("dodge", a.getDodge().intValue(), Integer::sum);
            }
            if (eq.getBonusAttributes() != null) {
                Equipment.Attributes a = eq.getBonusAttributes();
                bonus.merge("attack", safe(a.getAttack()), Integer::sum);
                bonus.merge("defense", safe(a.getDefense()), Integer::sum);
                bonus.merge("valor", safe(a.getValor()), Integer::sum);
                bonus.merge("command", safe(a.getCommand()), Integer::sum);
                bonus.merge("hp", safe(a.getHp()), Integer::sum);
                bonus.merge("mobility", safe(a.getMobility()), Integer::sum);
                if (a.getDodge() != null) bonus.merge("dodge", a.getDodge().intValue(), Integer::sum);
            }
        }

        Map<String, Integer> setCounts = countEquippedSets(equips);
        for (Map.Entry<String, Integer> entry : setCounts.entrySet()) {
            String setId = entry.getKey();
            int count = entry.getValue();
            if (count < 3) continue;

            Map<String, Object> suit = findSuitBySetId(setId);
            if (suit == null) continue;

            bonus.merge("attack", getInt(suit, "gen_att"), Integer::sum);
            bonus.merge("defense", getInt(suit, "gen_def"), Integer::sum);

            if (count >= 6) {
                bonus.merge("valor", getInt(suit, "gen_for"), Integer::sum);
                bonus.merge("command", getInt(suit, "gen_leader"), Integer::sum);
                bonus.merge("hp", getInt(suit, "army_life"), Integer::sum);
                bonus.merge("mobility", getInt(suit, "army_sp"), Integer::sum);
                bonus.merge("hit", getInt(suit, "army_hit"), Integer::sum);
                bonus.merge("dodge", getInt(suit, "army_mis"), Integer::sum);
            }
        }

        return bonus;
    }

    private int safe(Integer v) { return v != null ? v : 0; }

    private String buildDesc(int att, int def, int vl, int cmd, int hp, int sp, int hit, int mis) {
        List<String> parts = new ArrayList<>();
        if (att > 0) parts.add("攻击+" + att);
        if (def > 0) parts.add("防御+" + def);
        if (vl > 0) parts.add("武力+" + vl);
        if (cmd > 0) parts.add("统帅+" + cmd);
        if (hp > 0) parts.add("士兵生命+" + hp);
        if (sp > 0) parts.add("速度+" + sp);
        if (hit > 0) parts.add("命中+" + hit);
        if (mis > 0) parts.add("闪避+" + mis);
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private String buildSecondaryDesc(int vl, int cmd, int hp, int sp, int hit, int mis) {
        List<String> parts = new ArrayList<>();
        if (vl > 0) parts.add("武力+" + vl);
        if (cmd > 0) parts.add("统帅+" + cmd);
        if (hp > 0) parts.add("士兵生命+" + hp);
        if (sp > 0) parts.add("速度+" + sp);
        if (hit > 0) parts.add("命中+" + hit);
        if (mis > 0) parts.add("闪避+" + mis);
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private int getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private String getStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    /**
     * 从 suit_config 动态获取套装的3件/6件效果描述
     * 确保描述与实际计算逻辑一致（3件=攻防, 6件=辅助属性）
     */
    public String[] getSetEffectDescriptions(String setId) {
        Map<String, Object> suit = findSuitBySetId(setId);
        if (suit == null) return new String[]{"", ""};
        int genAtt = getInt(suit, "gen_att");
        int genDef = getInt(suit, "gen_def");
        int genFor = getInt(suit, "gen_for");
        int genLeader = getInt(suit, "gen_leader");
        int armyLife = getInt(suit, "army_life");
        int armySp = getInt(suit, "army_sp");
        int armyHit = getInt(suit, "army_hit");
        int armyMis = getInt(suit, "army_mis");
        String threeDesc = buildDesc(genAtt, genDef, 0, 0, 0, 0, 0, 0);
        String sixDesc = buildSecondaryDesc(genFor, genLeader, armyLife, armySp, armyHit, armyMis);
        return new String[]{threeDesc, sixDesc};
    }

    /**
     * 修正装备列表中所有套装描述，使用 suit_config 数据覆盖
     */
    public void fixSetDescriptions(List<Equipment> equips) {
        if (equips == null) return;
        Map<String, String[]> descCache = new HashMap<>();
        for (Equipment eq : equips) {
            Equipment.SetInfo si = eq.getSetInfo();
            if (si == null || si.getSetId() == null || si.getSetId().isEmpty()) continue;
            String setId = si.getSetId();
            String[] descs = descCache.computeIfAbsent(setId, this::getSetEffectDescriptions);
            si.setThreeSetEffect(descs[0] != null && !descs[0].equals("-") ? descs[0] : "");
            si.setSixSetEffect(descs[1] != null && !descs[1].equals("-") ? descs[1] : "");
        }
    }

    public static class SuitBonus {
        public int attack, defense, valor, command, hp, mobility, dodge, hit;
        public List<SuitEffect> activeEffects = new ArrayList<>();
    }

    public static class SuitEffect {
        public String suitName;
        public int matchCount, total;
        public boolean threeActive, sixActive;
        public String threeDesc, sixDesc;
    }
}
