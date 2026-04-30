package com.tencent.wxcloudrun.service.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PlayerSimulationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PlayerSimulationScheduler.class);

    private final PlayerSimulationService playerSimulationService;

    @Value("${simulation.players.enabled:false}")
    private boolean enabled;

    @Value("${simulation.players.max-players-per-tick:30}")
    private int maxPlayersPerTick;

    @Value("${simulation.players.include-war-modules:true}")
    private boolean includeWarModules;

    @Value("${simulation.players.activity-profile:medium}")
    private String activityProfile;

    @Value("${simulation.players.server-weights:}")
    private String serverWeightsRaw;

    public PlayerSimulationScheduler(PlayerSimulationService playerSimulationService) {
        this.playerSimulationService = playerSimulationService;
    }

    @Scheduled(cron = "${simulation.players.cron:0 */10 * * * ?}", zone = "Asia/Shanghai")
    public void simulateTick() {
        if (!enabled) return;
        try {
            Map<String, Object> result = playerSimulationService.runSimulationOnce(
                    maxPlayersPerTick,
                    includeWarModules,
                    activityProfile,
                    parseServerWeights(serverWeightsRaw)
            );
            logger.info("[玩家模拟] 本轮执行完成: {}", result);
        } catch (Exception e) {
            logger.error("[玩家模拟] 本轮执行失败", e);
        }
    }

    private Map<Integer, Double> parseServerWeights(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyMap();
        Map<Integer, Double> result = new LinkedHashMap<>();
        String[] pairs = raw.split(",");
        for (String pair : pairs) {
            if (pair == null || pair.trim().isEmpty()) continue;
            String[] kv = pair.trim().split("[:=]");
            if (kv.length != 2) continue;
            try {
                int serverId = Integer.parseInt(kv[0].trim());
                double weight = Double.parseDouble(kv[1].trim());
                if (weight > 0) result.put(serverId, weight);
            } catch (Exception ignore) {
                // ignore malformed config
            }
        }
        return result;
    }
}
