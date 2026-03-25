package com.tencent.wxcloudrun.service.story;

import com.tencent.wxcloudrun.dao.StoryProgressMapper;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.EquipmentPre;
import com.tencent.wxcloudrun.model.StoryProgress;
import com.tencent.wxcloudrun.repository.EquipmentPreRepository;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.service.equipment.EquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StoryService {

    private static final int FIRST_NODE = 110;

    /**
     * 剧情推进到指定节点时赠送的装备 (nodeId → equipPreId)
     * 1046: 黑铁剑 (玩家刚看完1045的赠剑对话)
     * 1155: 黑铁锴 (玩家刚看完1151的缴获铠甲对话)
     */
    private static final Map<Integer, Integer> STORY_REWARD_MAP = new HashMap<>();
    static {
        STORY_REWARD_MAP.put(1046, 22001); // 黑铁剑
        STORY_REWARD_MAP.put(1155, 22004); // 黑铁锴
    }

    @Autowired private StoryProgressMapper storyProgressMapper;
    @Autowired private EquipmentPreRepository equipmentPreRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private EquipmentService equipmentService;

    public Map<String, Object> getProgress(String userId, int serverId) {
        StoryProgress progress = storyProgressMapper.findByUserAndServer(userId, serverId);
        Map<String, Object> result = new HashMap<>();
        if (progress == null) {
            result.put("currentNode", FIRST_NODE);
            result.put("completed", false);
        } else {
            result.put("currentNode", progress.getCurrentNode());
            result.put("completed", Boolean.TRUE.equals(progress.getCompleted()));
        }
        return result;
    }

    public Map<String, Object> advance(String userId, int serverId, int nodeId) {
        StoryProgress progress = getOrCreate(userId, serverId);

        if (Boolean.TRUE.equals(progress.getCompleted())) {
            Map<String, Object> result = new HashMap<>();
            result.put("currentNode", 0);
            result.put("completed", true);
            return result;
        }

        int cur = progress.getCurrentNode() != null ? progress.getCurrentNode() : 0;
        if (nodeId <= cur && cur > 0) {
            log.info("引导进度不回退: userId={}, serverId={}, cur={}, requested={}", userId, serverId, cur, nodeId);
            Map<String, Object> result = new HashMap<>();
            result.put("currentNode", cur);
            result.put("completed", false);
            return result;
        }

        String rewardName = null;
        Integer rewardPreId = STORY_REWARD_MAP.get(nodeId);
        if (rewardPreId != null) {
            rewardName = grantEquipReward(userId + "_" + serverId, rewardPreId);
        }

        progress.setCurrentNode(nodeId);
        progress.setUpdateTime(System.currentTimeMillis());
        storyProgressMapper.update(progress);

        Map<String, Object> result = new HashMap<>();
        result.put("currentNode", nodeId);
        result.put("completed", false);
        if (rewardName != null) {
            result.put("rewardEquip", rewardName);
        }
        return result;
    }

    private String grantEquipReward(String gameUserId, int equipPreId) {
        try {
            EquipmentPre pre = equipmentPreRepository.findById(equipPreId);
            if (pre == null) {
                log.warn("剧情奖励装备模板不存在: preId={}", equipPreId);
                return null;
            }
            Equipment equipment = equipmentService.buildEquipmentFromPre(
                    gameUserId, pre, "STORY", "剧情赠送");
            equipmentRepository.save(equipment);
            log.info("剧情赠送装备: userId={}, equip={} (preId={})", gameUserId, pre.getName(), equipPreId);
            return pre.getName();
        } catch (Exception e) {
            log.error("剧情赠送装备失败: userId={}, preId={}", gameUserId, equipPreId, e);
            return null;
        }
    }

    public Map<String, Object> complete(String userId, int serverId) {
        StoryProgress progress = getOrCreate(userId, serverId);
        progress.setCompleted(true);
        progress.setCurrentNode(0);
        progress.setUpdateTime(System.currentTimeMillis());
        storyProgressMapper.update(progress);

        Map<String, Object> result = new HashMap<>();
        result.put("currentNode", 0);
        result.put("completed", true);
        return result;
    }

    public Map<String, Object> skip(String userId, int serverId) {
        return complete(userId, serverId);
    }

    private StoryProgress getOrCreate(String userId, int serverId) {
        StoryProgress progress = storyProgressMapper.findByUserAndServer(userId, serverId);
        if (progress == null) {
            progress = new StoryProgress();
            progress.setUserId(userId);
            progress.setServerId(serverId);
            progress.setCurrentNode(FIRST_NODE);
            progress.setCompleted(false);
            progress.setCreateTime(System.currentTimeMillis());
            progress.setUpdateTime(System.currentTimeMillis());
            storyProgressMapper.insert(progress);
        }
        return progress;
    }
}
