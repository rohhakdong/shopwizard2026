package com.shopwizard.order.service;

import com.shopwizard.order.mapper.LegacyUpriceMigrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 레거시 발주단가매칭 → 새 {@code tOrdUpriceMatch} 일회성 이관(미리보기 → 확정).
 *
 * <p>레거시 규칙 헤더(tOrdOrderUpriceMatch)의 키는 채널상품명을 여러 번 가공한 값이라 새 키(주문 라인의 상품명·옵션)로 복원할 수 없다.
 * 대신 레거시가 주문 라인마다 풀어 둔 구성({@code tOrdCurrOrderProd})을 원본 주문 라인과 조인해 <b>정확한 새 키</b>를 얻는다.
 * 레거시 수량({@code TotalQty})은 라인 합계(주문수량 포함)라서 새 규칙의 "상품 1개분" 수량은 {@code TotalQty ÷ 주문수량}이다.</p>
 *
 * <p>상태: {@code NEW}(이관 대상), {@code SAME}(이미 같은 규칙이 있음), {@code CONFLICT_EXISTING}(이미 다른 규칙이 있음 — 덮어쓰지 않는다),
 * {@code CONFLICT_LEGACY}(같은 키가 레거시 안에서 서로 다른 구성), {@code INACTIVE_CODE}(사용 중지된 발주단가코드 — 검토 필요, 자동 이관 제외),
 * {@code MISSING_CODE}(코드가 없거나 발주단가표에 없음), {@code ANOMALY_QTY}(수량이 주문수량으로 나누어떨어지지 않음 등), {@code TOO_MANY}(30줄 초과).
 * 사은품 구성('힣' 이름)은 일반 줄로 이관한다.</p>
 */
@Service
@RequiredArgsConstructor
public class LegacyUpriceMigrationService {

    static final int MAX_LINES = 30;
    static final int MAX_QTY = 9999;
    static final int MAX_ROWS_RETURNED = 1000;
    /** 이관 행의 등록자 표시 — 되돌릴 때 {@code DELETE … WHERE RegistId='MIGRATE-LEGACY'}. */
    public static final String MIGRATE_ID = "MIGRATE-LEGACY";

    private final LegacyUpriceMigrationMapper mapper;
    private final LegacyUpriceMigrationWriter writer;

    public record Line(String code, int qty) {}

    public record Candidate(String prodName, String itemName, String status, int orders, List<Line> lines,
                            int giftLines, boolean keyTrimmed, Integer sampleOrderNo) {}

    public record Preview(int legacyHeaders, int keys, Map<String, Integer> counts, List<Candidate> rows, boolean truncated,
                          List<Map<String, Object>> unmappable) {}

    public record CommitResult(int inserted, int skipped, int failed, Map<String, Integer> counts) {}

    @Transactional(readOnly = true)
    public Preview preview(String keyPrefix) {
        List<Candidate> all = plan(mapper.selectCandidates(), existing(), keyPrefix);
        Map<String, Integer> counts = new TreeMap<>();
        for (Candidate c : all) counts.merge(c.status(), 1, Integer::sum);
        boolean truncated = all.size() > MAX_ROWS_RETURNED;
        return new Preview(mapper.countLegacyHeaders(), all.size(), counts,
                truncated ? all.subList(0, MAX_ROWS_RETURNED) : all, truncated, mapper.selectUnmappableHeaders());
    }

    /** NEW 만 키 단위 트랜잭션으로 저장한다. 미리보기를 다시 계산하므로 그 사이 생긴 규칙은 건너뛴다. */
    public CommitResult commit(String keyPrefix, String actorName) {
        List<Candidate> all = plan(mapper.selectCandidates(), existing(), keyPrefix);
        Map<String, Integer> counts = new TreeMap<>();
        int inserted = 0, skipped = 0, failed = 0;
        for (Candidate c : all) {
            counts.merge(c.status(), 1, Integer::sum);
            if (!"NEW".equals(c.status())) continue;
            try {
                if (writer.insertRule(c.prodName(), c.itemName(), c.lines(), c.sampleOrderNo(), actorName)) inserted++; else skipped++;
            } catch (Exception e) {
                failed++;
            }
        }
        return new CommitResult(inserted, skipped, failed, counts);
    }

    private Map<String, List<Line>> existing() {
        Map<String, List<Line>> out = new LinkedHashMap<>();
        for (Map<String, Object> r : mapper.selectExistingRules()) {
            out.computeIfAbsent(key(str(r.get("prodName")).trim(), str(r.get("itemName")).trim()), k -> new ArrayList<>())
                    .add(new Line(str(r.get("code")), num(r.get("qty"))));
        }
        return out;
    }

    // ── 순수 계획 ────────────────────────────────────────────────

    /** 레거시 구성 행 → 새 규칙 후보. rows 는 {@code selectCandidates} 모양(주문번호·순번 순). */
    static List<Candidate> plan(List<Map<String, Object>> rows, Map<String, List<Line>> existing, String keyPrefix) {
        // 주문별로 모으고, 코드별 사용 여부·발주단가 이름을 기억한다
        Map<Integer, Order> orders = new LinkedHashMap<>();
        Map<String, Integer> useYn = new LinkedHashMap<>();
        Map<String, String> upriceName = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Integer orderNo = num(r.get("orderNo"));
            Order o = orders.computeIfAbsent(orderNo, k -> new Order(orderNo, str(r.get("prodName")), str(r.get("itemName")), num(r.get("orderQty"))));
            String code = str(r.get("code")).trim();
            o.comps.add(new Line(code, num(r.get("totalQty"))));
            if (!code.isEmpty()) {
                useYn.putIfAbsent(code, r.get("upriceUseYn") == null ? null : num(r.get("upriceUseYn")));
                upriceName.putIfAbsent(code, str(r.get("upriceName")));
            }
        }

        // 새 키(상품명·옵션, 출고지시가 쓰는 것처럼 앞뒤 공백 제거)별로 묶는다
        Map<String, List<Order>> byKey = new TreeMap<>();
        Map<String, boolean[]> trimmed = new LinkedHashMap<>();
        for (Order o : orders.values()) {
            String k = key(o.prodName.trim(), o.itemName.trim());
            byKey.computeIfAbsent(k, x -> new ArrayList<>()).add(o);
            if (!o.prodName.equals(o.prodName.trim()) || !o.itemName.equals(o.itemName.trim())) trimmed.put(k, new boolean[]{true});
        }

        List<Candidate> out = new ArrayList<>();
        for (Map.Entry<String, List<Order>> e : byKey.entrySet()) {
            Order first = e.getValue().get(0);
            String prod = first.prodName.trim();
            String item = first.itemName.trim();
            if (keyPrefix != null && !keyPrefix.isBlank() && !prod.startsWith(keyPrefix.trim())) continue;
            boolean keyTrimmed = trimmed.containsKey(e.getKey());

            List<List<Line>> decomps = new ArrayList<>();
            for (Order o : e.getValue()) {
                List<Line> d = decompose(o);
                if (d != null && !decomps.contains(d)) decomps.add(d);
            }
            List<Line> lines = decomps.size() == 1 ? decomps.get(0) : List.of();
            int gifts = 0;
            for (Line l : lines) if (upriceName.getOrDefault(l.code(), "").startsWith("힣")) gifts++;

            String status;
            if (decomps.isEmpty()) status = "ANOMALY_QTY";
            else if (decomps.size() > 1) status = "CONFLICT_LEGACY";
            else if (lines.size() > MAX_LINES) status = "TOO_MANY";
            else if (lines.stream().anyMatch(l -> l.code().isEmpty() || !useYn.containsKey(l.code()) || useYn.get(l.code()) == null)) status = "MISSING_CODE";
            else if (lines.stream().anyMatch(l -> l.qty() < 1 || l.qty() > MAX_QTY)) status = "ANOMALY_QTY";
            else if (lines.stream().anyMatch(l -> useYn.get(l.code()) != 1)) status = "INACTIVE_CODE";
            else {
                List<Line> ex = existing == null ? null : existing.get(key(prod, item));
                if (ex == null) status = "NEW";
                else status = ex.equals(lines) ? "SAME" : "CONFLICT_EXISTING";
            }
            out.add(new Candidate(prod, item, status, e.getValue().size(), lines, gifts, keyTrimmed, first.orderNo));
        }
        return out;
    }

    /** 한 주문의 구성 → 상품 1개분 줄(같은 코드는 합산). 수량이 주문수량으로 나누어떨어지지 않으면 null. */
    private static List<Line> decompose(Order o) {
        if (o.orderQty <= 0 || o.comps.isEmpty()) return null;
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (Line c : o.comps) {
            if (c.qty() <= 0 || c.qty() % o.orderQty != 0) return null;
            merged.merge(c.code(), c.qty() / o.orderQty, Integer::sum);
        }
        List<Line> out = new ArrayList<>();
        merged.forEach((code, qty) -> out.add(new Line(code, qty)));
        return out;
    }

    private static String key(String prod, String item) { return prod + "\u0000" + item; }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static int num(Object o) { return o instanceof Number n ? n.intValue() : 0; }

    private static final class Order {
        final Integer orderNo;
        final String prodName;
        final String itemName;
        final int orderQty;
        final List<Line> comps = new ArrayList<>();

        Order(Integer orderNo, String prodName, String itemName, int orderQty) {
            this.orderNo = orderNo;
            this.prodName = prodName;
            this.itemName = itemName;
            this.orderQty = orderQty;
        }
    }
}
