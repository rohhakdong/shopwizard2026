package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderPay;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderPayMapper {
    List<OrderPay> selectList(Map<String, Object> params);
    OrderPay select(Map<String, Object> params);
    OrderPay selectByPgTradeNo(String pgTradeNo);
    /** 같은 PG 의 같은 가맹점 주문번호로 이미 저장된 결제의 주문번호(없으면 null) — 재전송·새로고침 때 중복 주문을 막는다. */
    Integer selectOrderNoByPgOrder(@org.apache.ibatis.annotations.Param("pgCode") String pgCode,
                                   @org.apache.ibatis.annotations.Param("pgOrderCode") String pgOrderCode);
    OrderPay selectPgByOrderNo(int orderNo);
    /** PG 구분 없이 거래번호로 결제건을 찾는다(KCP 웹훅의 tno). 같은 거래에 여러 건이면 최신 한 건. */
    OrderPay selectByPgTradeNoAny(@org.apache.ibatis.annotations.Param("pgCode") String pgCode,
                                  @org.apache.ibatis.annotations.Param("pgTradeNo") String pgTradeNo);
    /** 가상계좌 입금 확인 — 입금 금액·시각과 결제일을 기록한다. */
    int markDeposited(Map<String, Object> params);
    /** 입금 마감이 지났는데 아직 주문접수인 KCP 가상계좌 결제건(미입금 만료 대상). */
    List<OrderPay> selectExpiredKcpVbank(@org.apache.ibatis.annotations.Param("graceMinutes") int graceMinutes);
    void insert(OrderPay orderPay);
    void update(OrderPay orderPay);
    void updateOrderPayRecipt(Map<String, Object> params);
    void updateOrderPayReciptAuto(Map<String, Object> params);
    void delete(Map<String, Object> params);
}
