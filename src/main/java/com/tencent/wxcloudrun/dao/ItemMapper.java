package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItemMapper {
    
    /**
     * 根据道具ID查询
     */
    Item findById(@Param("itemId") Integer itemId);
    
    /**
     * 查询所有道具
     */
    List<Item> findAll();
}
