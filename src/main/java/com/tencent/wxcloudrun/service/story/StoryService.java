package com.tencent.wxcloudrun.service.story;

import com.tencent.wxcloudrun.dao.StoryProgressMapper;
import com.tencent.wxcloudrun.model.StoryProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StoryService {

    private static final int FIRST_NODE = 110;

    @Autowired
    private StoryProgressMapper storyProgressMapper;

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
        progress.setCurrentNode(nodeId);
        progress.setUpdateTime(System.currentTimeMillis());
        storyProgressMapper.update(progress);

        Map<String, Object> result = new HashMap<>();
        result.put("currentNode", nodeId);
        result.put("completed", false);
        return result;
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
