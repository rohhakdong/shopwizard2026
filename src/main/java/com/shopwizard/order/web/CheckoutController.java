package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.CheckoutQuoteRequest;
import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.model.PricedLine;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.order.service.CheckoutPricingService;
import com.shopwizard.order.service.CheckoutService;
import com.shopwizard.payment.model.ApproveRequest;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.PaymentResult;
import com.shopwizard.payment.service.PaymentGateway;
import com.shopwizard.payment.service.PaymentGatewayRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;

/**
 * 고객 쇼핑몰(shop.html) 결제완료 콜백.
 * PG 결제 승인 + 주문 저장을 한 번의 요청으로 처리한다. PG(토스/KCP …)는 {@code pgCode} 로 고르고(없으면 기본 PG),
 * 승인·취소는 {@link PaymentGateway} 뒤에 숨는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/checkout")
public class CheckoutController {

    private final PaymentGatewayRegistry gatewayRegistry;
    private final CheckoutService checkoutService;
    private final CheckoutPricingService pricingService;

    /**
     * 주문서 견적. 결제창을 열기 전에 서버가 계산한 단가·배송비·합계를 받아 그 금액으로 결제한다
     * (클라이언트가 계산한 금액은 신뢰하지 않는다 — /order/checkout 이 같은 계산으로 다시 검증한다).
     * 비회원도 쓰는 공개 경로이며 부작용이 없다.
     */
    @PostMapping("/quote")
    public PricedOrder quote(@RequestBody CheckoutQuoteRequest req) {
        return pricingService.price(req.getOrderProds());
    }

    @PostMapping
    public Map<String, Object> checkout(@RequestBody CheckoutRequest req, HttpServletRequest request) throws Exception {
        PaymentGateway gateway = gatewayRegistry.require(req.getPgCode());
        if (req.getOrderId() == null || req.getOrderId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 정보가 올바르지 않습니다.");
        }
        if (req.getOrderProds() == null || req.getOrderProds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 상품 정보가 없습니다.");
        }

        // 같은 PG·같은 가맹점 주문번호로 이미 주문이 저장돼 있으면(재전송·새로고침) 새로 만들지 않고 그 주문을 돌려준다.
        Optional<Integer> existing = checkoutService.findExistingOrderNo(gateway.code(), req.getOrderId());
        if (existing.isPresent()) {
            return Map.of("success", true, "orderNo", existing.get());
        }

        // 금액은 클라이언트가 아니라 서버가 상품 테이블로 계산한다. 결제 승인(돈이 움직이는 단계) **전에** 비교해서,
        // 위조된 금액이면 승인 호출 자체를 하지 않는다. 라인 정보(상품명·가격·옵션·상점)도 서버 값으로 교체한다.
        PricedOrder priced = pricingService.price(req.getOrderProds());
        // 클라이언트가 보낸 금액(amount/totalAmt)은 참고값이다 — 보냈다면 서버 계산과 같아야 한다.
        if ((req.getAmount() != null && req.getAmount() != priced.getTotalAmt())
                || (req.getTotalAmt() != null && req.getTotalAmt().longValue() != priced.getTotalAmt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "주문 금액이 현재 상품 가격과 일치하지 않습니다. 주문서를 새로고침한 뒤 다시 시도해 주세요.");
        }
        req.setOrderProds(toServerLines(priced));
        req.setTotalAmt((int) priced.getTotalAmt());

        // 로그인 고객의 custId는 요청 바디의 값이 아닌 서버 세션에서 강제로 가져와,
        // 다른 고객 명의로 주문이 저장되는 것(IDOR)을 막는다. 로그인하지 않은 경우(세션 없음)는
        // 비회원 주문으로 간주 — 레거시 스키마의 CustId=0 관례를 그대로 따른다
        // (OrderProdMapper.xml의 pCustId='0' 분기, 비회원 주문조회에서 이 값으로 조회함).
        Integer custId = AuthSession.custId(request);
        req.setCustId(custId != null ? custId : 0);

        // 등록자(registId/registName)도 요청 바디가 아니라 세션에서 정한다. 로그인 고객은 본인 로그인ID/이름, 비회원은
        // 클라이언트가 보낸 값을 버려서 CheckoutService 가 기존 규칙(GUEST / 주문자명)을 적용하게 한다.
        Map<String, String> me = AuthSession.get(request, AuthSession.Type.CUST);
        if (me != null && custId != null && custId != 0) {
            String loginId = me.getOrDefault("loginId", "");
            String name = me.get("name");
            req.setRegistId(loginId);
            req.setRegistName(name == null || name.isBlank() ? loginId : name);
        } else {
            req.setRegistId(null);
            req.setRegistName(null);
        }

        // 1. PG 결제 승인 (DB 트랜잭션 밖에서 먼저 수행). 승인 금액은 서버가 계산한 합계로 PG 에 전달한다.
        ApproveRequest approve = new ApproveRequest();
        approve.setOrderCode(req.getOrderId());
        approve.setAmount(priced.getTotalAmt());
        approve.setPayMethod(req.getPayMethod());
        Map<String, String> payload = new HashMap<>();
        if (req.getPayload() != null) payload.putAll(req.getPayload());
        if (req.getPaymentKey() != null) payload.putIfAbsent("paymentKey", req.getPaymentKey());   // 하위 호환
        approve.setPayload(payload);
        PaymentResult paid = gateway.approve(approve);

        // 2. 주문 + 상품 + 결제 + 로그 저장 (트랜잭션). 실패 시 이미 승인된 결제를 취소해서
        //    "결제는 됐는데 주문은 없는" 상태가 남지 않도록 한다 (best-effort 보상 처리).
        try {
            int orderNo = checkoutService.checkout(req, paid);
            return Map.of("success", true, "orderNo", orderNo);
        } catch (Exception e) {
            try {
                CancelRequest cancel = new CancelRequest();
                cancel.setTradeNo(paid.getTradeNo());
                cancel.setReason("주문 저장 실패로 인한 자동 취소");
                gateway.cancel(cancel);
            } catch (Exception cancelEx) {
                // 취소 자체가 실패해도 원래 예외를 그대로 전달 — 로그로 남겨 수동 대사(reconciliation) 대상이 되게 한다.
                e.addSuppressed(cancelEx);
            }
            throw e;
        }
    }

    private static List<CheckoutProdItem> toServerLines(PricedOrder priced) {
        List<CheckoutProdItem> out = new ArrayList<>();
        for (PricedLine l : priced.getLines()) {
            CheckoutProdItem it = new CheckoutProdItem();
            it.setProdCode(l.getProdCode());
            it.setProdName(l.getProdName());
            it.setItemCode(l.getItemCode());
            it.setItemName(l.getItemName());
            it.setProdQty(l.getProdQty());
            it.setListPrice(l.getListPrice());
            it.setSalePrice(l.getSalePrice());
            it.setOptionPrice(l.getOptionPrice());
            it.setShopCode(l.getShopCode());
            out.add(it);
        }
        return out;
    }
}
