package com.shopwizard.order.service;

import com.shopwizard.order.mapper.BasketMapper;
import com.shopwizard.order.model.Basket;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 장바구니. 모든 메서드는 "로그인한 고객 본인(custId, 서버 세션에서 온 값)"의 행만 다룬다 —
 * 예전에는 클라이언트가 보낸 custId/basketNo 를 그대로 믿어 남의 장바구니를 읽고 지울 수 있었다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BasketService {

    /** tOrdBasket.ProdQty 가 smallint 라서 상한을 둔다. */
    static final int MAX_QTY = 999;

    private final BasketMapper basketMapper;

    public List<Basket> selectList(String custId) {
        return basketMapper.selectList(Map.of("custId", custId));
    }

    public Basket select(String custId, Integer basketNo) {
        return basketMapper.select(Map.of("custId", custId, "basketNo", basketNo));
    }

    public Basket selectByItem(String custId, String prodCode, String itemCode) {
        Map<String, Object> p = new HashMap<>();
        p.put("custId", custId);
        p.put("prodCode", prodCode);
        p.put("itemCode", blankToNull(itemCode));
        return basketMapper.selectByItem(p);
    }

    /**
     * 장바구니에 담는다. 같은 상품·옵션이 이미 있으면 새 행을 만들지 않고 수량을 합산한다.
     * 상품명/가격은 저장하지 않으므로(조회 시 상품 테이블에서 가져옴) 클라이언트가 보낸 값은 무시한다.
     */
    public Basket add(String custId, String loginId, String name, Basket req) {
        String prodCode = req.getProdCode() == null ? "" : req.getProdCode().trim();
        if (prodCode.isEmpty() || basketMapper.countProd(prodCode) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 상품입니다");
        }
        int qty = req.getProdQty() == null ? 1 : req.getProdQty();
        checkQty(qty);
        String itemCode = blankToNull(req.getItemCode());

        Basket existing = selectByItem(custId, prodCode, itemCode);
        if (existing != null) {
            int merged = Math.min(MAX_QTY, (existing.getProdQty() == null ? 0 : existing.getProdQty()) + qty);
            changeQty(custId, existing.getBasketNo(), merged, loginId, name);
            return select(custId, existing.getBasketNo());
        }

        Basket row = new Basket();
        row.setCustId(custId);
        row.setProdCode(prodCode);
        row.setItemCode(itemCode);
        row.setProdQty(qty);
        row.setChnlCode(req.getChnlCode() == null ? "" : req.getChnlCode());
        row.setRegistId(loginId);
        row.setRegistName(name);
        basketMapper.insert(row);
        return select(custId, row.getBasketNo());
    }

    public void changeQty(String custId, Integer basketNo, int qty, String loginId, String name) {
        checkQty(qty);
        Map<String, Object> p = new HashMap<>();
        p.put("custId", custId);
        p.put("basketNo", basketNo);
        p.put("prodQty", qty);
        p.put("changeId", loginId);
        p.put("changeName", name);
        if (basketMapper.update(p) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다");
        }
    }

    public void delete(String custId, Integer basketNo) {
        basketMapper.delete(Map.of("custId", custId, "basketNo", basketNo));
    }

    public void deleteMulti(String custId, List<Integer> basketNos) {
        if (basketNos == null || basketNos.isEmpty()) return;
        basketMapper.deleteMulti(Map.of("custId", custId, "pBasketNoArr", basketNos));
    }

    public void clean(String custId) {
        basketMapper.clean(Map.of("custId", custId));
    }

    private static void checkQty(int qty) {
        if (qty < 1 || qty > MAX_QTY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1~" + MAX_QTY + " 사이여야 합니다");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
