package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdCodeMapper;
import com.shopwizard.order.model.OrderProdCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 발주상품코드 — 레거시 "발주상품코드" 화면(wOrdOrderProdCodeList). 키워드와 용량 조합마다 상점 상품코드를 정의하는 마스터다.
 * 기본키 {@code MatchProdCode} 는 서버가 {@code 키워드::용량1x수량1[+용량2x수량2]} 로 만든다(레거시 규칙).
 * 수정에서는 상점·상품코드·상품명칭·사용여부만 바꾸고 키워드·용량(= 기본키의 재료)은 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
public class OrderProdCodeService {

    private final OrderProdCodeMapper mapper;

    @Transactional(readOnly = true)
    public List<OrderProdCode> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public OrderProdCode select(Map<String, Object> params) { return mapper.select(params); }

    /** 기본키로 한 건(없으면 null). */
    @Transactional(readOnly = true)
    public OrderProdCode find(String matchProdCode) {
        if (matchProdCode == null || matchProdCode.isBlank()) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("matchProdCode", matchProdCode.trim());
        return mapper.select(key);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderProdCode create(OrderProdCode in) {
        String shop = required(in.getShopCode(), "상점", 12);
        String prodCode = required(in.getProdCode(), "상품코드", 255);
        String prodName = required(in.getProdName(), "상품명칭", 255);
        String keywrd = required(in.getKeywrd(), "키워드", 50);
        String capa1 = required(in.getCapa1(), "용량1", 50);
        String qty1 = quantity(in.getQty1(), "수량1", true);
        String capa2 = optional(in.getCapa2(), "용량2", 50);
        String qty2 = quantity(in.getQty2(), "수량2", false);
        if ((capa2 == null) != (qty2 == null)) throw bad("용량2와 수량2는 함께 입력하거나 둘 다 비워야 합니다.");
        int useYn = useYn(in.getUseYn());

        String code = matchProdCode(keywrd, capa1, qty1, capa2, qty2);
        if (code.length() > 255) throw bad("키워드와 용량이 너무 깁니다.");
        if (find(code) != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 매칭상품코드입니다: " + code);

        OrderProdCode row = new OrderProdCode();
        row.setMatchProdCode(code);
        row.setShopCode(shop);
        row.setProdCode(prodCode);
        row.setProdName(prodName);
        row.setKeywrd(keywrd);
        row.setCapa1(capa1);
        row.setQty1(qty1);
        row.setCapa2(capa2 == null ? "" : capa2);
        row.setQty2(qty2 == null ? "" : qty2);
        row.setUseYn(useYn);
        row.setResultType("성공");
        row.setResultMessag("처리에 성공하였습니다.");
        row.setState(1);
        row.setRegistId(in.getRegistId());
        row.setRegistName(in.getRegistName());
        mapper.insert(row);
        return find(code);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderProdCode modify(OrderProdCode in) {
        OrderProdCode existing = find(in.getMatchProdCode());
        if (existing == null) throw bad("매칭상품코드를 찾을 수 없습니다.");
        existing.setShopCode(required(in.getShopCode(), "상점", 12));
        existing.setProdCode(required(in.getProdCode(), "상품코드", 255));
        existing.setProdName(required(in.getProdName(), "상품명칭", 255));
        existing.setUseYn(useYn(in.getUseYn()));
        existing.setResultType("수정");
        existing.setResultMessag("정보수정에 성공하였습니다.");
        existing.setChangeId(in.getChangeId());
        existing.setChangeName(in.getChangeName());
        mapper.update(existing);
        return find(existing.getMatchProdCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(String matchProdCode) {
        OrderProdCode existing = find(matchProdCode);
        if (existing == null) throw bad("매칭상품코드를 찾을 수 없습니다.");
        mapper.delete(existing);
    }

    /** {@code 키워드::용량1x수량1} + (용량2가 있으면 {@code +용량2x수량2}). */
    static String matchProdCode(String keywrd, String capa1, String qty1, String capa2, String qty2) {
        StringBuilder sb = new StringBuilder(keywrd).append("::").append(capa1).append('x').append(qty1);
        if (capa2 != null) sb.append('+').append(capa2).append('x').append(qty2);
        return sb.toString();
    }

    private static String required(String v, String label, int max) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) throw bad(label + "을(를) 입력하세요.");
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    private static String optional(String v, String label, int max) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) return null;
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    /** 1 이상 정수 문자열(컬럼이 varchar(5)). 비어 있으면 필수가 아닐 때 null. */
    private static String quantity(String v, String label, boolean required) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) {
            if (required) throw bad(label + "을(를) 입력하세요.");
            return null;
        }
        if (!t.matches("[1-9][0-9]{0,4}")) throw bad(label + "은(는) 1~99999 사이의 정수여야 합니다.");
        return t;
    }

    private static int useYn(Integer v) {
        if (v == null) return 1;
        if (v != 0 && v != 1) throw bad("사용여부가 올바르지 않습니다.");
        return v;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
