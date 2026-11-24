package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderConslt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 주문상세의 고객상담(shopion.tOprCustConslt). operate 의 옛 스캐폴드와 달리 주문 기준으로 읽고 코드표·상품은 LEFT JOIN 이다. */
@Mapper
public interface OrderConsltMapper {

    /** 한 주문의 상담(최신 먼저). 코드표에 없는 옛 값이나 State 0 행도 나온다. */
    List<OrderConslt> selectByOrder(@Param("orderNo") String orderNo);

    /** 상담 한 건(처리 수정 전 소유 주문 확인용). */
    OrderConslt selectOne(@Param("custConsltNo") int custConsltNo);

    /** 등록 — custConsltNo 가 채번되어 돌아온다. */
    int insert(OrderConslt conslt);

    /** 처리내용 수정. 반영된 행 수. */
    int updateAnswer(@Param("custConsltNo") int custConsltNo, @Param("answrTitle") String answrTitle,
                     @Param("answrCntnts") String answrCntnts, @Param("mngrId") String mngrId, @Param("mngrName") String mngrName,
                     @Param("changeId") String changeId, @Param("changeName") String changeName);

    /** 사용 중(State=1)인 코드값인가(상담구분·사유·매체 검증). */
    int countActiveCode(@Param("constrCode") String constrCode, @Param("constrVal") String constrVal);

    /** 줄의 상담여부 표시. orderProdNo 가 null 이면 주문의 모든 줄. */
    int markConsulted(@Param("orderNo") int orderNo, @Param("orderProdNo") Integer orderProdNo,
                      @Param("changeId") String changeId, @Param("changeName") String changeName);
}
