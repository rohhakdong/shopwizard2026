package com.shopwizard.order.service;

import com.shopwizard.order.mapper.KeywordTargetMapper;
import com.shopwizard.order.mapper.OrderKeywrdMapper;
import com.shopwizard.order.model.OrderKeywrd;
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
 * 자동발주 "키워드로 대상 가져오기"(레거시 자동발주 2단계의 키워드 조건). 상점이 지정되지 않은 지불완료 라인 중 그 상점의
 * 키워드(tOrdOrderKeywrd, 사용 중)에 맞는 것을 찾아 상점으로 배정한다 — 배정되면 기존 자동발주 흐름(수수료 적용·출고지시·발주서)이 그대로 이어진다.
 *
 * <p>판정: 상점의 LIKE(상품명)/LIKE OPTION(옵션명) 키워드 중 하나라도 포함되고(대소문자 무시) NOT LIKE/NOT LIKE OPTION 키워드는 하나도
 * 포함되지 않으면 후보. 상점에 LIKE 계열 키워드가 없으면 후보가 없다(레거시와 같음). 이미 다른 상점에 배정된 라인은 건드리지 않고,
 * 여러 상점 키워드에 걸린 라인은 먼저 배정하는 상점이 가져간다(두 번째 상점에서는 이미 배정돼 후보에서 빠진다).</p>
 */
@Service
@RequiredArgsConstructor
public class KeywordTargetService {

    /** 샵피온몰(자체 쇼핑몰) 채널 — 레거시 자동발주가 대상에서 빼던 채널이다(자체몰 주문은 자동발주 대상이 아니다). */
    static final String OWN_MALL_CHNL_CODE = "1208674775Z2";
    static final int SAMPLE_SIZE = 30;

    private final KeywordTargetMapper mapper;
    private final OrderKeywrdMapper keywrdMapper;
    private final KeywordAssignWriter writer;

    public record Sample(Integer orderNo, Integer orderProdNo, String prodName, String itemName, List<String> otherShops) {}

    public record Preview(String shopCode, String shopName, int keywordCnt, int candidates, int alsoOtherShops, List<Sample> sample) {}

    public record AssignResult(String shopCode, int candidates, int assigned, int skipped) {}

    /** 한 상점의 키워드(소문자·공백 제거). */
    record ShopKeywords(String shopName, List<String> like, List<String> likeOption, List<String> notLike, List<String> notLikeOption) {
        int total() { return like.size() + likeOption.size() + notLike.size() + notLikeOption.size(); }
    }

    @Transactional(readOnly = true)
    public Preview preview(String shopCode) {
        String shop = requireShop(shopCode);
        String shopName = requireShopName(shop);
        Map<String, ShopKeywords> all = keywordsByShop(keywrdMapper.selectAllActive());
        List<Map<String, Object>> lines = mapper.selectUnassignedLines(shop, OWN_MALL_CHNL_CODE);
        ShopKeywords mine = all.get(shop);

        int candidates = 0, also = 0;
        List<Sample> sample = new ArrayList<>();
        if (mine != null) {
            for (Map<String, Object> l : lines) {
                String prod = str(l.get("prodName")), item = str(l.get("itemName"));
                if (!matches(mine, prod, item)) continue;
                candidates++;
                List<String> others = otherShops(all, shop, prod, item);
                if (!others.isEmpty()) also++;
                if (sample.size() < SAMPLE_SIZE) sample.add(new Sample(num(l.get("orderNo")), num(l.get("orderProdNo")), prod, item, others));
            }
        }
        return new Preview(shop, shopName, mine == null ? 0 : mine.total(), candidates, also, sample);
    }

    /** 후보를 건별로 배정한다. 미리보기를 다시 계산하므로 그 사이 다른 상점이 가져간 라인은 후보에서 빠지거나 건너뛴다. */
    public AssignResult assign(String shopCode, String actorId, String actorName) {
        String shop = requireShop(shopCode);
        String shopName = requireShopName(shop);
        ShopKeywords mine = keywordsByShop(keywrdMapper.selectAllActive()).get(shop);
        if (mine == null) return new AssignResult(shop, 0, 0, 0);

        int candidates = 0, assigned = 0, skipped = 0;
        for (Map<String, Object> l : mapper.selectUnassignedLines(shop, OWN_MALL_CHNL_CODE)) {
            if (!matches(mine, str(l.get("prodName")), str(l.get("itemName")))) continue;
            candidates++;
            boolean ok;
            try {
                ok = writer.assign(num(l.get("orderNo")), num(l.get("orderProdNo")), shop, shopName, actorId, actorName);
            } catch (Exception e) {
                ok = false;
            }
            if (ok) assigned++; else skipped++;
        }
        return new AssignResult(shop, candidates, assigned, skipped);
    }

    // ── 순수 판정 ────────────────────────────────────────────────

    static Map<String, ShopKeywords> keywordsByShop(List<OrderKeywrd> keywords) {
        Map<String, List<OrderKeywrd>> byShop = new LinkedHashMap<>();
        for (OrderKeywrd k : keywords) {
            if (k.getShopCode() == null || k.getKeywrdName() == null || k.getKeywrdName().trim().isEmpty()) continue;
            byShop.computeIfAbsent(k.getShopCode().trim(), x -> new ArrayList<>()).add(k);
        }
        Map<String, ShopKeywords> out = new LinkedHashMap<>();
        byShop.forEach((shop, list) -> {
            List<String> like = new ArrayList<>(), likeOpt = new ArrayList<>(), not = new ArrayList<>(), notOpt = new ArrayList<>();
            String name = null;
            for (OrderKeywrd k : list) {
                if (name == null) name = k.getShopName();
                String kw = k.getKeywrdName().trim().toLowerCase(Locale.ROOT);
                switch (k.getKeywrdSect() == null ? "" : k.getKeywrdSect().trim().toUpperCase(Locale.ROOT)) {
                    case "LIKE" -> like.add(kw);
                    case "LIKE OPTION" -> likeOpt.add(kw);
                    case "NOT LIKE" -> not.add(kw);
                    case "NOT LIKE OPTION" -> notOpt.add(kw);
                    default -> { /* 알 수 없는 구분은 무시 */ }
                }
            }
            out.put(shop, new ShopKeywords(name == null ? shop : name, like, likeOpt, not, notOpt));
        });
        return out;
    }

    /** 상품명/옵션이 이 상점의 후보인가. */
    static boolean matches(ShopKeywords k, String prodName, String itemName) {
        if (k.like().isEmpty() && k.likeOption().isEmpty()) return false;
        String prod = prodName == null ? "" : prodName.toLowerCase(Locale.ROOT);
        String item = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
        boolean hit = k.like().stream().anyMatch(prod::contains) || k.likeOption().stream().anyMatch(item::contains);
        if (!hit) return false;
        return k.notLike().stream().noneMatch(prod::contains) && k.notLikeOption().stream().noneMatch(item::contains);
    }

    private static List<String> otherShops(Map<String, ShopKeywords> all, String shop, String prod, String item) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, ShopKeywords> e : all.entrySet()) {
            if (e.getKey().equals(shop)) continue;
            if (matches(e.getValue(), prod, item)) out.add(e.getValue().shopName());
        }
        return out;
    }

    private static String requireShop(String shopCode) {
        String shop = shopCode == null ? "" : shopCode.trim();
        if (shop.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "상점을 선택하세요.");
        return shop;
    }

    private String requireShopName(String shop) {
        String name = mapper.selectShopName(shop);
        if (name == null) throw new ResponseStatusException(BAD_REQUEST, "없는 상점입니다: " + shop);
        return name;
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static Integer num(Object o) { return o instanceof Number n ? n.intValue() : null; }
}
