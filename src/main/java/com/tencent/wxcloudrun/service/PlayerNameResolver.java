package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.GameServerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 统一从 user_server.lord_name 解析玩家显示名称。
 * 供 BossWar、Plunder、Supply、Market、Alliance、NationWar 等模块共用。
 */
@Component
public class PlayerNameResolver {

    private static final Logger logger = LoggerFactory.getLogger(PlayerNameResolver.class);
    private static final String DEFAULT_NAME = "君主";

    @Autowired
    private GameServerMapper gameServerMapper;

    /**
     * 从复合 userId（格式 rawUserId_serverId）解析主公名称。
     * 优先拆分复合ID精确查询，失败时尝试用原始ID模糊匹配。
     */
    public String resolve(String compositeUserId) {
        if (compositeUserId == null) return DEFAULT_NAME;
        compositeUserId = compositeUserId.trim();
        if (compositeUserId.isEmpty() || "null".equalsIgnoreCase(compositeUserId)) return DEFAULT_NAME;
        try {
            int idx = compositeUserId.lastIndexOf('_');
            String rawUserId = compositeUserId;
            if (idx > 0) {
                rawUserId = compositeUserId.substring(0, idx);
                String serverPart = compositeUserId.substring(idx + 1);
                if (isNumeric(serverPart)) {
                    int serverId = Integer.parseInt(serverPart);
                    Map<String, Object> ps = gameServerMapper.findPlayerServer(rawUserId, serverId);
                    String name = extractLordName(ps);
                    if (isValidName(name)) return name;
                }
            }

            String name = firstLordName(gameServerMapper.findPlayerServers(compositeUserId));
            if (isValidName(name)) return name;

            if (!rawUserId.equals(compositeUserId)) {
                name = firstLordName(gameServerMapper.findPlayerServers(rawUserId));
                if (isValidName(name)) return name;
            }
        } catch (Exception e) {
            logger.debug("解析主公名称失败: {}", compositeUserId);
        }
        return DEFAULT_NAME;
    }

    private String firstLordName(List<Map<String, Object>> servers) {
        if (servers == null || servers.isEmpty()) return null;
        for (Map<String, Object> server : servers) {
            String name = extractLordName(server);
            if (isValidName(name)) return name;
        }
        return null;
    }

    private String extractLordName(Map<String, Object> row) {
        if (row == null) return null;
        Object lordName = row.get("lordName");
        return lordName == null ? null : String.valueOf(lordName).trim();
    }

    private boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty()
                && !"null".equalsIgnoreCase(name.trim())
                && !DEFAULT_NAME.equals(name.trim());
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }
}
