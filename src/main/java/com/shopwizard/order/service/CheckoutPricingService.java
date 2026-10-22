package com.shopwizard.order.service;

import com.shopwizard.order.mapper.CheckoutPricingMapper;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.PricedLine;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.order.model.PricingItem;
import com.shopwizard.order.model.PricingProd;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 스토어프론트 주문 금액을 서버가 상품 테이블 기준으로 계산한다. 예전에는 단가·합계·배송비를 클라이언트가 정해서
 * 보냈고 서버는 "토스 승인액 == 클라이언트가 보낸 합계" 만 확인했기 때문에, 1원으로 결제해도 주문이 접수됐다.
 *
 * <p>입력에서는 상품코드·옵션코드·수량만 쓴다. 단가는 {@code 상품 판매가 + 옵션 추가금}, 배송비는 선불(DeliFeeType) 상품만
 * 상점(ShopCode)별 최대값을 합산한다(무료·착불은 0). 할인가·쿠폰·포인트는 아직 없는 기능이라 반영하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutPricingService {

    static final int MAX_LINES = 50;
    /** 장바구니(BasketService.MAX_QTY)와 같은 수량 상한. */
    static final int MAX_QTY = 999;
    /** 옵션이 없는 상품의 기본 행(선택사항없음). */
    static final String DEFAULT_ITEM_CODE = "20000";
    static final String PREPAID = "선불";

    private final CheckoutPricingMapper mapper;

    public PricedOrder price(List<CheckoutProdItem> items) {
        if (items == null || items.isEmpty()) throw bad("주문 상품 정보가 없습니다.");
        if (items.size() > MAX_LINES) throw bad("한 번에 주문할 수 있는 상품은 최대 " + MAX_LINES + "개입니다.");

        List<PricedLine> lines = new ArrayList<>();
        Map<String, Integer> feeByShop = new HashMap<>();
        long itemAmt = 0;

        for (CheckoutProdItem in : items) {
            if (in == null) throw bad("주문 상품 정보가 올바르지 않습니다.");
            String prodCode = trim(in.getProdCode());
            if (prodCode == null) throw bad("상품코드가 없습니다.");
            int qty = in.getProdQty() == null ? 0 : in.getProdQty();
            if (qty < 1 || qty > MAX_QTY) throw bad("수량은 1~" + MAX_QTY + " 사이여야 합니다.");

            PricingProd prod = mapper.selectProd(prodCode);
            if (prod == null || !eq(prod.getState(), 1)) throw bad("존재하지 않는 상품입니다: " + prodCode);
            if (!eq(prod.getSaleYn(), 1)) throw bad("판매 중이 아닌 상품입니다: " + nameOf(prod));
            int salePrice = wholeWon(prod.getSalePrice(), "상품 가격");
            if (salePrice <= 0) throw bad("가격 문의 상품은 주문할 수 없습니다: " + nameOf(prod));

            PricingItem item = resolveItem(prod, trim(in.getItemCode()));
            int optionPrice = item == null ? 0 : wholeWon(item.getOptionPrice(), "옵션 가격");
            if (optionPrice < 0) throw bad("옵션 가격이 올바르지 않습니다: " + nameOf(prod));

            long unit = (long) salePrice + optionPrice;
            long lineAmt = unit * qty;
            if (unit > Integer.MAX_VALUE) throw bad("주문 금액이 너무 큽니다.");
            itemAmt += lineAmt;
            if (itemAmt > Integer.MAX_VALUE) throw bad("주문 금액이 너무 큽니다.");

            PricedLine line = new PricedLine();
            line.setProdCode(prod.getProdCode());
            line.setProdName(prod.getProdName());
            line.setShopCode(prod.getShopCode() == null ? "" : prod.getShopCode());
            line.setItemCode(item == null ? null : item.getItemCode());
            line.setItemName(item == null ? null : item.getItemName());
            line.setProdQty(qty);
            line.setListPrice(wholeWon(prod.getListPrice(), "정가"));
            line.setSalePrice(salePrice);
            line.setOptionPrice(optionPrice);
            line.setUnitPrice((int) unit);
            line.setLineAmt(lineAmt);
            lines.add(line);

            // 배송비: 선불 상품만, 같은 상점(출고지)은 가장 큰 값 하나
            if (PREPAID.equals(prod.getDeliFeeType())) {
                int fee = Math.max(0, wholeWon(prod.getDeliFeeAmt(), "배송비"));
                feeByShop.merge(line.getShopCode(), fee, Math::max);
            }
        }

        long deli = 0;
        for (int fee : feeByShop.values()) deli += fee;
        if (itemAmt + deli > Integer.MAX_VALUE) throw bad("주문 금액이 너무 큽니다.");

        PricedOrder order = new PricedOrder();
        order.setLines(lines);
        order.setItemAmt(itemAmt);
        order.setDeliAmt((int) deli);
        order.setTotalAmt(itemAmt + deli);
        return order;
    }

    /**
     * 주문 라인의 옵션 행을 정한다. 옵션코드를 보냈으면 그 상품의 판매 가능한 옵션이어야 하고, 안 보냈거나 기본행(20000)이면
     * 진짜 옵션이 없는 상품일 때만 허용한다(옵션 상품을 옵션 선택 없이 사는 것을 막는다).
     */
    private PricingItem resolveItem(PricingProd prod, String itemCode) {
        boolean wantsDefault = itemCode == null || DEFAULT_ITEM_CODE.equals(itemCode);
        if (wantsDefault && mapper.countRealOptions(prod.getProdCode()) > 0) {
            throw bad("옵션을 선택해 주세요: " + nameOf(prod));
        }
        // 옵션코드를 안 보낸 옵션 없는 상품은 기존 데이터 관례대로 기본행(20000)을 붙인다(기본행이 없으면 옵션 없이 진행).
        PricingItem item = mapper.selectItem(prod.getProdCode(), itemCode == null ? DEFAULT_ITEM_CODE : itemCode);
        if (item == null) {
            if (itemCode == null) return null;
            throw bad("존재하지 않는 옵션입니다: " + nameOf(prod));
        }
        if (!eq(item.getSaleYn(), 1) || (item.getSupplyQty() != null && item.getSupplyQty() <= 0)) {
            throw bad("품절된 옵션입니다: " + nameOf(prod));
        }
        return item;
    }

    private static int wholeWon(BigDecimal v, String what) {
        if (v == null) return 0;
        try {
            return v.setScale(0, java.math.RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException e) {
            throw bad(what + "이 올바르지 않습니다.");
        }
    }

    private static boolean eq(Integer v, int expected) {
        return v != null && v == expected;
    }

    private static String nameOf(PricingProd p) {
        return p.getProdName() == null ? p.getProdCode() : p.getProdName();
    }

    private static String trim(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
