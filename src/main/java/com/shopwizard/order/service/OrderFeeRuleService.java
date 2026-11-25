package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderFeeRuleMapper;
import com.shopwizard.order.model.OrderFeeRule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 배송비·판촉비 규칙 — 레거시 "배송비판촉비규칙" 화면(wOrdOrderFeeRuleList). 상점 단위 규칙이고 적용은 자동발주의
 * "수수료 규칙 적용"({@code OrderFeeApplyService})이 한다. 구간의 0(또는 비움)은 제한 없음.
 *
 * <p>수수료율이 0 이 아니면 규칙은 공급가 재계산만 하므로 배송비·판촉비·배송형태 입력은 의미가 없다 — 저장은 하되 값은 0/전체무료로 정리한다.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderFeeRuleService {

    /** 규칙이 가리킬 수 있는 배송형태. 전체무료 = 배송형태와 상관없이 적용하고 배송형태를 무료로 바꾼다. */
    public static final Set<String> SECTS = Set.of("전체무료", "무료", "선불", "착불");

    private final OrderFeeRuleMapper mapper;

    @Transactional(readOnly = true)
    public List<OrderFeeRule> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public OrderFeeRule select(Map<String, Object> params) { return mapper.select(params); }

    @Transactional(readOnly = true)
    public OrderFeeRule find(Integer ruleNo) {
        if (ruleNo == null) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("ruleNo", ruleNo);
        return mapper.select(key);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderFeeRule create(OrderFeeRule in) {
        OrderFeeRule row = normalize(in);
        checkMasters(row);
        row.setState(state(in.getState(), 1));
        row.setRegistId(in.getRegistId());
        row.setRegistName(in.getRegistName());
        mapper.insert(row);
        return find(row.getRuleNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderFeeRule modify(OrderFeeRule in) {
        OrderFeeRule existing = find(in.getRuleNo());
        if (existing == null) throw bad("규칙을 찾을 수 없습니다.");
        OrderFeeRule row = normalize(in);
        checkMasters(row);
        row.setRuleNo(existing.getRuleNo());
        row.setState(state(in.getState(), existing.getState() == null ? 0 : existing.getState()));
        row.setChangeId(in.getChangeId());
        row.setChangeName(in.getChangeName());
        mapper.update(row);
        return find(existing.getRuleNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Integer ruleNo) {
        OrderFeeRule existing = find(ruleNo);
        if (existing == null) throw bad("규칙을 찾을 수 없습니다.");
        mapper.delete(existing);
    }

    private OrderFeeRule normalize(OrderFeeRule in) {
        OrderFeeRule r = new OrderFeeRule();
        r.setShopCode(required(in.getShopCode(), "상점", 12));
        r.setChnlCode(optional(in.getChnlCode(), "채널", 12));
        r.setProdName(optional(in.getProdName(), "상품명(포함)", 50));
        r.setDealName(optional(in.getDealName(), "딜명(포함)", 50));

        r.setOrderQtyStart(nonNegative(in.getOrderQtyStart(), "주문수량 시작", Integer.MAX_VALUE));
        r.setOrderQtyEnd(nonNegative(in.getOrderQtyEnd(), "주문수량 종료", Integer.MAX_VALUE));
        r.setOrderAmtStart(nonNegative(in.getOrderAmtStart(), "주문금액 시작", 99_999_999));
        r.setOrderAmtEnd(nonNegative(in.getOrderAmtEnd(), "주문금액 종료", 99_999_999));
        range(r.getOrderQtyStart(), r.getOrderQtyEnd(), "주문수량");
        range(r.getOrderAmtStart(), r.getOrderAmtEnd(), "주문금액");

        BigDecimal rate = in.getChnlFeeRate() == null ? BigDecimal.ZERO : in.getChnlFeeRate();
        if (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0) throw bad("채널 수수료율은 0 이상 100 이하여야 합니다.");
        r.setChnlFeeRate(rate.setScale(2, java.math.RoundingMode.HALF_UP));

        if (rate.signum() != 0) {
            // 수수료율 규칙은 공급가 재계산만 한다 — 배송비·판촉비·배송형태 입력은 의미 없으므로 정리해 저장한다.
            r.setDeliFee(0);
            r.setPromotFee(0);
            r.setDeliFeeSect("전체무료");
        } else {
            r.setDeliFee(nonNegative(in.getDeliFee(), "배송비", 99_999_999));
            r.setPromotFee(nonNegative(in.getPromotFee(), "판촉비", 99_999_999));
            String sect = in.getDeliFeeSect() == null ? "" : in.getDeliFeeSect().trim();
            if (!SECTS.contains(sect)) throw bad("배송형태는 전체무료·무료·선불·착불 중 하나여야 합니다.");
            r.setDeliFeeSect(sect);
        }
        r.setRemark(optional(in.getRemark(), "비고", 50));
        return r;
    }

    private void checkMasters(OrderFeeRule r) {
        if (mapper.countShop(r.getShopCode()) == 0) throw bad("없는 상점입니다: " + r.getShopCode());
        if (!r.getChnlCode().isEmpty() && mapper.countChnl(r.getChnlCode()) == 0) throw bad("없는 채널입니다: " + r.getChnlCode());
    }

    /** 사용 여부(1/0). 이번 적용 정책은 레거시처럼 State 를 보지 않지만 값은 보존한다. 비우면 신규 1 / 수정은 지금 값 유지. */
    private static int state(Integer v, int fallback) {
        if (v == null) return fallback;
        if (v != 0 && v != 1) throw bad("사용 여부는 사용(1) 또는 사용 안 함(0) 이어야 합니다.");
        return v;
    }

    private static String required(String v, String label, int max) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) throw bad(label + "을(를) 입력하세요.");
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

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

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
