package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.EquipmentPre;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipmentPreMapper {

    EquipmentPre findById(@Param("id") Integer id);

    List<EquipmentPre> findAll();

    List<EquipmentPre> findByColor(@Param("color") Integer color);

    List<EquipmentPre> findBySuitId(@Param("suitId") Integer suitId);

    List<EquipmentPre> findByNeedLevel(@Param("needLevel") Integer needLevel);

    List<EquipmentPre> findByIds(@Param("ids") List<Integer> ids);

    List<EquipmentPre> findBySetName(@Param("setName") String setName);
}
