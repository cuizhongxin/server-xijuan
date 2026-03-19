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
                @Param("nation") String nation,
                @Param("todayChallenge") int todayChallenge,
                @Param("todayWins") int todayWins,
                @Param("todayPurchased") int todayPurchased,
                @Param("lastResetDate") String lastResetDate,
                @Param("lastChallengeTime") long lastChallengeTime,
                @Param("pendingFame") long pendingFame,
                @Param("pendingSilver") long pendingSilver,
                @Param("pendingExp") long pendingExp,
                @Param("rewardClaimed") int rewardClaimed,
                @Param("settleDate") String settleDate,
                @Param("updateTime") long updateTime);

    List<Map<String, Object>> findByRanking(@Param("offset") int offset, @Param("limit") int limit);

    List<Map<String, Object>> findRandomInRange(@Param("minRank") int minRank,
                                                 @Param("maxRank") int maxRank,
                                                 @Param("excludeUserId") String excludeUserId,
                                                 @Param("limit") int limit);

    int countAll();

    List<Map<String, Object>> findAllOrderByRanking();

    List<Map<String, Object>> findAllOrderByPower();

    void updateRanking(@Param("userId") String userId, @Param("ranking") int ranking,
                       @Param("updateTime") long updateTime);

    void swapRanking(@Param("userA") String userA, @Param("rankA") int rankA,
                     @Param("userB") String userB, @Param("rankB") int rankB,
                     @Param("updateTime") long updateTime);

    void setPendingReward(@Param("userId") String userId,
                          @Param("pendingFame") long pendingFame,
                          @Param("pendingSilver") long pendingSilver,
                          @Param("pendingExp") long pendingExp,
                          @Param("settleDate") String settleDate);

    void markRewardClaimed(@Param("userId") String userId);

    void insertBattle(@Param("attackerId") String attackerId,
                      @Param("attackerName") String attackerName,
                      @Param("attackerLevel") int attackerLevel,
                      @Param("defenderId") String defenderId,
                      @Param("defenderName") String defenderName,
                      @Param("defenderLevel") int defenderLevel,
                      @Param("victory") boolean victory,
                      @Param("fameGain") long fameGain,
                      @Param("atkOldRank") int atkOldRank,
                      @Param("atkNewRank") int atkNewRank,
                      @Param("defOldRank") int defOldRank,
                      @Param("defNewRank") int defNewRank,
                      @Param("battleReport") String battleReport,
                      @Param("createTime") long createTime,
                      @Param("createDate") String createDate);

    List<Map<String, Object>> findBattlesByUser(@Param("userId") String userId,
                                                 @Param("limit") int limit);

    Map<String, Object> findBattleReportById(@Param("id") long id);

    void insertRewardLog(@Param("userId") String userId,
                         @Param("ranking") int ranking,
                         @Param("fameReward") long fameReward,
                         @Param("silverReward") long silverReward,
                         @Param("settleDate") String settleDate,
                         @Param("createTime") long createTime);

    List<Map<String, Object>> findRewardLogs(@Param("userId") String userId, @Param("limit") int limit);
}
