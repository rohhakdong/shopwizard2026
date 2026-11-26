package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.ConsltInquiry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 고객문의 전체 조회(읽기 전용) — shopion.tOprCustConslt. */
@Mapper
public interface ConsltInquiryMapper {

    List<ConsltInquiry> selectList(@Param("q") ConsltQuery q, @Param("offset") int offset, @Param("limit") int limit);

    int selectCount(@Param("q") ConsltQuery q);

    /** 조회 조건(검증·정리를 마친 값; null 은 조건 없음). */
    @lombok.Value
    class ConsltQuery {
        String start;
        String end;
        String type;
        String reason;
        String media;
        /** 1 처리완료 · 0 미처리 · null 전체 */
        Integer answered;
        String chnlCode;
        String svcCode;
        String orderNo;
        String custName;
        /** 숫자만 남긴 연락처 */
        String phone;
        /** LIKE 이스케이프를 마친 제목·내용 키워드 */
        String keyword;
        String mngrName;
    }
}
