package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderUpriceMapper;
import com.shopwizard.order.mapper.UpriceMatchMapper;
import com.shopwizard.order.model.UpriceMatchDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
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
    private final OrderUpriceMapper upriceMapper;

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

    // ── 단가매칭결과 화면: 규칙 목록·편집·삭제 ──────────────────────────

    /** 규칙(상품명+옵션)별 목록 — 구성·원가 합계(사용 중인 코드만)·못 쓰는 코드 수. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> selectRules(Map<String, Object> params) {
        return upriceMatchMapper.selectRules(params);
    }

    @Transactional(readOnly = true)
    public int selectRuleCount(Map<String, Object> params) {
        return upriceMatchMapper.selectRuleCount(params);
    }

    /** 규칙의 줄 — 사용 중지·삭제된 코드도 포함(편집용). */
    @Transactional(readOnly = true)
    public List<UpriceMatchDetail> selectRuleLines(String prodName, String itemName) {
        return upriceMatchMapper.selectRuleLines(prodName, itemName == null ? "" : itemName);
    }

    /** 규칙 전체 삭제. 이미 들어온 주문은 건드리지 않고, 이 상품은 다시 "발주단가매칭" 미매칭 목록에 나타난다. */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(String prodName, String itemName) {
        String item = itemName == null ? "" : itemName;
        if (upriceMatchMapper.selectRuleLines(prodName, item).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매칭 규칙을 찾을 수 없습니다.");
        }
        upriceMatchMapper.deleteByProdItem(prodName, item);
    }

    /**
     * (상품명,옵션) 의 매칭 규칙을 교체 저장한다. 기존 줄을 전부 지우고 새로 넣는다. 줄을 모두 검증한 뒤에 쓴다:
     * 최소 1줄, 발주단가코드는 발주단가표에 있어야 하고 한 규칙 안에서 겹칠 수 없으며, 수량은 1~9,999. 상품명·옵션은 주문 줄 원문과
     * 정확히 비교되므로 trim 하지 않는다(옵션이 비면 ""). 세트가 아니라도 항상 이 메커니즘을 거친다(사용자 요청).
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMatch(String prodName, String itemName, List<UpriceMatchDetail> lines,
                          String registId, String registName) {
        if (prodName == null || prodName.trim().isEmpty()) throw bad("상품명을 입력하세요.");
        if (prodName.length() > 255) throw bad("상품명은 255자 이내여야 합니다.");
        String item = itemName == null ? "" : itemName;
        if (item.length() > 255) throw bad("옵션은 255자 이내여야 합니다.");
        if (lines == null || lines.isEmpty()) throw bad("최소 1줄 이상의 발주단가 매칭이 필요합니다.");
        if (lines.size() > 30) throw bad("한 규칙에는 최대 30줄까지 넣을 수 있습니다.");

        java.util.Set<String> seen = new HashSet<>();
        for (UpriceMatchDetail line : lines) {
            String code = line == null || line.getMatchUpriceCode() == null ? "" : line.getMatchUpriceCode().trim();
            if (code.isEmpty()) throw bad("발주단가코드가 비어 있는 줄이 있습니다.");
            if (line.getQty() == null || line.getQty() <= 0 || line.getQty() > 9_999) throw bad("수량은 1~9,999 사이여야 합니다.");
            if (!seen.add(code)) throw bad("같은 발주단가코드가 두 줄 이상입니다: " + code + " (수량을 합쳐 한 줄로 넣으세요)");
            Map<String, Object> key = new HashMap<>();
            key.put("matchUpriceCode", code);
            if (upriceMapper.select(key) == null) throw bad("발주단가표에 없는 발주단가코드입니다: " + code);
        }

        upriceMatchMapper.deleteByProdItem(prodName, item);
        int seq = 1;
        for (UpriceMatchDetail line : lines) {
            UpriceMatchDetail d = new UpriceMatchDetail();
            d.setProdName(prodName);
            d.setItemName(item);
            d.setMatchSeq(seq++);
            d.setMatchUpriceCode(line.getMatchUpriceCode().trim());
            d.setQty(line.getQty());
            d.setRegistId(registId);
            d.setRegistName(registName);
            upriceMatchMapper.insertDetail(d);
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
