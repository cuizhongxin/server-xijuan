package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.EquipmentPre;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipmentPreMapper {

    EquipmentPre findById(@Param("id") Integer id);

    List<EquipmentPre> findAll();

    List<EquipmentPre> findBySource(@Param("source") String source);

    List<EquipmentPre> findByLevel(@Param("level") Integer level);

    List<EquipmentPre> findByLevelAndSource(@Param("level") Integer level, @Param("source") String source);

    List<EquipmentPre> findByIds(@Param("ids") List<Integer> ids);

    List<EquipmentPre> findBySetName(@Param("setName") String setName);
}
