package com.shopwizard.framework.auth;

import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.function.Function;

/**
 * 상점/채널/서비스로 제한된 관리자({@link DataScope})가 남의 행을 건드리지 못하게 하는 소유권 검사. 조회 목록은
 * {@link DataScopeFilter} 가 파라미터로 거르고, 여기서는 파라미터로 걸리지 않는 단건 조회와 쓰기(주문번호·상품코드 같은
 * 키만 알면 호출되는 엔드포인트)를 막는다. 아무 제한도 없는 계정(전체 권한)은 DB 조회 없이 바로 통과한다.
 *
 * <p>소유가 아니면 존재하지 않는 행과 똑같이 403 — 남의 주문번호가 존재하는지도 알려주지 않는다. 서비스는 테넌트 경계라
 * 상점/채널 제한이 없는 계정도 다른 서비스의 행은 처리할 수 없다(마스터 데이터 쓰기는 {@code require*Svc} / {@link #svcFor}).</p>
 */
@Component
@RequiredArgsConstructor
public class DataScopeGuard {

    private static final String DENIED = "다른 업체(채널)의 데이터는 처리할 수 없습니다.";
    private static final String DENIED_SVC = "다른 서비스의 데이터는 처리할 수 없습니다.";

    private final DataScopeMapper mapper;

    // off 면 검사하지 않는다(RoleAccessInterceptor 와 같은 플래그).
    @Value("${app.authz.mode:enforce}")
    private String mode;

    public DataScope scope(HttpServletRequest request) {
        if ("off".equalsIgnoreCase(mode == null ? "" : mode.trim())) return DataScope.UNRESTRICTED;
        return DataScope.of(request);
    }

    /** 상점·채널로 제한된 계정이 할 수 없는 업무(정산 생성·마감처럼 여러 상점에 걸치는 쓰기)는 제한 없는 계정만. */
    public void requireUnscoped(HttpServletRequest request) {
        if (scope(request).scoped()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점·채널이 지정된 계정은 이 작업을 할 수 없습니다.");
        }
    }

    /** 서비스 경계를 넘는 업무(서비스 마스터, 역할 권한 체계)는 전체 권한 계정만. */
    public void requireAllServices(HttpServletRequest request) {
        if (scope(request).svcScoped()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "전체 권한 계정만 할 수 있는 작업입니다.");
        }
    }

    /** 주문라인(OrderNo, OrderProdNo)이 내 상점/채널/서비스 것인가. */
    public void requireLine(HttpServletRequest request, Integer orderNo, Integer orderProdNo) {
        DataScope s = scope(request);
        if (!s.anyScoped()) return;
        if (orderNo == null || orderProdNo == null
                || mapper.countLines(orderNo, orderProdNo, s.shopCode(), s.chnlCode(), s.svcCode()) == 0) throw denied();
    }

    /** 배치 요청의 모든 라인이 내 것이어야 한다 — 하나라도 남의 것이면 전체를 거부한다(화면은 자기 라인만 보낸다). */
    public <L> void requireLines(HttpServletRequest request, Collection<L> lines,
                                 Function<L, Integer> orderNo, Function<L, Integer> orderProdNo) {
        if (lines == null || !scope(request).anyScoped()) return;
        for (L line : lines) requireLine(request, orderNo.apply(line), orderProdNo.apply(line));
    }

    /** 주문 헤더(고객 정보 포함): 내 상점 라인이 있는 주문이거나 내 채널 주문(그리고 내 서비스 채널). */
    public void requireOrder(HttpServletRequest request, Integer orderNo) {
        DataScope s = scope(request);
        if (!s.anyScoped()) return;
        if (orderNo == null || mapper.countOrder(orderNo, s.shopCode(), s.chnlCode(), s.svcCode()) == 0) throw denied();
    }

    /** 카탈로그 상품(tCatProd) 소유 — 상점 기준(상품에는 채널이 없다), 서비스는 상품 상점의 서비스. */
    public void requireCatProd(HttpServletRequest request, String prodCode) {
        DataScope s = scope(request);
        if (s.shopCode() == null && s.svcCode() == null) return;
        if (prodCode == null || mapper.countCatProd(prodCode, s.shopCode(), s.svcCode()) == 0) throw denied();
    }

    /** 판매 상품(shopion.tPrdProd) 소유 — 상점 기준, 서비스는 상품 상점의 서비스. */
    public void requirePrdProd(HttpServletRequest request, String prodCode) {
        DataScope s = scope(request);
        if (s.shopCode() == null && s.svcCode() == null) return;
        if (prodCode == null || mapper.countPrdProd(prodCode, s.shopCode(), s.svcCode()) == 0) throw denied();
    }

    public void requireAdjust(HttpServletRequest request, Integer adjustNo) {
        DataScope s = scope(request);
        if (!s.anyScoped()) return;
        if (adjustNo == null || mapper.countAdjust(String.valueOf(adjustNo), s.shopCode(), s.chnlCode(), s.svcCode()) == 0) throw denied();
    }

    /**
     * 이미 조회한 단건 행의 shopCode/chnlCode 가 내 범위인가(행이 없으면 통과 — 새어나갈 게 없다).
     * 서비스 제한 계정은 행의 채널(없으면 상점)이 내 서비스에 속해야 한다.
     */
    public void requireRow(HttpServletRequest request, Object row, String rowShopCode, String rowChnlCode) {
        DataScope s = scope(request);
        if (!s.anyScoped() || row == null) return;
        if (s.shopCode() != null && !s.shopCode().equals(rowShopCode)) throw denied();
        if (s.chnlCode() != null && !s.chnlCode().equals(rowChnlCode)) throw denied();
        if (s.svcCode() != null) {
            boolean ok = (rowChnlCode != null && !rowChnlCode.isBlank() && mapper.countChnlInSvc(rowChnlCode, s.svcCode()) > 0)
                    || (rowShopCode != null && !rowShopCode.isBlank() && mapper.countShopInSvc(rowShopCode, s.svcCode()) > 0);
            if (!ok) throw denied();
        }
    }

    /** 신규 등록/수정 시 body 의 상점코드를 믿지 않는다 — 상점 제한 계정은 항상 자기 상점. */
    public String shopFor(HttpServletRequest request, String requested) {
        String shop = scope(request).shopCode();
        return shop != null ? shop : requested;
    }

    /** 마스터 데이터 등록/수정 시 body 의 서비스코드를 믿지 않는다 — 서비스 제한 계정은 항상 자기 서비스(다른 서비스로 옮길 수 없다). */
    public String svcFor(HttpServletRequest request, String requested) {
        String svc = scope(request).svcCode();
        return svc != null ? svc : requested;
    }

    // ── 마스터 데이터 단건 소유(서비스) 검사: 없는 행이거나 다른 서비스 행이면 403 ──
    public void requireShopSvc(HttpServletRequest request, String shopCode) {
        String svc = scope(request).svcCode();
        if (svc != null && (shopCode == null || mapper.countShopInSvc(shopCode, svc) == 0)) throw deniedSvc();
    }

    public void requireChnlSvc(HttpServletRequest request, String chnlCode) {
        String svc = scope(request).svcCode();
        if (svc != null && (chnlCode == null || mapper.countChnlInSvc(chnlCode, svc) == 0)) throw deniedSvc();
    }

    public void requireCompSvc(HttpServletRequest request, String compCode) {
        String svc = scope(request).svcCode();
        if (svc != null && (compCode == null || mapper.countCompInSvc(compCode, svc) == 0)) throw deniedSvc();
    }

    public void requireSupplySvc(HttpServletRequest request, String supplyCode) {
        String svc = scope(request).svcCode();
        if (svc != null && (supplyCode == null || mapper.countSupplyInSvc(supplyCode, svc) == 0)) throw deniedSvc();
    }

    public void requireSaleCompSvc(HttpServletRequest request, String saleCompCode) {
        String svc = scope(request).svcCode();
        if (svc != null && (saleCompCode == null || mapper.countSaleCompInSvc(saleCompCode, svc) == 0)) throw deniedSvc();
    }

    public void requireWarehsSvc(HttpServletRequest request, String warehsCode) {
        String svc = scope(request).svcCode();
        if (svc != null && (warehsCode == null || mapper.countWarehsInSvc(warehsCode, svc) == 0)) throw deniedSvc();
    }

    public void requireBrandSvc(HttpServletRequest request, Integer brandId) {
        String svc = scope(request).svcCode();
        if (svc != null && (brandId == null || mapper.countBrandInSvc(brandId, svc) == 0)) throw deniedSvc();
    }

    public void requireRoleSvc(HttpServletRequest request, String roleUid) {
        String svc = scope(request).svcCode();
        if (svc != null && (roleUid == null || mapper.countRoleInSvc(roleUid, svc) == 0)) throw deniedSvc();
    }

    /** 관리자 계정의 역할이 내 서비스 것인가. */
    public void requireMngrSvc(HttpServletRequest request, String mngrUid) {
        String svc = scope(request).svcCode();
        if (svc != null && (mngrUid == null || mapper.countMngrInSvc(mngrUid, svc) == 0)) throw deniedSvc();
    }

    private static ResponseStatusException denied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, DENIED);
    }

    private static ResponseStatusException deniedSvc() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, DENIED_SVC);
    }
}
