package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustDeliAddr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustDeliAddrMapper {
    List<CustDeliAddr> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustDeliAddr select(Map<String, Object> params);
    CustDeliAddr selectDefault(Integer custId);

    // ── 고객 본인 배송지(스토어프론트). 모든 쿼리가 CustId 로 소유권을 확인한다 ──────────────────────────
    /** 같은 회원의 배송지 변경(추가·기본 지정·삭제)을 직렬화한다 — 개수 제한과 "기본은 하나" 규칙이 동시 요청에 깨지지 않게. */
    Integer lockCust(@Param("custId") Integer custId);
    List<CustDeliAddr> selectMine(@Param("custId") Integer custId);
    CustDeliAddr selectOne(@Param("custId") Integer custId, @Param("deliAddrNo") Integer deliAddrNo);
    int countMine(@Param("custId") Integer custId);
    CustDeliAddr findSame(@Param("custId") Integer custId, @Param("a") CustDeliAddr a);
    int insertMine(CustDeliAddr addr);
    int updateMine(CustDeliAddr addr);
    int clearDefault(@Param("custId") Integer custId);
    int setDefault(@Param("custId") Integer custId, @Param("deliAddrNo") Integer deliAddrNo);
    int deleteMine(@Param("custId") Integer custId, @Param("deliAddrNo") Integer deliAddrNo);
    /** 기본 배송지가 없을 때 가장 최근 배송지를 기본으로 올린다. */
    int promoteLatest(@Param("custId") Integer custId);
}
