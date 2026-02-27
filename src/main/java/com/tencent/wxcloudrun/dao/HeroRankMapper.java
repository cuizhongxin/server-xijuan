package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface HeroRankMapper {

    Map<String, Object> findByUserId(@Param("userId") String userId);

    void upsert(@Param("userId") String userId,
                @Param("userName") String userName,
                @Param("level") int level,
                @Param("power") int power,
                @Param("fame") long fame,
                @Param("rankName") String rankName,
                @Param("ranking") int ranking,
                @Param("todayChallenge") int todayChallenge,
                @Param("todayWins") int todayWins,
                @Param("todayPurchased") int todayPurchased,
                @Param("lastResetDate") String lastResetDate,
                @Param("lastChallengeTime") long lastChallengeTime,
                @Param("updateTime") long updateTime);

    List<Map<String, Object>> findTopN(@Param("limit") int limit);

    List<Map<String, Object>> findPage(@Param("offset") int offset, @Param("limit") int limit);

    int countAll();

    /** 按战力排序返回所有用户（用于排名结算） */
    List<Map<String, Object>> findAllOrderByPower();

    void updateRanking(@Param("userId") String userId, @Param("ranking") int ranking);

    // 挑战记录
    void insertBattle(@Param("attackerId") String attackerId,
                      @Param("attackerName") String attackerName,
                      @Param("attackerLevel") int attackerLevel,
                      @Param("defenderId") String defenderId,
                      @Param("defenderName") String defenderName,
                      @Param("defenderLevel") int defenderLevel,
                      @Param("victory") boolean victory,
                      @Param("fameGain") long fameGain,
                      @Param("createTime") long createTime,
                      @Param("createDate") String createDate);

    List<Map<String, Object>> findBattlesByAttacker(@Param("attackerId") String attackerId,
                                                     @Param("limit") int limit);

    // 每日奖励日志
    void insertRewardLog(@Param("userId") String userId,
                         @Param("ranking") int ranking,
                         @Param("fameReward") long fameReward,
                         @Param("silverReward") long silverReward,
                         @Param("settleDate") String settleDate,
                         @Param("createTime") long createTime);

    List<Map<String, Object>> findRewardLogs(@Param("userId") String userId, @Param("limit") int limit);
}
