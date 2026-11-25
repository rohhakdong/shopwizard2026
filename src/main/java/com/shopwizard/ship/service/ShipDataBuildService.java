package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderGiftRuleMapper;
import com.shopwizard.order.model.OrderGiftRule;
import com.shopwizard.ship.mapper.ShipAutoOrderMapper;
import com.shopwizard.ship.model.PrintRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 자동발주의 "배송 데이터 생성"(레거시 8단계 {@code runCreateShipCommon3} + 9단계 출력순서).
 * 한 상점의 발주서 대기 행(원본 라인 출고지시 + 아직 발주확인 전)에 대해:
 * ① 지난번 사은품 행 삭제 ② 출고지·우편번호 정리 ③ 사은품 규칙 적용(사은품 행 생성) ④ 묶음배송 표시 ⑤ 출력순서 키.
 * 몇 번을 다시 실행해도 같은 결과가 되도록(멱등) 사은품 행은 OrderType='사은품' 으로 구분해 매번 지우고 다시 만든다.
 *
 * <p>레거시와 달라진 점(의도적): 사은품 규칙은 사용 중(State=1)만 쓰고, 각 규칙을 서로 덮어쓰지 않고 독립 평가하며,
 * 증정수량이 0이면 행을 만들지 않고, 묶음배송은 "서로 다른 주문 2개 이상"일 때만 켠다.</p>
 */
@Service
@RequiredArgsConstructor
public class ShipDataBuildService {

    static final String DEFAULT_SHIP_PLACE = "기본출고지";

    private final ShipAutoOrderMapper mapper;
    private final OrderGiftRuleMapper giftRuleMapper;

    /** 결과 요약 — 화면에 "대상 N행 / 묶음 N행 / 사은품 N행" 으로 보여 준다. */
    public record Result(String shopCode, String shipPlace, int rows, int bundledRows, int giftRows) {}

    /** 사은품 1건 계획(어느 앵커 행 아래에 무엇을 몇 개). 테스트가 직접 검증한다. */
    public record GiftPlan(Integer orderNo, Integer orderProdNo, int ruleNo, String giftName, int giftQty,
                           int groupQty, long groupAmt) {}

    @Transactional(rollbackFor = Exception.class)
    public Result build(String shopCode, String registId, String registName) {
        String shop = shopCode == null ? "" : shopCode.trim();
        if (shop.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "상점을 선택하세요.");

        mapper.deletePendingGifts(shop);

        String prodDesc = mapper.selectShopProdDesc(shop);
        String shipPlace = prodDesc == null || prodDesc.trim().isEmpty() ? DEFAULT_SHIP_PLACE : prodDesc.trim();
        mapper.updateShipPlaceAndZip(shop, shipPlace, registId, registName);

        List<PrintRow> rows = mapper.selectPendingRows(shop);
        List<GiftPlan> plans = planGifts(rows, giftRuleMapper.selectActiveByShop(shop));
        int giftRows = 0;
        Map<String, Boolean> totalsDone = new LinkedHashMap<>();
        for (GiftPlan p : plans) {
            if (totalsDone.put(p.orderNo() + "-" + p.orderProdNo(), Boolean.TRUE) == null) {
                mapper.updateGiftTotals(p.orderNo(), p.orderProdNo(), p.groupQty(), p.groupAmt());
            }
            giftRows += mapper.insertGiftRow(p.orderNo(), p.orderProdNo(), p.giftName(), p.giftQty(), registId, registName);
        }

        mapper.resetBundle(shop);
        int bundledRows = mapper.markBundle(shop);
        mapper.updateSortNames(shop);

        return new Result(shop, shipPlace, rows.size() + giftRows, bundledRows, giftRows);
    }

    /**
     * 사은품 계산(순수 함수). 수신자(주소+이름) 그룹마다 총 주문수량·총 금액을 구하고, 규칙을 번호 순으로 각각 평가한다.
     * 규칙의 행 조건(채널·채널상품명·딜명·증정 기간)에 맞는 행이 그룹에 있고 총수량·총금액이 구간에 들면(0/비움 = 제한 없음)
     * 그 중 가장 앞 주문(앵커)에 사은품 1건을 붙인다. 증정수량 = 규칙 증정수량 x (중복=0 이면 1, 아니면 총수량 DIV 중복).
     * 대상 행은 구성 1번(OrderChangeNo=1)이고 사은품이 아닌 것만 — 구성 행이 여럿이어도 라인 수량·금액은 한 번만 센다.
     */
    static List<GiftPlan> planGifts(List<PrintRow> rows, List<OrderGiftRule> rules) {
        List<GiftPlan> plans = new ArrayList<>();
        if (rows == null || rules == null || rules.isEmpty()) return plans;

        Map<String, List<PrintRow>> groups = new LinkedHashMap<>();
        for (PrintRow r : rows) {
            if (!isBase(r)) continue;
            groups.computeIfAbsent(norm(r.getRecverAddr()) + "@@@" + norm(r.getRecverName()), k -> new ArrayList<>()).add(r);
        }
        for (List<PrintRow> group : groups.values()) {
            int totalQty = 0;
            long totalAmt = 0;
            for (PrintRow r : group) {
                totalQty += nz(r.getOrderProdQty() != null ? r.getOrderProdQty() : r.getProdQty());
                totalAmt += nz(r.getSalePrice());
            }
            for (OrderGiftRule rule : rules) {
                if (!inRange(totalQty, rule.getTotalOrderQtyStart(), rule.getTotalOrderQtyEnd())) continue;
                if (!inRange(totalAmt, rule.getTotalOrderAmtStart(), rule.getTotalOrderAmtEnd())) continue;
                PrintRow anchor = null;
                for (PrintRow r : group) {   // 입력이 (주문번호, 라인번호) 순이라 처음 맞는 행이 가장 앞 주문
                    if (rowMatches(rule, r)) { anchor = r; break; }
                }
                if (anchor == null) continue;
                int dup = nz(rule.getDupYn());
                int giftQty = nz(rule.getGiftQty()) * (dup <= 0 ? 1 : totalQty / dup);
                String name = rule.getGiftProdName1() == null ? "" : rule.getGiftProdName1().trim();
                if (giftQty <= 0 || name.isEmpty()) continue;
                plans.add(new GiftPlan(anchor.getOrderNo(), anchor.getOrderProdNo(),
                        nz(rule.getRuleNo()), name, giftQty, totalQty, totalAmt));
            }
        }
        return plans;
    }

    private static boolean isBase(PrintRow r) {
        return nz(r.getOrderChangeNo()) == 1 && !"사은품".equals(r.getOrderType());
    }

    /** 행 조건: 채널(같음)·채널상품명/딜명(포함, 대소문자 무시)·증정 기간(결제완료일시가 기간 안). 비어 있으면 조건 없음. */
    static boolean rowMatches(OrderGiftRule rule, PrintRow r) {
        if (!blank(rule.getChnlCode()) && !rule.getChnlCode().trim().equals(r.getChnlCode() == null ? "" : r.getChnlCode().trim())) return false;
        if (!contains(r.getOrderProdName(), rule.getChnlProdName())) return false;
        if (!contains(r.getDealName(), rule.getDealName())) return false;
        if (!blank(rule.getGiftStartDate()) && !blank(rule.getGiftEndDate())) {
            String pay = r.getPayCmpletDate();
            if (blank(pay)) return false;
            if (pay.compareTo(rule.getGiftStartDate()) < 0 || pay.compareTo(rule.getGiftEndDate()) > 0) return false;
        }
        return true;
    }

    /** 구간 판정 — 시작/종료가 NULL 이거나 0 이면 그쪽은 제한 없음(레거시의 "0 = 조건 없음"). */
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
        while (end > 0 && s.charAt(end - 1) == ' ') end--;   // DB 동등 비교가 끝 공백을 무시하는 것과 맞춘다
        return s.substring(0, end).toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    private static int nz(Integer i) { return i == null ? 0 : i; }
}
