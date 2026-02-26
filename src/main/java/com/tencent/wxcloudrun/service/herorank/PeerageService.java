package com.tencent.wxcloudrun.service.herorank;

import com.tencent.wxcloudrun.dao.PeerageConfigMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PeerageService {

    private static final Logger logger = LoggerFactory.getLogger(PeerageService.class);

    @Autowired
    private PeerageConfigMapper peerageConfigMapper;

    @Autowired
    private UserResourceService resourceService;

    @Autowired
    private GeneralRepository generalRepository;

    private List<Map<String, Object>> peerageConfigs = new ArrayList<>();
    private List<Map<String, Object>> soldierTiers = new ArrayList<>();

    @PostConstruct
    public void init() {
        peerageConfigs = peerageConfigMapper.findAllPeerage();
        soldierTiers = peerageConfigMapper.findAllSoldierTiers();
        logger.info("加载爵位配置 {} 条, 兵种配置 {} 条", peerageConfigs.size(), soldierTiers.size());
    }

    /**
     * 获取爵位+兵种升级信息
     */
    public Map<String, Object> getPeerageInfo(String userId) {
        UserResource res = resourceService.getUserResource(userId);
        int level = res != null && res.getLevel() != null ? res.getLevel() : 1;
        long fame = res != null && res.getFame() != null ? res.getFame() : 0;
        String currentRank = res != null && res.getRank() != null ? res.getRank() : "白身";

        Map<String, Object> currentPeerage = findPeerageByRank(currentRank);
        Map<String, Object> nextPeerage = findNextPeerage(currentRank);

        int maxTier = currentPeerage != null ? getInt(currentPeerage, "maxSoldierTier", 1) : 1;

        Map<String, Object> result = new HashMap<>();
        result.put("fame", fame);
        result.put("level", level);
        result.put("currentRank", currentRank);
        result.put("currentPeerage", currentPeerage);
        result.put("nextPeerage", nextPeerage);
        result.put("maxSoldierTier", maxTier);
        result.put("peerageList", peerageConfigs);

        // 按兵种大类分组返回可用兵种
        Map<String, List<Map<String, Object>>> tiersByCategory = new LinkedHashMap<>();
        for (String cat : Arrays.asList("步", "骑", "弓")) {
            List<Map<String, Object>> tiers = soldierTiers.stream()
                    .filter(t -> cat.equals(t.get("troopCategory")))
                    .map(t -> {
                        Map<String, Object> copy = new HashMap<>(t);
                        int tier = getInt(t, "tier", 1);
                        copy.put("unlocked", tier <= maxTier);
                        return copy;
                    })
                    .collect(Collectors.toList());
            tiersByCategory.put(cat, tiers);
        }
        result.put("soldierTiers", tiersByCategory);

        return result;
    }

    /**
     * 升级武将兵种
     */
    public Map<String, Object> upgradeSoldierTier(String userId, String generalId, int targetTier, String troopCategory) {
        UserResource res = resourceService.getUserResource(userId);
        String currentRank = res != null && res.getRank() != null ? res.getRank() : "白身";
        Map<String, Object> peerage = findPeerageByRank(currentRank);
        int maxTier = peerage != null ? getInt(peerage, "maxSoldierTier", 1) : 1;

        if (targetTier > maxTier) {
            throw new BusinessException(400, "爵位不足，当前爵位最高可升级到" + maxTier + "阶兵种");
        }

        // 找到目标兵种配置
        Map<String, Object> tierConfig = soldierTiers.stream()
                .filter(t -> getInt(t, "tier", 0) == targetTier
                        && troopCategory.equals(t.get("troopCategory")))
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, "无效的兵种配置"));

        long cost = getLong(tierConfig, "upgradeSilver", 0);
        if (cost > 0 && !resourceService.consumeSilver(userId, cost)) {
            throw new BusinessException(400, "白银不足，需要" + cost + "白银");
        }

        // 更新武将的士兵信息（打平字段）
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new BusinessException(400, "武将不存在");
        }
        if (!userId.equals(general.getUserId())) {
            throw new BusinessException(400, "只能升级自己的武将");
        }

        general.setSoldierRank(targetTier);
        general.setUpdateTime(System.currentTimeMillis());

        generalRepository.save(general);

        logger.info("用户 {} 武将 {} 兵种升级到 {}阶 {}, 消耗白银 {}",
                userId, general.getName(), targetTier, tierConfig.get("name"), cost);

        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("newTier", targetTier);
        result.put("tierName", tierConfig.get("name"));
        result.put("tierIcon", tierConfig.get("icon"));
        result.put("powerMultiplier", tierConfig.get("powerMultiplier"));
        result.put("cost", cost);
        return result;
    }

    // ==== 内部工具 ====

    private Map<String, Object> findPeerageByRank(String rank) {
        return peerageConfigs.stream()
                .filter(p -> rank.equals(p.get("rankName")))
                .findFirst().orElse(null);
    }

    private Map<String, Object> findNextPeerage(String currentRank) {
        boolean found = false;
        for (Map<String, Object> p : peerageConfigs) {
            if (found) return p;
            if (currentRank.equals(p.get("rankName"))) found = true;
        }
        return null;
    }

    private int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    private long getLong(Map<String, Object> m, String key, long def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return def;
    }

    private double getDouble(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return def;
    }
}
