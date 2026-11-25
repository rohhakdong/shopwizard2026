package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderGiftRuleMapper;
import com.shopwizard.order.model.OrderGiftRule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 발주 사은품 규칙 — 레거시 "발주사은품규칙" 화면(wOrdOrderGiftRuleList). 상점 단위 규칙으로, 조건(채널·채널상품명·딜명·기간·
 * 총 주문 수량/금액 구간)에 맞는 주문에 사은품(1번 필수, 2~5 선택)을 증정수량만큼 붙인다.
 *
 * <p>수량/금액 구간의 0(또는 비움)은 "제한 없음"이다(레거시 SQL 이 0 이면 조건을 건너뛴다). {@code DupYn}: 0 = 수신자당 한 세트만,
 * N(≥1) = 총 주문수량 N개마다 증정수량을 곱한다(레거시 SQL 은 {@code 증정수량 × (총수량 DIV DupYn)} 이라 0 이면 NULL 이 됐다).
 * 이 서비스는 규칙을 저장·관리한다. 사용 중(State=1)인 규칙은 자동발주의 배송 데이터 생성({@code ShipDataBuildService})이 발주서 사은품 행으로 반영한다.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderGiftRuleService {

    private final OrderGiftRuleMapper mapper;

    @Transactional(readOnly = true)
    public List<OrderGiftRule> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public OrderGiftRule select(Map<String, Object> params) { return mapper.select(params); }

    /** 규칙번호로 한 건(없으면 null). */
    @Transactional(readOnly = true)
    public OrderGiftRule find(Integer ruleNo) {
        if (ruleNo == null) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("ruleNo", ruleNo);
        return mapper.select(key);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderGiftRule create(OrderGiftRule in) {
        OrderGiftRule row = normalize(in);
        checkMasters(row, null);
        row.setState(state(in.getState(), 1));   // 비우면 사용 중
        row.setRegistId(in.getRegistId());
        row.setRegistName(in.getRegistName());
        mapper.insert(row);
        return find(row.getRuleNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderGiftRule modify(OrderGiftRule in) {
        OrderGiftRule existing = find(in.getRuleNo());
        if (existing == null) throw bad("규칙을 찾을 수 없습니다.");
        OrderGiftRule row = normalize(in);
        checkMasters(row, existing);
        row.setRuleNo(existing.getRuleNo());
        row.setState(state(in.getState(), existing.getState() == null ? 0 : existing.getState()));   // 비우면 지금 값 유지
        row.setChangeId(in.getChangeId());
        row.setChangeName(in.getChangeName());
        mapper.update(row);
        return find(existing.getRuleNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Integer ruleNo) {
        OrderGiftRule existing = find(ruleNo);
        if (existing == null) throw bad("규칙을 찾을 수 없습니다.");
        mapper.delete(existing);
    }

    /** 형식·범위 검증과 값 정리(공백 제거, 빈 값은 "" / 0). 마스터 존재 여부는 {@link #checkMasters} 에서. */
    /** 사용 여부(1=사용, 0=사용 안 함). 사용 중인 규칙만 배송 데이터 생성에서 사은품으로 적용된다. */
    private static int state(Integer v, int fallback) {
        if (v == null) return fallback;
        if (v != 0 && v != 1) throw bad("사용 여부는 사용(1) 또는 사용 안 함(0) 이어야 합니다.");
        return v;
    }

    private OrderGiftRule normalize(OrderGiftRule in) {
        OrderGiftRule r = new OrderGiftRule();
        r.setShopCode(required(in.getShopCode(), "상점", 50));
        r.setChnlCode(optional(in.getChnlCode(), "채널", 50));
        r.setChnlProdCode(optional(in.getChnlProdCode(), "채널상품코드", 50));
        r.setChnlProdName(optional(in.getChnlProdName(), "채널상품명칭", 255));
        r.setDealName(optional(in.getDealName(), "딜명칭", 50));

        r.setTotalOrderQtyStart(nonNegative(in.getTotalOrderQtyStart(), "총 주문수량 시작", Integer.MAX_VALUE));
        r.setTotalOrderQtyEnd(nonNegative(in.getTotalOrderQtyEnd(), "총 주문수량 종료", Integer.MAX_VALUE));
        r.setTotalOrderAmtStart(nonNegative(in.getTotalOrderAmtStart(), "총 주문금액 시작", Integer.MAX_VALUE));
        r.setTotalOrderAmtEnd(nonNegative(in.getTotalOrderAmtEnd(), "총 주문금액 종료", Integer.MAX_VALUE));
        range(r.getTotalOrderQtyStart(), r.getTotalOrderQtyEnd(), "총 주문수량");
        range(r.getTotalOrderAmtStart(), r.getTotalOrderAmtEnd(), "총 주문금액");

        r.setGiftProdCode1(required(in.getGiftProdCode1(), "사은품 코드 1", 255));
        r.setGiftProdName1(required(in.getGiftProdName1(), "사은품 명칭 1", 255));
        r.setGiftProdCode2(optional(in.getGiftProdCode2(), "사은품 코드 2", 255));
        r.setGiftProdName2(optional(in.getGiftProdName2(), "사은품 명칭 2", 255));
        r.setGiftProdCode3(optional(in.getGiftProdCode3(), "사은품 코드 3", 255));
        r.setGiftProdName3(optional(in.getGiftProdName3(), "사은품 명칭 3", 255));
        r.setGiftProdCode4(optional(in.getGiftProdCode4(), "사은품 코드 4", 255));
        r.setGiftProdName4(optional(in.getGiftProdName4(), "사은품 명칭 4", 255));
        r.setGiftProdCode5(optional(in.getGiftProdCode5(), "사은품 코드 5", 255));
        r.setGiftProdName5(optional(in.getGiftProdName5(), "사은품 명칭 5", 255));
        pair(r.getGiftProdCode2(), r.getGiftProdName2(), 2);
        pair(r.getGiftProdCode3(), r.getGiftProdName3(), 3);
        pair(r.getGiftProdCode4(), r.getGiftProdName4(), 4);
        pair(r.getGiftProdCode5(), r.getGiftProdName5(), 5);

        if (in.getGiftQty() == null || in.getGiftQty() < 1 || in.getGiftQty() > 999_999) throw bad("증정 수량은 1 이상 999,999 이하여야 합니다.");
        r.setGiftQty(in.getGiftQty());
        r.setPromotFee(nonNegative(in.getPromotFee(), "판촉 수수료", 99_999_999));
        r.setDupYn(nonNegative(in.getDupYn(), "중복 여부", 999_999));

        String start = date(in.getGiftStartDate(), "증정 시작일", false);
        String end = date(in.getGiftEndDate(), "증정 종료일", true);
        if ((start == null) != (end == null)) throw bad("증정 기간은 시작일과 종료일을 함께 입력하세요.");
        if (start != null && start.substring(0, 10).compareTo(end.substring(0, 10)) > 0) throw bad("증정 종료일은 시작일보다 빠를 수 없습니다.");
        r.setGiftStartDate(start);
        r.setGiftEndDate(end);

        r.setRemark(optional(in.getRemark(), "비고", 50));
        return r;
    }

    /** 상점·채널이 실제로 있는지, 사은품 코드 1 이 발주단가표에 있는지(신규이거나 코드를 바꾼 경우만 — 레거시에서 넘어온 기존 값은 그대로 둘 수 있게). */
    private void checkMasters(OrderGiftRule r, OrderGiftRule existing) {
        if (mapper.countShop(r.getShopCode()) == 0) throw bad("없는 상점입니다: " + r.getShopCode());
        if (!r.getChnlCode().isEmpty() && mapper.countChnl(r.getChnlCode()) == 0) throw bad("없는 채널입니다: " + r.getChnlCode());
        boolean codeChanged = existing == null || !r.getGiftProdCode1().equals(existing.getGiftProdCode1());
        if (codeChanged && mapper.countUprice(r.getGiftProdCode1()) == 0) {
            throw bad("사은품 코드 1 이 발주단가표에 없습니다: " + r.getGiftProdCode1() + " (발주단가표 화면에서 먼저 등록하세요)");
        }
    }

    private static String required(String v, String label, int max) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) throw bad(label + "을(를) 입력하세요.");
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    /** 선택 입력 — 비면 "" (레거시 화면도 빈 값을 ""로 저장해 SQL 이 `isNotEmpty` 로 건너뛴다). */
    private static String optional(String v, String label, int max) {
        String t = v == null ? "" : v.trim();
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    private static Integer nonNegative(Integer v, String label, int max) {
        if (v == null) return 0;
        if (v < 0 || v > max) throw bad(label + "은(는) 0 이상 " + String.format("%,d", max) + " 이하여야 합니다.");
        return v;
    }

    /** 종료가 0 이면 제한 없음, 아니면 시작 ≤ 종료. */
    private static void range(int start, int end, String label) {
        if (end != 0 && start > end) throw bad(label + " 시작은 종료보다 클 수 없습니다.");
    }

    private static void pair(String code, String name, int no) {
        if (code.isEmpty() != name.isEmpty()) throw bad("사은품 " + no + " 은(는) 코드와 명칭을 함께 입력하세요.");
    }

    /** yyyy-MM-dd(뒤에 시각이 붙어 있어도 날짜만 본다) → 시작은 00:00:00, 종료는 23:59:59 로 정규화. 비면 null. */
    private static String date(String v, String label, boolean endOfDay) {
        String t = v == null ? "" : v.trim();
        if (t.isEmpty()) return null;
        try {
            LocalDate d = LocalDate.parse(t.length() > 10 ? t.substring(0, 10) : t);
            return d + (endOfDay ? " 23:59:59" : " 00:00:00");
        } catch (DateTimeParseException e) {
            throw bad(label + "이(가) 올바른 날짜(yyyy-MM-dd)가 아닙니다.");
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
