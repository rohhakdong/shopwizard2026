package com.shopwizard.adjust.model;

import lombok.Data;

/** tAdjAdjustNoSeq 채번 테이블용 — INSERT 후 생성된 Seq 값을 그대로 정산번호(AdjustNo)로 쓴다. */
@Data
public class AdjustNoSeq {
    private Integer seq;
}
