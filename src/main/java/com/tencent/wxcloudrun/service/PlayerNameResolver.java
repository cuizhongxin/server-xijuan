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
        if (compositeUserId == null || compositeUserId.isEmpty()) return DEFAULT_NAME;
        try {
            int idx = compositeUserId.lastIndexOf('_');
            if (idx > 0) {
                String rawUserId = compositeUserId.substring(0, idx);
                int serverId = Integer.parseInt(compositeUserId.substring(idx + 1));
                Map<String, Object> ps = gameServerMapper.findPlayerServer(rawUserId, serverId);
                if (ps != null && ps.get("lordName") != null) {
                    String name = ps.get("lordName").toString();
                    if (!name.isEmpty()) return name;
                }
            }
            List<Map<String, Object>> servers = gameServerMapper.findPlayerServers(compositeUserId);
            if (servers != null && !servers.isEmpty()) {
                Object lordName = servers.get(0).get("lordName");
                if (lordName != null && !lordName.toString().isEmpty()) {
                    return lordName.toString();
                }
            }
        } catch (Exception e) {
            logger.debug("解析主公名称失败: {}", compositeUserId);
        }
        return DEFAULT_NAME;
    }
}
