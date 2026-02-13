package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShopMapper {
    
    /**
     * 查询所有商品
     */
    List<Shop> findAll();
    
    /**
     * 根据分类查询商品
     */
    List<Shop> findByClassify(@Param("classify") String classify);
    
    /**
     * 根据ID查询商品
     */
    Shop findById(@Param("id") Long id);
}
