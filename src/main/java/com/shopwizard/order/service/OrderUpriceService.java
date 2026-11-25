package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderUpriceMapper;
import com.shopwizard.order.model.OrderUprice;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 발주단가표 — 레거시 "발주단가표" 화면(wOrdOrderUpriceList). 키워드·용량마다 개당 발주단가를 정의하는 마스터다.
 * 기본키 {@code MatchUpriceCode} 는 서버가 {@code 키워드-용량} 으로 만든다(레거시 규칙). 같은 상점에서 같은 발주상품명칭이
 * 같은 사용여부로 둘 이상 있을 수 없고, 수정에서는 상점·단가·단량·사용여부만 바꾼다. 단가매칭 규칙(출고지시 원가 계산)이 쓰는 코드는
 * 지울 수 없다 — 지우면 출고지시 원가에서 그 줄이 조용히 빠진다. 상점을 비우면 모든 서비스가 쓰는 공통 코드다.
 */
@Service
@RequiredArgsConstructor
public class OrderUpriceService {

    private final OrderUpriceMapper mapper;

    @Transactional(readOnly = true)
    public List<OrderUprice> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public OrderUprice select(Map<String, Object> params) { return mapper.select(params); }

    /** 기본키로 한 건(없으면 null). */
    @Transactional(readOnly = true)
    public OrderUprice find(String matchUpriceCode) {
        if (matchUpriceCode == null || matchUpriceCode.isBlank()) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("matchUpriceCode", matchUpriceCode.trim());
        return mapper.select(key);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderUprice create(OrderUprice in) {
        String shop = blankToNull(in.getShopCode());
        if (shop != null && shop.length() > 12) throw bad("상점코드가 올바르지 않습니다.");
        String name = required(in.getOrderProdName(), "발주상품명칭", 255);
        String keywrd = required(in.getKeywrd(), "키워드", 50);
        String capa = required(in.getCapa(), "용량", 50);
        int uprice = uprice(in.getUprice());
        String unit = optional(in.getUnitName(), "단량", 50);
        int useYn = useYn(in.getUseYn());

        String code = keywrd + "-" + capa;
        if (code.length() > 255) throw bad("키워드와 용량이 너무 깁니다.");
        if (find(code) != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 매칭단가코드입니다: " + code);
        if (mapper.countSameName(shop, name, useYn, code) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "사용 중인 발주상품명칭에 중복이 있습니다: " + name);
        }

        OrderUprice row = new OrderUprice();
        row.setMatchUpriceCode(code);
        row.setShopCode(shop == null ? "" : shop);
        row.setOrderProdName(name);
        row.setKeywrd(keywrd);
        row.setCapa(capa);
        row.setUprice(uprice);
        row.setUnitName(unit);
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
    public OrderUprice modify(OrderUprice in) {
        OrderUprice existing = find(in.getMatchUpriceCode());
        if (existing == null) throw bad("매칭단가코드를 찾을 수 없습니다.");
        String shop = blankToNull(in.getShopCode());
        if (shop != null && shop.length() > 12) throw bad("상점코드가 올바르지 않습니다.");
        int uprice = uprice(in.getUprice());
        int useYn = useYn(in.getUseYn());
        if (mapper.countSameName(shop, existing.getOrderProdName(), useYn, existing.getMatchUpriceCode()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "사용 중인 발주상품명칭에 중복이 있습니다: " + existing.getOrderProdName());
        }
        existing.setShopCode(shop == null ? "" : shop);
        existing.setUprice(uprice);
        existing.setUnitName(optional(in.getUnitName(), "단량", 50));
        existing.setUseYn(useYn);
        existing.setResultType("수정");
        existing.setResultMessag("정보수정에 성공하였습니다.");
        existing.setChangeId(in.getChangeId());
        existing.setChangeName(in.getChangeName());
        mapper.update(existing);
        return find(existing.getMatchUpriceCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(String matchUpriceCode) {
        OrderUprice existing = find(matchUpriceCode);
        if (existing == null) throw bad("매칭단가코드를 찾을 수 없습니다.");
        int usage = mapper.countUsage(existing.getMatchUpriceCode());
        if (usage > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "단가매칭 규칙이나 발주견적에서 쓰고 있어 지울 수 없습니다(" + usage + "건). 쓰지 않으려면 사용여부를 '미사용'으로 바꾸세요.");
        }
        mapper.delete(existing);
    }

    private static String required(String v, String label, int max) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) throw bad(label + "을(를) 입력하세요.");
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    private static String optional(String v, String label, int max) {
        String t = blankToNull(v);
        if (t != null && t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    private static String blankToNull(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }

    /** 0 이상 정수(컬럼 int(8) — 99,999,999 이하). */
    private static int uprice(Integer v) {
        if (v == null) throw bad("단가를 입력하세요.");
        if (v < 0 || v > 99_999_999) throw bad("단가는 0 이상 99,999,999 이하의 정수여야 합니다.");
        return v;
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
