package com.shopwizard.order.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 상품 매칭 결과. 화면 매칭 1건 또는 CSV 1행에 대한 처리 요약.
 */
@Data
public class ProdMatchResult {
    private boolean ok = true;
    private String prodName;
    private String prodOption;
    private String message;
    private int applied;                 // backfill 성공한 주문라인 수
    private int skipped;                  // 정산마감 등으로 건너뛴 수
    private List<String> skipDetails = new ArrayList<>();

    public static ProdMatchResult fail(String prodName, String prodOption, String message) {
        ProdMatchResult r = new ProdMatchResult();
        r.ok = false;
        r.prodName = prodName;
        r.prodOption = prodOption;
        r.message = message;
        return r;
    }
}
