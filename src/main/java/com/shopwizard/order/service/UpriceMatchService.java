package com.shopwizard.order.service;

import com.shopwizard.order.mapper.UpriceMatchMapper;
import com.shopwizard.order.model.UpriceMatchDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 발주단가매칭 — 채널 상품명(ProdName+ItemName)을 발주단가코드 1개 이상으로 분해해 저장한다.
 * 세트 상품이면 여러 줄, 단품이면 1줄이지만 저장 메커니즘은 항상 동일하다.
 * 규칙만 저장하고 기존 주문은 건드리지 않는다 — 실제 원가 반영은 출고지시 처리 시점에
 * {@link ShipDirectIssueService} 가 그 시점의 최신 규칙으로 계산한다.
 */
@Service
@RequiredArgsConstructor
public class UpriceMatchService {

    private final UpriceMatchMapper upriceMatchMapper;

    public List<Map<String, Object>> selectUnmatched(Map<String, Object> params) {
        return upriceMatchMapper.selectListUnmatched(params);
    }

    public int selectUnmatchedCount(Map<String, Object> params) {
        return upriceMatchMapper.selectListUnmatchedCount(params);
    }

    /** 기존 저장된 매칭 상세 조회 (수정 진입 시 불러오기용). */
    public List<UpriceMatchDetail> selectDetail(String prodName, String itemName) {
        return upriceMatchMapper.selectPriced(prodName, itemName);
    }

    /**
     * (상품명,옵션) 의 매칭 규칙을 교체 저장한다. 기존 줄을 전부 지우고 새로 넣는다.
     * 최소 1줄 필요 — 세트가 아니라도 항상 이 메커니즘을 거치게 한다(사용자 요청).
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMatch(String prodName, String itemName, List<UpriceMatchDetail> lines,
                          String registId, String registName) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("최소 1줄 이상의 발주단가 매칭이 필요합니다.");
        }
        upriceMatchMapper.deleteByProdItem(prodName, itemName);
        int seq = 1;
        for (UpriceMatchDetail line : lines) {
            if (line.getMatchUpriceCode() == null || line.getMatchUpriceCode().isBlank()) {
                throw new IllegalArgumentException("발주단가코드가 비어 있는 줄이 있습니다.");
            }
            if (line.getQty() == null || line.getQty() <= 0) {
                throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
            }
            UpriceMatchDetail d = new UpriceMatchDetail();
            d.setProdName(prodName);
            d.setItemName(itemName);
            d.setMatchSeq(seq++);
            d.setMatchUpriceCode(line.getMatchUpriceCode());
            d.setQty(line.getQty());
            d.setRegistId(registId);
            d.setRegistName(registName);
            upriceMatchMapper.insertDetail(d);
        }
    }
}
