package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.DeliRstrctZipcode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeliRstrctZipcodeMapper {
    /** 구분별 우편번호 건수. */
    List<Map<String, Object>> selectSects();
    List<DeliRstrctZipcode> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    /** 이미 등록된 우편번호(구분 안에서) — 일괄 등록의 중복 판정. */
    List<String> selectExisting(@Param("sect") String sect, @Param("zipcodes") java.util.Collection<String> zipcodes);
    /** 한 번에 여러 건 등록(중복은 호출자가 걸러낸다). */
    int insertAll(@Param("sect") String sect, @Param("zipcodes") java.util.Collection<String> zipcodes,
                  @Param("registId") String registId, @Param("registName") String registName);
    int deleteOne(@Param("sect") String sect, @Param("zipcode") String zipcode);
    int deleteSect(@Param("sect") String sect);
    /** 이 구분에 등록된 우편번호 수 — 규칙 화면의 경고용. */
    int countBySect(@Param("sect") String sect);
}
