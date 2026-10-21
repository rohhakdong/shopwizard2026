package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.model.OrderDashBoard;
import com.shopwizard.order.model.OrderDetlStats;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.service.OrderProdService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/orderprod")
public class OrderProdController {
    private final OrderProdService orderProdService;
    private final DataScopeGuard scopeGuard;   // 상점/채널 제한 관리자는 자기 라인만

    /** 관리자 전용 (mngr_loginId 인증). 예: 주문상세 모달에서 pOrderNo로 라인 조회 (order-list.js). */
    @GetMapping("/list")
    public List<OrderProd> selectList(@RequestParam Map<String, Object> params) {
        return orderProdService.selectList(params);
    }

    @GetMapping("/list/count")
    public int selectListCount(@RequestParam Map<String, Object> params) {
        return orderProdService.selectListCount(params);
    }

    /**
     * 대시보드 "배송 대기" — 지불완료/출고지시/발주확인 라인 수. 어느 서비스의 건수인지는 요청 파라미터가 아니라
     * 로그인한 관리자 세션의 svcCode 로만 정한다(클라이언트가 보낸 값은 쓰지 않는다).
     */
    @GetMapping("/state-count")
    public Map<String, Integer> selectStateCount(HttpServletRequest request) {
        Map<String, String> me = AuthSession.get(request, AuthSession.Type.MNGR);
        return orderProdService.selectPendingShipCounts(me == null ? null : me.get("svcCode"), scopeGuard.scope(request));
    }

    /**
     * 고객 전용 (cust_id 인증, shop.html 마이페이지 "내 주문내역"). 관리자용 /list 와 경로를
     * 분리한 이유: AuthInterceptor("cust_id")가 이 경로에 걸리는데, 관리자 세션은 cust_id
     * 쿠키가 없어 /list 에 같이 걸어두면 관리자의 주문상세 조회가 401로 막히기 때문.
     *
     * 클라이언트가 보낸 pCustId는 신뢰하지 않고 서버 세션의 로그인 고객 custId로 강제 덮어써서,
     * 다른 고객의 주문내역을 조회하는 것(IDOR)을 막는다.
     */
    @GetMapping("/my/list")
    public List<OrderProd> selectMyList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceCustIdFromSession(params, request);
        return orderProdService.selectList(params);
    }

    @GetMapping("/my/list/count")
    public int selectMyListCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceCustIdFromSession(params, request);
        return orderProdService.selectListCount(params);
    }

    /**
     * 비회원 주문조회 (인증 불필요). 주문번호+연락처가 둘 다 정확히 일치해야만 조회되며
     * (selectGuestOrderList 참고), CustId=0(비회원)으로 저장된 주문만 대상이다.
     */
    @GetMapping("/guest/list")
    public List<OrderProd> selectGuestList(@RequestParam Map<String, Object> params) {
        Object orderNo = params.get("pOrderNo");
        Object phoneNo = params.get("pOrderPhoneNo");
        if (orderNo == null || String.valueOf(orderNo).isBlank()
                || phoneNo == null || String.valueOf(phoneNo).isBlank()) {
            return List.of();
        }
        return orderProdService.selectGuestOrderList(params);
    }

    private void forceCustIdFromSession(Map<String, Object> params, HttpServletRequest request) {
        Integer custId = AuthSession.custId(request);
        // 로그인 고객이 아니면(인터셉터가 이미 막지만 이중 방어) 절대 매칭될 수 없는 값을 넣어
        // 클라이언트가 보낸 pCustId로 남의 주문이 조회되지 않게 한다.
        params.put("pCustId", custId != null ? custId.toString() : "-1");
    }

    /**
     * 상점(거래처) 계정 전용 (SHOP 로그인 필요, WebMvcConfig 참고). 로그인한 상점 자신에게
     * 배정된 주문 라인만 조회한다 — 관리자용 /list, 회원용 /my/list 와 마찬가지로 클라이언트가
     * 보낸 pShopCode는 신뢰하지 않고 서버 세션의 상점코드로 강제 덮어써서 다른 상점의 주문을
     * 조회하는 것(IDOR)을 막는다.
     */
    @GetMapping("/shop/list")
    public List<OrderProd> selectShopList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceShopCodeFromSession(params, request);
        return orderProdService.selectList(params);
    }

    @GetMapping("/shop/list/count")
    public int selectShopListCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceShopCodeFromSession(params, request);
        return orderProdService.selectListCount(params);
    }

    private void forceShopCodeFromSession(Map<String, Object> params, HttpServletRequest request) {
        Map<String, String> claims = AuthSession.get(request, AuthSession.Type.SHOP);
        if (claims == null || claims.get("shopCode") == null || claims.get("shopCode").isEmpty()) {
            // 로그인 상점이 아니면 절대 매칭될 수 없는 값을 넣어 전체 목록이 새어나가지 않게 한다
            // (AuthInterceptor가 이미 로그인 여부를 검사하지만, 이중 방어 차원).
            params.put("pShopCode", "__NONE__");
            return;
        }
        params.put("pShopCode", claims.get("shopCode"));
        // selectList/selectListCount는 pSvcCode 유무로 결제(OPay)/상품(P)/상점(S) 조인을
        // 붙일지 결정하는 공용 쿼리라(관리자용 order-list.js는 항상 svcCode를 실어 보냄),
        // 여기서도 실어주지 않으면 SELECT 절이 참조하는 컬럼이 없어 SQL 오류가 난다.
        String svcCode = claims.get("svcCode");
        if (svcCode != null && !svcCode.isEmpty()) {
            params.put("pSvcCode", svcCode);
        }
    }

    @GetMapping
    public OrderProd select(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        scopeGuard.requireLine(request, toInt(params.get("orderNo")), toInt(params.get("orderProdNo")));
        return orderProdService.select(params);
    }

    private static Integer toInt(Object v) {
        try { return v == null ? null : Integer.valueOf(String.valueOf(v).trim()); } catch (NumberFormatException e) { return null; }
    }

    // 주문라인의 생성·삭제·상태 변경(POST/PUT/DELETE, /state, /state/error, /returnreason, /refundcancel,
    // /shopinfo, /invoice)은 HTTP 로 열어두지 않는다 — 상태는 order/ship 의 배치 게이트 서비스만 바꾼다.
    // (화면은 이 엔드포인트들을 호출하지 않는다. 서버 내부는 OrderProdService/Mapper 를 직접 호출한다.)

    @GetMapping("/mypage/list")
    public List<OrderProd> mypageSelectList(@RequestParam Map<String, Object> params) { return orderProdService.mypageSelectList(params); }

    @GetMapping("/count/cust/{custId}")
    public int countByCustId(@PathVariable Integer custId) { return orderProdService.countByCustId(custId); }

    @GetMapping("/monthly/amt/{custId}")
    public int selectMonthlyTotalAmt(@PathVariable Integer custId) { return orderProdService.selectMonthlyTotalAmt(custId); }

    @GetMapping("/monthly/amt/dept")
    public int selectMonthlyTotalAmtDeptAccnt(@RequestParam Map<String, Object> params) { return orderProdService.selectMonthlyTotalAmtDeptAccnt(params); }

    @GetMapping("/dashboard")
    public List<OrderDashBoard> selectListDashboardAnyb(@RequestParam Map<String, Object> params) { return orderProdService.selectListDashboardAnyb(params); }

    @GetMapping("/count/state")
    public int countOrderStateDiffer(@RequestParam Map<String, Object> params) { return orderProdService.countOrderStateDiffer(params); }

    @GetMapping("/statistics/prod")
    public List<OrderDetlStats> selectListStatisticsProd(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsProd(params); }

    @GetMapping("/statistics/shop")
    public List<OrderDetlStats> selectListStatisticsShop(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsShop(params); }

    @GetMapping("/statistics/chnl")
    public List<OrderDetlStats> selectListStatisticsChnl(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsChnl(params); }

    @GetMapping("/statistics/chnlshop")
    public List<OrderDetlStats> selectListStatisticsChnlShop(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsChnlShop(params); }

    @GetMapping("/statistics/chnlprod")
    public List<OrderDetlStats> selectListStatisticsChnlProd(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsChnlProd(params); }
}
