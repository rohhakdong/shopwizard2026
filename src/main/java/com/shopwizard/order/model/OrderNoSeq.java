package com.shopwizard.order.model;

import lombok.Data;

/**
 * tOrdOrderNoSeq 채번 테이블용 모델.
 * INSERT 후 자동 채번된 Seq 값을 그대로 주문번호(OrderNo)로 사용한다.
 */
@Data
public class OrderNoSeq {
    private Integer seq;
}
