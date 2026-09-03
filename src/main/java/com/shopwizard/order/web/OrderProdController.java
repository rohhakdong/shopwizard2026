package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderDashBoard;
import com.shopwizard.order.model.OrderDetlStats;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.service.OrderProdService;
import jakarta.servlet.http.Cookie;
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
     * 고객 전용 (cust_id 인증, shop.html 마이페이지 "내 주문내역"). 관리자용 /list 와 경로를
     * 분리한 이유: AuthInterceptor("cust_id")가 이 경로에 걸리는데, 관리자 세션은 cust_id
     * 쿠키가 없어 /list 에 같이 걸어두면 관리자의 주문상세 조회가 401로 막히기 때문.
     *
     * 클라이언트가 보낸 pCustId는 신뢰하지 않고 cust_id 쿠키 값으로 강제 덮어써서, 다른 고객의
     * 주문내역을 조회하는 것(IDOR)을 막는다.
     */
    @GetMapping("/my/list")
    public List<OrderProd> selectMyList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceCustIdFromCookie(params, request);
        return orderProdService.selectList(params);
    }

    @GetMapping("/my/list/count")
    public int selectMyListCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceCustIdFromCookie(params, request);
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

    private void forceCustIdFromCookie(Map<String, Object> params, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie c : cookies) {
            if ("cust_id".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                params.put("pCustId", c.getValue());
                return;
            }
        }
    }

    /**
     * 상점(거래처) 계정 전용 (shop_code 인증, WebMvcConfig 참고). 로그인한 상점 자신에게
     * 배정된 주문 라인만 조회한다 — 관리자용 /list, 회원용 /my/list 와 마찬가지로 클라이언트가
     * 보낸 pShopCode는 신뢰하지 않고 shop_code 쿠키 값으로 강제 덮어써서 다른 상점의 주문을
     * 조회하는 것(IDOR)을 막는다.
     */
    @GetMapping("/shop/list")
    public List<OrderProd> selectShopList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceShopCodeFromCookie(params, request);
        return orderProdService.selectList(params);
    }

    @GetMapping("/shop/list/count")
    public int selectShopListCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        forceShopCodeFromCookie(params, request);
        return orderProdService.selectListCount(params);
    }

    private void forceShopCodeFromCookie(Map<String, Object> params, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) { params.put("pShopCode", "__NONE__"); return; }
        boolean foundShopCode = false;
        for (Cookie c : cookies) {
            if ("shop_code".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                params.put("pShopCode", c.getValue());
                foundShopCode = true;
            }
            // selectList/selectListCount는 pSvcCode 유무로 결제(OPay)/상품(P)/상점(S) 조인을
            // 붙일지 결정하는 공용 쿼리라(관리자용 order-list.js는 항상 svcCode를 실어 보냄),
            // 여기서도 실어주지 않으면 SELECT 절이 참조하는 컬럼이 없어 SQL 오류가 난다.
            if ("shop_svcCode".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                params.put("pSvcCode", c.getValue());
            }
        }
        if (!foundShopCode) {
            // 쿠키가 없으면 절대 매칭될 수 없는 값을 넣어 전체 목록이 새어나가지 않게 한다
            // (AuthInterceptor가 이미 shop_code 존재를 검사하지만, 이중 방어 차원).
            params.put("pShopCode", "__NONE__");
        }
    }

    @GetMapping
    public OrderProd select(@RequestParam Map<String, Object> params) { return orderProdService.select(params); }

    @PostMapping
    public void insert(@RequestBody OrderProd orderProd) { orderProdService.insert(orderProd); }

    @PutMapping
    public void update(@RequestBody OrderProd orderProd) { orderProdService.update(orderProd); }

    @PutMapping("/price")
    public void updatePrice(@RequestBody OrderProd orderProd) { orderProdService.updatePrice(orderProd); }

    @PutMapping("/price/shipdirect")
    public void updatePriceShipDirect(@RequestBody OrderProd orderProd) { orderProdService.updatePriceShipDirect(orderProd); }

    @PutMapping("/price/shipdirectprint")
    public void updatePriceShipDirectPrint(@RequestBody OrderProd orderProd) { orderProdService.updatePriceShipDirectPrint(orderProd); }

    @PutMapping("/price/adjustdetail")
    public void updatePriceAdjustDetail(@RequestBody OrderProd orderProd) { orderProdService.updatePriceAdjustDetail(orderProd); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { orderProdService.delete(params); }

    @GetMapping("/mypage/list")
    public List<OrderProd> mypageSelectList(@RequestParam Map<String, Object> params) { return orderProdService.mypageSelectList(params); }

    @PutMapping("/mypage")
    public void mypageUpdate(@RequestBody Map<String, Object> params) { orderProdService.mypageUpdate(params); }

    @GetMapping("/count/cust/{custId}")
    public int countByCustId(@PathVariable Integer custId) { return orderProdService.countByCustId(custId); }

    @GetMapping("/monthly/amt/{custId}")
    public int selectMonthlyTotalAmt(@PathVariable Integer custId) { return orderProdService.selectMonthlyTotalAmt(custId); }

    @GetMapping("/monthly/amt/dept")
    public int selectMonthlyTotalAmtDeptAccnt(@RequestParam Map<String, Object> params) { return orderProdService.selectMonthlyTotalAmtDeptAccnt(params); }

    @GetMapping("/dashboard")
    public List<OrderDashBoard> selectListDashboardAnyb(@RequestParam Map<String, Object> params) { return orderProdService.selectListDashboardAnyb(params); }

    @PutMapping("/returnreason")
    public void updateReturnReason(@RequestBody OrderProd orderProd) { orderProdService.updateReturnReason(orderProd); }

    @PutMapping("/state")
    public void updateOrderState(@RequestBody Map<String, Object> params) { orderProdService.updateOrderState(params); }

    @PutMapping("/state/error")
    public void updateOrderStateError(@RequestBody Map<String, Object> params) { orderProdService.updateOrderStateError(params); }

    @PutMapping("/refundcancel")
    public void updateRefundCancel(@RequestBody Map<String, Object> params) { orderProdService.updateRefundCancel(params); }

    @PutMapping("/shopinfo")
    public void updateShopInfo(@RequestBody Map<String, Object> params) { orderProdService.updateShopInfo(params); }

    @PutMapping("/invoice")
    public void updateInvoice(@RequestBody Map<String, Object> params) { orderProdService.updateInvoice(params); }

    @PutMapping("/custconsltyn")
    public void updateCustConsltYn(@RequestBody Map<String, Object> params) { orderProdService.updateCustConsltYn(params); }

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
