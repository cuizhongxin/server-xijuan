package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DailyTaskMapper {

    List<Map<String, Object>> findDailyTasks(@Param("userId") String userId,
                                              @Param("taskDate") String taskDate);

    void upsertDailyTask(@Param("userId") String userId,
                         @Param("taskDate") String taskDate,
                         @Param("taskType") String taskType,
                         @Param("increment") int increment);

    void claimDailyTask(@Param("userId") String userId,
                        @Param("taskDate") String taskDate,
                        @Param("taskType") String taskType);

    List<Map<String, Object>> findStageClaims(@Param("userId") String userId,
                                               @Param("claimDate") String claimDate);

    void insertStageClaim(@Param("userId") String userId,
                          @Param("claimDate") String claimDate,
                          @Param("stage") int stage);

    Map<String, Object> findAchievement(@Param("userId") String userId,
                                         @Param("achievementType") String achievementType);

    void upsertAchievement(@Param("userId") String userId,
                           @Param("achievementType") String achievementType);

    void claimAchievement(@Param("userId") String userId,
                          @Param("achievementType") String achievementType);

    List<Map<String, Object>> findAllAchievements(@Param("userId") String userId);
}
