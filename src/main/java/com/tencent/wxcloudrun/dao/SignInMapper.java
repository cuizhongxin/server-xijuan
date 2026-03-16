package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SignInMapper {

    List<String> findSignedDates(@Param("userId") String userId, @Param("yearMonth") String yearMonth);

    int countSignIn(@Param("userId") String userId, @Param("signDate") String signDate);

    void insertSignIn(@Param("userId") String userId, @Param("signDate") String signDate, @Param("isMakeup") int isMakeup);

    int countMonthSignIn(@Param("userId") String userId, @Param("yearMonth") String yearMonth);

    int countMonthMakeup(@Param("userId") String userId, @Param("yearMonth") String yearMonth);
}
