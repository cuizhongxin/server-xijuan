package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CornucopiaMapper {

    // ── 期数 ──
    Map<String, Object> findActivePeriod();
    Map<String, Object> findPeriodById(@Param("id") long id);
    Map<String, Object> findLastDrawnPeriod();
    void insertPeriod(@Param("periodNum") int periodNum, @Param("drawTime") String drawTime, @Param("carryover") long carryover);
    void addToPool(@Param("id") long id, @Param("amount") long amount);
    void finishDraw(@Param("id") long id, @Param("grandNumber") String grandNumber, @Param("firstNumber") String firstNumber,
                    @Param("grandWinnerId") String grandWinnerId, @Param("firstWinnerId") String firstWinnerId,
                    @Param("grandPrize") long grandPrize, @Param("firstPrize") long firstPrize);

    // ── 购票 ──
    int countUserTickets(@Param("userId") String userId, @Param("periodId") long periodId);
    List<Map<String, Object>> findUserTickets(@Param("userId") String userId, @Param("periodId") long periodId);
    void insertTicket(@Param("userId") String userId, @Param("periodId") long periodId, @Param("ticketNumber") String ticketNumber);
    List<Map<String, Object>> findAllTicketsByPeriod(@Param("periodId") long periodId);

    // ── 旧表兼容 ──
    Map<String, Object> findByUserAndDate(@Param("userId") String userId, @Param("drawDate") String drawDate);
    void upsertRecord(@Param("userId") String userId, @Param("drawDate") String drawDate,
                      @Param("drawCount") int drawCount, @Param("freeUsed") int freeUsed);
    int getTodayDrawCount(@Param("userId") String userId, @Param("drawDate") String drawDate);
}
