package com.shopwizard.authority.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 역할의 "켜진" 메뉴/기능(State=1) 조회 — 권한 판정 전용. 역할 자체의 State 는 보지 않는다
 * (레거시 데이터에서 운영 중인 계정이 State=0 역할에 매달려 있음).
 */
@Mapper
public interface RolePermissionMapper {
    /** 키: MenuCode, MenuUri */
    List<Map<String, Object>> selectActiveMenus(@Param("roleUid") String roleUid);

    /** 키: MenuCode, FunctnUri */
    List<Map<String, Object>> selectActiveFunctns(@Param("roleUid") String roleUid);
}
