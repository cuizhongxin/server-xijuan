package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface PeerageConfigMapper {

    List<Map<String, Object>> findAllPeerage();

    List<Map<String, Object>> findAllSoldierTiers();

    List<Map<String, Object>> findSoldierTiersByCategory(@Param("troopCategory") String troopCategory);

    Map<String, Object> findFameTokenConfig(@Param("itemId") int itemId);

    List<Map<String, Object>> findAllFameTokenConfigs();
}
