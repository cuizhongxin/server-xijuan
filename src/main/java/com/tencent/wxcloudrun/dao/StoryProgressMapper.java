package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.StoryProgress;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StoryProgressMapper {

    @Select("SELECT * FROM story_progress WHERE user_id = #{userId} AND server_id = #{serverId} LIMIT 1")
    StoryProgress findByUserAndServer(@Param("userId") String userId, @Param("serverId") int serverId);

    @Insert("INSERT INTO story_progress (user_id, server_id, current_node, completed, create_time, update_time) " +
            "VALUES (#{userId}, #{serverId}, #{currentNode}, #{completed}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(StoryProgress progress);

    @Update("UPDATE story_progress SET current_node = #{currentNode}, completed = #{completed}, " +
            "update_time = #{updateTime} WHERE id = #{id}")
    void update(StoryProgress progress);
}
