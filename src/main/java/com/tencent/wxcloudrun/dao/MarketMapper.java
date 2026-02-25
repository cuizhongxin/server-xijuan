package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface MarketMapper {

    void insertListing(@Param("sellerId") String sellerId,
                       @Param("sellerName") String sellerName,
                       @Param("itemType") String itemType,
                       @Param("itemId") String itemId,
                       @Param("itemName") String itemName,
                       @Param("itemIcon") String itemIcon,
                       @Param("itemLevel") int itemLevel,
                       @Param("itemQuality") int itemQuality,
                       @Param("itemCount") int itemCount,
                       @Param("price") long price,
                       @Param("commission") long commission,
                       @Param("itemSnapshot") String itemSnapshot,
                       @Param("createTime") long createTime);

    Map<String, Object> findById(@Param("id") long id);

    List<Map<String, Object>> findActive(@Param("itemType") String itemType,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    int countActive(@Param("itemType") String itemType);

    List<Map<String, Object>> findBySeller(@Param("sellerId") String sellerId,
                                            @Param("limit") int limit);

    void updateStatus(@Param("id") long id,
                      @Param("status") String status,
                      @Param("buyerId") String buyerId,
                      @Param("buyerName") String buyerName,
                      @Param("updateTime") long updateTime);

    void insertTradeLog(@Param("listingId") long listingId,
                        @Param("sellerId") String sellerId,
                        @Param("buyerId") String buyerId,
                        @Param("itemType") String itemType,
                        @Param("itemName") String itemName,
                        @Param("price") long price,
                        @Param("createTime") long createTime);

    List<Map<String, Object>> findTradeLogs(@Param("userId") String userId, @Param("limit") int limit);
}
