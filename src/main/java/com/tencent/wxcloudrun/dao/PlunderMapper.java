package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.PlunderData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlunderMapper {

    PlunderData findByUserId(@Param("userId") String userId);

    void upsert(@Param("userId") String userId,
                @Param("availableCount") Integer availableCount,
                @Param("lastRecoverTime") Long lastRecoverTime,
                @Param("todayPurchased") Integer todayPurchased,
                @Param("lastResetDate") String lastResetDate,
                @Param("todayCount") Integer todayCount,
                @Param("updateTime") Long updateTime);

    /**
     * 获取同区服用户ID和等级信息（用于匹配掠夺对象）
     */
    List<Map<String, Object>> findUserLevelsByServerId(@Param("serverSuffix") String serverSuffix);
}
