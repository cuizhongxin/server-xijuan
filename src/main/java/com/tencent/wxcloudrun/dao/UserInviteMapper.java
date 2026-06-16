package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserInviteMapper {

    int insertIgnore(@Param("inviterUserId") Long inviterUserId,
                     @Param("inviteeUserId") Long inviteeUserId,
                     @Param("createTime") Long createTime);

    long countInviteesByInviter(@Param("inviterUserId") Long inviterUserId);
}

