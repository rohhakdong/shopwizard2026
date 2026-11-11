package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("catalogProdItemService")
@RequiredArgsConstructor
public class ProdItemService {
    private final ProdItemMapper prodItemMapper;
    private final ProdMapper prodMapper;

    public List<ProdItem> selectList(Map<String, Object> params) { return prodItemMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodItemMapper.selectCount(params); }
    public ProdItem select(Map<String, Object> params) { return prodItemMapper.select(params); }
    public String selectItemCodeByAttrVal(Map<String, Object> params) { return prodItemMapper.selectItemCodeByAttrVal(params); }

    // 옵션을 바꾸면 게시된 상품(판매 상품이 있는 상품)은 판매 쪽 옵션도 같은 트랜잭션에서 맞춘다.
    // 판매 쪽 재고 수량(SupplyQty)은 주문 때 차감되므로 기존 옵션에서는 덮어쓰지 않는다.
    @Transactional
    public int insert(ProdItem prodItem) {
        int result = prodItemMapper.insert(prodItem);
        syncIfPublished(prodItem.getProdCode());
        return result;
    }
    @Transactional
    public int update(ProdItem prodItem) {
        int result = prodItemMapper.update(prodItem);
        syncIfPublished(prodItem.getProdCode());
        return result;
    }
    @Transactional
    public int updateItemlist(Map<String, Object> params) {
        int result = prodItemMapper.updateItemlist(params);
        syncIfPublished(params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode")));
        return result;
    }
    @Transactional
    public int delete(ProdItem prodItem) {
        int result = prodItemMapper.delete(prodItem);
        syncIfPublished(prodItem.getProdCode());
        return result;
    }
    @Transactional
    public int disable(String prodCode) {
        int result = prodItemMapper.disable(prodCode);
        syncIfPublished(prodCode);
        return result;
    }
    @Transactional
    public int enable(Map<String, Object> params) {
        int result = prodItemMapper.enable(params);
        syncIfPublished(params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode")));
        return result;
    }

    /**
     * 승인 게시 — 상품 본체(copy2shopion)가 먼저 복사되므로 이 시점엔 판매 상품이 이미 있다. 그래서 동기화(upsert)를 쓴다:
     * 처음 승인이면 옵션 전부가 새로 들어가고, 재승인·기존 상품이면 중복 키 오류 없이 내용만 갱신된다.
     * 판매 상품 자체가 없으면(본체 복사 없이 호출) 예전처럼 단순 복사한다.
     */
    @Transactional
    public int copy2Shopion(Map<String, Object> params) {
        String prodCode = params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode"));
        if (isPublished(prodCode)) return sync(prodCode);
        return prodItemMapper.copy2Shopion(params);
    }
    @Transactional public int delete2Shopion(String prodCode) { return prodItemMapper.delete2Shopion(prodCode); }

    private static final int DEFAULT_ITEM_CODE = 20000;
    private static final String PLACEHOLDER_ATTR_VAL = "선택사항없음";
    /** 레거시 규칙: 새 옵션의 재고는 초기값 9,999,999. */
    private static final int INITIAL_QTY = 9_999_999;

    /**
     * 속성값에서 만든 옵션 조합을 이 상품의 옵션 행에 반영한다 — 레거시 {@code ProdServiceImpl#setProdInfo} 의 규칙:
     * 같은 조합이 이미 있으면 다시 판매 상태로 켜고, 없으면 새로 추가하며, <b>목록에 없는 기존 옵션은 판매중지</b>한다.
     * 단 ItemCode 20000 "선택사항없음" 기본 행은 건드리지 않는다(쇼핑몰·주문이 이 행을 기본 행으로 다룬다).
     * 새 옵션은 기존 최대 ItemCode 다음 번호(20001, 20002 …)를 받는다. 게시된 상품이면 판매 쪽 옵션도 한 번에 맞춘다.
     *
     * @param prod   등록자/수정자와 리드타임을 읽는 상품(ProdCode 필수)
     * @param combos {@link AttrCombos#expand} 결과 — 각 원소는 {속성값1~4}
     * @return 새로 만든 옵션 수
     */
    @Transactional
    public int applyAttributeOptions(Prod prod, List<String[]> combos) {
        String prodCode = prod.getProdCode();
        Map<String, Object> q = new java.util.HashMap<>();
        q.put("pProdCode", prodCode);
        List<ProdItem> existing = prodItemMapper.selectList(q);
        String actorId = prod.getChangeId() != null ? prod.getChangeId() : prod.getRegistId();
        String actorName = prod.getChangeName() != null ? prod.getChangeName() : prod.getRegistName();

        Integer max = prodItemMapper.selectMaxItemCode(prodCode);
        int next = Math.max(max == null ? 0 : max, DEFAULT_ITEM_CODE) + 1;
        java.util.Set<Integer> keep = new java.util.HashSet<>();
        int created = 0;

        for (String[] c : combos) {
            ProdItem found = findByAttrVals(existing, c);
            if (found != null) {
                keep.add(found.getItemCode());
                if (!Integer.valueOf(1).equals(found.getSaleYn())) {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("prodCode", prodCode);
                    m.put("itemCode", found.getItemCode());
                    prodItemMapper.enable(m);
                }
            } else {
                ProdItem item = new ProdItem();
                item.setProdCode(prodCode);
                item.setItemCode(next++);
                item.setAttrVal1(c[0]); item.setAttrVal2(c[1]); item.setAttrVal3(c[2]); item.setAttrVal4(c[3]);
                item.setSupplyQty(INITIAL_QTY);
                item.setLeadTime(prod.getLeadTime() != null ? prod.getLeadTime() : 3);
                item.setLeadTimeReason(prod.getLeadTimeReason());
                item.setSaleYn(1);
                item.setSalePrice(0); item.setSupplyPrice(0); item.setBuyPrice(0); item.setOptionPrice(0);
                item.setState(0);
                item.setRegistId(actorId); item.setRegistName(actorName);
                item.setChangeId(actorId); item.setChangeName(actorName);
                prodItemMapper.insert(item);
                created++;
            }
        }

        for (ProdItem it : existing) {
            if (keep.contains(it.getItemCode()) || isPlaceholder(it) || Integer.valueOf(0).equals(it.getSaleYn())) continue;
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("prodCode", prodCode);
            m.put("itemCode", it.getItemCode());
            m.put("changeId", actorId);
            m.put("changeName", actorName);
            prodItemMapper.disableItem(m);
        }

        syncIfPublished(prodCode);
        return created;
    }

    /** 기본 행("선택사항없음")은 코드가 아니라 옵션값으로 알아본다 — 레거시 옵션 상품은 20000 이 실제 첫 번째 옵션이다. */
    private static boolean isPlaceholder(ProdItem it) {
        return PLACEHOLDER_ATTR_VAL.equals(it.getAttrVal1() == null ? null : it.getAttrVal1().trim());
    }

    private static ProdItem findByAttrVals(List<ProdItem> items, String[] c) {
        for (ProdItem it : items) {
            if (nz(it.getAttrVal1()).equals(c[0]) && nz(it.getAttrVal2()).equals(c[1])
                    && nz(it.getAttrVal3()).equals(c[2]) && nz(it.getAttrVal4()).equals(c[3])) return it;
        }
        return null;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void syncIfPublished(String prodCode) {
        if (isPublished(prodCode)) sync(prodCode);
    }

    private int sync(String prodCode) {
        prodItemMapper.deleteMissing2Shopion(prodCode);
        return prodItemMapper.upsert2Shopion(prodCode);
    }

    private boolean isPublished(String prodCode) {
        return prodCode != null && !prodCode.isBlank() && prodMapper.countShopion(prodCode) > 0;
    }
}
