package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderFeeApplyMapper;
import com.shopwizard.order.mapper.OrderFeeRuleMapper;
import com.shopwizard.order.model.FeeLine;
import com.shopwizard.order.model.OrderFeeRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 자동발주의 "수수료 규칙 적용"(레거시 자동발주 2단계 {@code createOrdersCommon1Fees} + 묶음 수수료 처리).
 * 한 상점의 지불완료 라인에 배송비·판촉비 규칙(tOrdOrderFeeRule)을 적용해 배송형태·배송비·판촉비·공급가와 마진을 고친다.
 * 출고지시 전에 실행해야 출고 행·발주서·정산에 값이 그대로 전파된다(출고지시가 원본 라인을 스냅샷으로 복사한다).
 *
 * <p>평가는 레거시와 같은 의미다 — 규칙을 RuleNo 순으로 하나씩 적용하고 나중 규칙이 앞 규칙을 덮어쓴다(State 는 보지 않는다):
 * <ul>
 *   <li>수수료율 ≠ 0: 공급가 = 판매가 x (100 - 수수료율) / 100 (반올림)만</li>
 *   <li>전체무료: 배송비·판촉비를 규칙 값으로, 배송형태를 '무료'로 — 배송형태와 상관없이</li>
 *   <li>그 밖(무료/선불/착불): 현재 배송형태가 규칙의 배송형태와 같은 라인만 배송비·판촉비를 규칙 값으로</li>
 * </ul>
 * 규칙이 하나라도 있으면 스마일배송·11번가풀필먼트 채널은 선불/0/0 으로 고정하고, 마지막으로 같은 수신자(주소+이름)에게 가는
 * 서로 다른 주문이 둘 이상이면 가장 작은 주문번호의 주문 외에는 배송비·판촉비를 0 으로 한다(묶음배송은 한 번만 부과).</p>
 *
 * <p>레거시와 달라진 점: 채널은 이름이 아니라 코드로 비교하고, 샵링커 수집분의 배송형태 "0/1/2" 는 무료/선불/착불로 읽는다.
 * 원본 라인을 직접 바꾸므로 규칙이 배송형태를 '무료'로 바꾼 뒤 다시 실행하면 "현재 배송형태" 조건이 달라질 수 있다.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderFeeApplyService {

    private final OrderFeeApplyMapper applyMapper;
    private final OrderFeeRuleMapper ruleMapper;
    private final OrderFeeLineWriter writer;

    /** 결과 요약 — 규칙 수, 대상 라인, 바뀐 라인, 묶음으로 0 이 된 라인, 그 사이 바뀌어 건너뛴 라인. */
    public record Result(String shopCode, int rules, int lines, int changed, int bundleZeroed, int skipped) {}

    /** 계획 한 건: 라인과 새 값. */
    record Planned(FeeLine line, OrderFeeLineWriter.Values values, boolean bundleZeroed) {}

    public Result apply(String shopCode, String actorId, String actorName) {
        String shop = shopCode == null ? "" : shopCode.trim();
        if (shop.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "상점을 선택하세요.");

        List<OrderFeeRule> rules = ruleMapper.selectByShop(shop);
        List<FeeLine> lines = applyMapper.selectPayCompleteLines(shop);
        List<Planned> plans = plan(lines, rules);

        int changed = 0, zeroed = 0, skipped = 0;
        for (Planned p : plans) {
            boolean ok;
            try {
                ok = writer.write(p.line().getOrderNo(), p.line().getOrderProdNo(), shop, p.values(), actorId, actorName);
            } catch (Exception e) {
                ok = false;
            }
            if (ok) {
                changed++;
                if (p.bundleZeroed()) zeroed++;
            } else {
                skipped++;
            }
        }
        return new Result(shop, rules.size(), lines.size(), changed, zeroed, skipped);
    }

    /** 규칙 적용 계산(순수 함수) — 값이 실제로 달라지는 라인만 돌려준다. */
    static List<Planned> plan(List<FeeLine> lines, List<OrderFeeRule> rules) {
        List<Planned> out = new ArrayList<>();
        if (lines == null || lines.isEmpty() || rules == null || rules.isEmpty()) return out;

        Map<FeeLine, State> states = new LinkedHashMap<>();
        for (FeeLine l : lines) states.put(l, new State(l));

        for (OrderFeeRule rule : rules) {
            for (FeeLine l : lines) {
                if (!matches(rule, l)) continue;
                State s = states.get(l);
                BigDecimal rate = rule.getChnlFeeRate() == null ? BigDecimal.ZERO : rule.getChnlFeeRate();
                String sect = rule.getDeliFeeSect() == null ? "" : rule.getDeliFeeSect().trim();
                if (rate.signum() != 0) {
                    s.supply = new BigDecimal(nz(l.getSalePrice()))
                            .multiply(new BigDecimal(100).subtract(rate))
                            .divide(new BigDecimal(100), 0, RoundingMode.HALF_UP).intValue();
                } else if ("전체무료".equals(sect)) {
                    s.deliFeeAmt = nz(rule.getDeliFee());
                    s.promotFeeAmt = nz(rule.getPromotFee());
                    s.type = "무료";
                } else if (normType(s.type).equals(sect)) {
                    s.deliFeeAmt = nz(rule.getDeliFee());
                    s.promotFeeAmt = nz(rule.getPromotFee());
                }
            }
        }

        // 스마일배송·11번가풀필먼트: 규칙 결과와 상관없이 선불/0/0 (레거시는 규칙마다 다시 덮었으므로 최종 결과가 같다)
        for (FeeLine l : lines) {
            String chnl = l.getChnlName() == null ? "" : l.getChnlName();
            if (chnl.contains("(스마일배송)") || chnl.contains("11번가풀필먼트")) {
                State s = states.get(l);
                s.type = "선불";
                s.deliFeeAmt = 0;
                s.promotFeeAmt = 0;
            }
        }

        // 묶음: 같은 수신자(주소+이름)에 서로 다른 주문이 둘 이상이면 가장 작은 주문번호 외의 주문은 배송비·판촉비 0
        Map<String, List<FeeLine>> groups = new LinkedHashMap<>();
        for (FeeLine l : lines) {
            groups.computeIfAbsent(norm(l.getRecverAddr()) + "@@@" + norm(l.getRecverName()), k -> new ArrayList<>()).add(l);
        }
        for (List<FeeLine> g : groups.values()) {
            TreeSet<Integer> orders = new TreeSet<>();
            for (FeeLine l : g) orders.add(l.getOrderNo());
            if (orders.size() < 2) continue;
            Integer first = orders.first();
            for (FeeLine l : g) {
                if (l.getOrderNo().equals(first)) continue;
                State s = states.get(l);
                if (s.deliFeeAmt != 0 || s.promotFeeAmt != 0) s.bundleZeroed = true;
                s.deliFeeAmt = 0;
                s.promotFeeAmt = 0;
            }
        }

        for (Map.Entry<FeeLine, State> e : states.entrySet()) {
            FeeLine l = e.getKey();
            State s = e.getValue();
            boolean same = java.util.Objects.equals(s.type, l.getDeliFeeType()) && s.deliFeeAmt == nz(l.getDeliFeeAmt())
                    && s.promotFeeAmt == nz(l.getPromotFeeAmt()) && s.supply == nz(l.getSupplyPrice());
            if (!same) out.add(new Planned(l, new OrderFeeLineWriter.Values(s.type, s.deliFeeAmt, s.promotFeeAmt, s.supply), s.bundleZeroed));
        }
        return out;
    }

    /** 규칙 조건: 채널(코드 같음)·상품명(상품명+공백+옵션에 포함)·딜명(포함, 대소문자 무시)·수량·금액 구간(0/비움 = 제한 없음). */
    static boolean matches(OrderFeeRule rule, FeeLine l) {
        if (!blank(rule.getChnlCode()) && !rule.getChnlCode().trim().equals(l.getChnlCode() == null ? "" : l.getChnlCode().trim())) return false;
        String prod = (l.getProdName() == null ? "" : l.getProdName()) + " " + (l.getItemName() == null ? "" : l.getItemName());
        if (!contains(prod, rule.getProdName())) return false;
        if (!contains(l.getDealName(), rule.getDealName())) return false;
        if (!inRange(nz(l.getProdQty()), rule.getOrderQtyStart(), rule.getOrderQtyEnd())) return false;
        return inRange(nz(l.getSalePrice()), rule.getOrderAmtStart(), rule.getOrderAmtEnd());
    }

    /** 배송형태를 한글로 — 샵링커 수집분은 0/1/2 로 들어 있다. */
    static String normType(String t) {
        if (t == null) return "";
        return switch (t.trim()) {
            case "0" -> "무료";
            case "1" -> "선불";
            case "2" -> "착불";
            default -> t.trim();
        };
    }

    private static boolean inRange(long value, Integer start, Integer end) {
        if (start != null && start > 0 && value < start) return false;
        return end == null || end <= 0 || value <= end;
    }

    private static boolean contains(String text, String part) {
        if (blank(part)) return true;
        return text != null && text.toLowerCase(Locale.ROOT).contains(part.trim().toLowerCase(Locale.ROOT));
    }

    private static String norm(String s) {
        if (s == null) return "";
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') end--;
        return s.substring(0, end).toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    private static int nz(Integer i) { return i == null ? 0 : i; }

    /** 라인의 계산 중 값(현재 값에서 시작). */
    private static final class State {
        String type;
        int deliFeeAmt;
        int promotFeeAmt;
        int supply;
        boolean bundleZeroed;

        State(FeeLine l) {
            type = l.getDeliFeeType();
            deliFeeAmt = nz(l.getDeliFeeAmt());
            promotFeeAmt = nz(l.getPromotFeeAmt());
            supply = nz(l.getSupplyPrice());
        }
    }
}
