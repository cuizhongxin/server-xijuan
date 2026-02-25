package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface SupplyTransportMapper {

    void insert(@Param("userId") String userId,
                @Param("gradeId") int gradeId,
                @Param("gradeName") String gradeName,
                @Param("silverReward") long silverReward,
                @Param("paperReward") long paperReward,
                @Param("foodReward") long foodReward,
                @Param("metalReward") long metalReward,
                @Param("startTime") long startTime,
                @Param("endTime") long endTime,
                @Param("createDate") String createDate);

    Map<String, Object> findActiveByUserId(@Param("userId") String userId);

    List<Map<String, Object>> findByUserIdAndDate(@Param("userId") String userId,
                                                   @Param("createDate") String createDate);

    List<Map<String, Object>> findAllActive();

    Map<String, Object> findById(@Param("id") long id);

    void updateRobbed(@Param("id") long id,
                      @Param("robbedCount") int robbedCount,
                      @Param("robbedSilver") long robbedSilver,
                      @Param("robbedPaper") long robbedPaper,
                      @Param("robbedFood") long robbedFood,
                      @Param("robbedMetal") long robbedMetal);

    void updateEndTime(@Param("id") long id,
                       @Param("endTime") long endTime,
                       @Param("speedUpMinutes") int speedUpMinutes);

    void updateStatus(@Param("id") long id, @Param("status") String status);
}
