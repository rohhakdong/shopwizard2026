package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.SlnkInvoiceLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 샵링커 송장 전송에 쓰는 조회(읽기 전용). 이력 기록은 {@code SlinkLogMapper}. */
@Mapper
public interface SlnkInvoiceMapper {

    /** 줄 한 건(전송 직전에 읽는다). 없으면 null. */
    SlnkInvoiceLine selectLine(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                               @Param("orderChangeNo") Integer orderChangeNo);

    /**
     * 송장은 저장됐는데 샵링커로 나갔다는 성공 이력이 없는 줄(최근 days 일). 성공 이력 = ResultType 이 dry·false·error 가 아닌 송장 이력
     * (true 또는 레거시가 남긴 응답 글). 샵링커 주문코드가 있고 제외 채널이 아닌 줄만. shop/chnl/svc 는 데이터 범위(null = 제한 없음).
     */
    List<SlnkInvoiceLine> selectPending(@Param("days") int days, @Param("excluded") List<String> excluded,
                                        @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode, @Param("svcCode") String svcCode,
                                        @Param("limit") int limit);

    /** 택배회사 마스터에 이 이름이 있는가(송장 엑셀이 택배회사명을 검사할 때). */
    int countDeliComp(@Param("name") String name);

    int countPending(@Param("days") int days, @Param("excluded") List<String> excluded,
                     @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode, @Param("svcCode") String svcCode);
}
