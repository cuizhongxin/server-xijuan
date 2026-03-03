package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserTacticsMapper {

    List<Map<String, Object>> findByUserId(@Param("userId") String userId);

    Map<String, Object> findByUserIdAndTacticsId(@Param("userId") String userId, @Param("tacticsId") String tacticsId);

    void upsert(@Param("userId") String userId, @Param("tacticsId") String tacticsId,
                @Param("level") int level, @Param("createTime") long createTime);

    void updateLevel(@Param("userId") String userId, @Param("tacticsId") String tacticsId, @Param("level") int level);
}
