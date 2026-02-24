package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SecretRealmRewardMapper {

    List<Map<String, Object>> findByRealmId(@Param("realmId") String realmId);

    List<Map<String, Object>> findByRealmIdAndType(@Param("realmId") String realmId,
                                                    @Param("rewardType") String rewardType);

    List<Map<String, Object>> findAllEnabled();
}
