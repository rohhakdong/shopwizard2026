package com.shopwizard.order.web;

import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderCancelRequest;
import com.shopwizard.order.model.RefundConfirmRequest;
import com.shopwizard.order.service.OrderService;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/order")
public class OrderController {
    private final OrderService orderService;
    private final DataScopeGuard scopeGuard;   // 상점/채널 제한 관리자는 자기 주문만(목록은 DataScopeFilter+매퍼 조건)

    @GetMapping("/list")
    public List<Order> selectList(@RequestParam Map<String, Object> params) { return orderService.selectList(params); }

    @GetMapping("/orderlist")
    public List<Order> selectListOrderList(@RequestParam Map<String, Object> params) { return orderService.selectListOrderList(params); }

    @GetMapping("/orderlist/count")
    public int selectCountOrderList(@RequestParam Map<String, Object> params) { return orderService.selectCountOrderList(params); }

    @GetMapping("/pay/list")
    public List<Order> selectListPay(@RequestParam Map<String, Object> params) { return orderService.selectListPay(params); }

    @GetMapping
    public Order select(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        scopeGuard.requireOrder(request, toInt(params.containsKey("orderNo") ? params.get("orderNo") : params.get("pOrderNo")));
        return orderService.select(params);
    }

    // 주문 상태를 바꾸는 쓰기 엔드포인트는 아래 배치 게이트(cancel-batch, refund-confirm-batch)뿐이다.
    // 예전에 있던 PUT /state, /cancel, /refundcancel 과 POST(주문 임의 생성)는 상태 검증·재고 복원·
    // 토스 환불을 전부 건너뛸 수 있어(/cancel 은 orderState 를 빼면 NULL 을 기록) 제거했다.
    // 서버 내부(결제 웹훅 등)는 OrderService 를 직접 호출한다.

    /** 주문취소접수 화면(order-cancel.js) — 선택한 주문라인들을 일괄 주문취소 처리. */
    @PostMapping("/cancel-batch")
    public ShipDirectIssueResult cancelBatch(@RequestBody OrderCancelRequest req, HttpServletRequest request) {
        scopeGuard.requireLines(request, req.getLines(), OrderCancelRequest.Line::getOrderNo, OrderCancelRequest.Line::getOrderProdNo);
        return orderService.cancelBatch(req);
    }

    /** 환불확정 화면(order-refund-confirm.js) — 선택한 주문라인들을 일괄 환불완료 처리. */
    @PostMapping("/refund-confirm-batch")
    public ShipDirectIssueResult confirmRefundBatch(@RequestBody RefundConfirmRequest req, HttpServletRequest request) {
        scopeGuard.requireLines(request, req.getLines(), RefundConfirmRequest.Line::getOrderNo, RefundConfirmRequest.Line::getOrderProdNo);
        return orderService.confirmRefundBatch(req);
    }

    private static Integer toInt(Object v) {
        try { return v == null ? null : Integer.valueOf(String.valueOf(v).trim()); } catch (NumberFormatException e) { return null; }
    }
}
