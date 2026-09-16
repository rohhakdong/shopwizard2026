package com.shopwizard.ship.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 출고지시 처리 결과. 선택한 라인 중 성공/실패 건수와 실패 사유.
 */
@Data
public class ShipDirectIssueResult {
    private int success;
    private int failed;
    private List<String> failDetails = new ArrayList<>();
}
