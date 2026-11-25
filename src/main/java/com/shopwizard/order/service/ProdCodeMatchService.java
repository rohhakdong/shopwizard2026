package com.shopwizard.order.service;

import com.shopwizard.order.mapper.ProdCodeMatchMapper;
import com.shopwizard.order.model.ProdCodeMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품매칭결과 — 상품명+옵션이 어떤 상품코드로 매칭되는지의 규칙(shopwizard.tOrdProdCodeMatch, 기본키 상품명+옵션)을 조회·등록·수정·삭제한다.
 * 이 규칙은 수집 주문 자동 매칭({@code ShoplinkerOrderWriter})·엑셀 주문 업로드({@code OrderExcelService})와 상품 매칭 화면이
 * 읽고 쓰는 것과 같은 테이블이다. 규칙은 <b>앞으로</b> 들어오는 주문의 매칭에 쓰이고 이미 들어온 미매칭 주문은 "상품 매칭" 화면에서 연결한다.
 *
 * <p>수집이 주문 줄의 {@code ProdName}·{@code ItemName} 과 <b>정확히</b> 비교하므로 상품명·옵션은 trim 하지 않고 입력 그대로 저장하고
 * (주문 상품명에 끝 공백이 있는 경우가 실제로 있다), 옵션이 비어 있으면 {@code ""}(null 이 아니라)로 저장한다. 상품코드는 판매 상품에 있어야 한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ProdCodeMatchService {

    private final ProdCodeMatchMapper mapper;

    @Transactional(readOnly = true)
    public List<ProdCodeMatch> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public ProdCodeMatch select(Map<String, Object> params) { return mapper.select(params); }

    /** 기본키(상품명+옵션)로 한 건(없으면 null). 옵션이 null 이면 빈 옵션으로 본다. */
    @Transactional(readOnly = true)
    public ProdCodeMatch find(String prodName, String prodOption) {
        if (prodName == null || prodName.isBlank()) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("prodName", prodName);
        key.put("prodOption", prodOption == null ? "" : prodOption);
        return mapper.select(key);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProdCodeMatch create(ProdCodeMatch in) {
        String name = name(in.getProdName());
        String option = option(in.getProdOption());
        String code = prodCode(in.getProdCode());
        if (mapper.countProd(code) == 0) throw bad("판매 상품에 없는 상품코드입니다: " + code);
        if (find(name, option) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 상품명·옵션의 매칭 규칙이 있습니다. 목록에서 수정하세요.");
        }
        ProdCodeMatch row = new ProdCodeMatch();
        row.setProdName(name);
        row.setProdOption(option);
        row.setProdCode(code);
        row.setRemark(in.getRemark());
        row.setRegistId(in.getRegistId());
        row.setRegistName(in.getRegistName());
        mapper.insert(row);
        return find(name, option);
    }

    /** 상품코드만 바꾼다(상품명·옵션은 기본키라 바꾸지 않는다). */
    @Transactional(rollbackFor = Exception.class)
    public ProdCodeMatch modify(ProdCodeMatch in) {
        ProdCodeMatch existing = find(in.getProdName(), in.getProdOption());
        if (existing == null) throw bad("매칭 규칙을 찾을 수 없습니다.");
        String code = prodCode(in.getProdCode());
        if (mapper.countProd(code) == 0) throw bad("판매 상품에 없는 상품코드입니다: " + code);
        existing.setProdCode(code);
        existing.setChangeId(in.getChangeId());
        existing.setChangeName(in.getChangeName());
        mapper.update(existing);
        return find(existing.getProdName(), existing.getProdOption());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(String prodName, String prodOption) {
        ProdCodeMatch existing = find(prodName, prodOption);
        if (existing == null) throw bad("매칭 규칙을 찾을 수 없습니다.");
        mapper.delete(existing);
    }

    // 상품명·옵션은 trim 하지 않는다 — 수집·엑셀이 주문 줄의 ProdName/ItemName 원문(끝 공백 포함)과 정확히 비교하므로 입력 그대로 저장해야 한다.
    private static String name(String v) {
        if (v == null || v.trim().isEmpty()) throw bad("상품명을 입력하세요.");
        if (v.length() > 255) throw bad("상품명은 255자 이내여야 합니다.");
        return v;
    }

    private static String option(String v) {
        String t = v == null ? "" : v;
        if (t.length() > 255) throw bad("옵션은 255자 이내여야 합니다.");
        return t;
    }

    private static String prodCode(String v) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) throw bad("상품코드를 입력하세요.");
        if (t.length() > 255) throw bad("상품코드는 255자 이내여야 합니다.");
        return t;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
