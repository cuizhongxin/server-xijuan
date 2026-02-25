package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlunderMapper {

    String findByUserId(@Param("userId") String userId);

    void upsert(@Param("userId") String userId, @Param("data") String data,
                @Param("createTime") Long createTime, @Param("updateTime") Long updateTime);

    /**
     * 获取所有用户ID和等级信息（用于匹配掠夺对象）
     */
    List<Map<String, Object>> findAllUserLevels();
}
