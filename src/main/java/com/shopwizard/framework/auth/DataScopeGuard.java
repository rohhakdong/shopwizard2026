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
 * 상점/채널로 제한된 관리자({@link DataScope})가 남의 행을 건드리지 못하게 하는 소유권 검사. 조회 목록은
 * {@link DataScopeFilter} 가 파라미터로 거르고, 여기서는 파라미터로 걸리지 않는 단건 조회와 쓰기(주문번호·상품코드 같은
 * 키만 알면 호출되는 엔드포인트)를 막는다. 스코프가 없으면(코드가 비어 있는 계정) DB 조회 없이 바로 통과한다.
 *
 * <p>소유가 아니면 존재하지 않는 행과 똑같이 403 — 남의 주문번호가 존재하는지도 알려주지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class DataScopeGuard {

    private static final String DENIED = "다른 업체(채널)의 데이터는 처리할 수 없습니다.";

    private final DataScopeMapper mapper;

    // off 면 검사하지 않는다(RoleAccessInterceptor 와 같은 플래그).
    @Value("${app.authz.mode:enforce}")
    private String mode;

    public DataScope scope(HttpServletRequest request) {
        if ("off".equalsIgnoreCase(mode == null ? "" : mode.trim())) return DataScope.UNRESTRICTED;
        return DataScope.of(request);
    }

    /** 주문라인(OrderNo, OrderProdNo)이 내 상점/채널 것인가. */
    public void requireLine(HttpServletRequest request, Integer orderNo, Integer orderProdNo) {
        DataScope s = scope(request);
        if (!s.scoped()) return;
        if (orderNo == null || orderProdNo == null
                || mapper.countLines(orderNo, orderProdNo, s.shopCode(), s.chnlCode()) == 0) throw denied();
    }

    /** 배치 요청의 모든 라인이 내 것이어야 한다 — 하나라도 남의 것이면 전체를 거부한다(화면은 자기 라인만 보낸다). */
    public <L> void requireLines(HttpServletRequest request, Collection<L> lines,
                                 Function<L, Integer> orderNo, Function<L, Integer> orderProdNo) {
        if (lines == null || !scope(request).scoped()) return;
        for (L line : lines) requireLine(request, orderNo.apply(line), orderProdNo.apply(line));
    }

    /** 주문 헤더(고객 정보 포함): 내 상점 라인이 있는 주문이거나 내 채널 주문. */
    public void requireOrder(HttpServletRequest request, Integer orderNo) {
        DataScope s = scope(request);
        if (!s.scoped()) return;
        if (orderNo == null || mapper.countOrder(orderNo, s.shopCode(), s.chnlCode()) == 0) throw denied();
    }

    /** 카탈로그 상품(tCatProd) 소유 — 상점 기준(상품에는 채널이 없다). */
    public void requireCatProd(HttpServletRequest request, String prodCode) {
        DataScope s = scope(request);
        if (s.shopCode() == null) return;
        if (prodCode == null || mapper.countCatProd(prodCode, s.shopCode()) == 0) throw denied();
    }

    /** 판매 상품(shopion.tPrdProd) 소유 — 상점 기준. */
    public void requirePrdProd(HttpServletRequest request, String prodCode) {
        DataScope s = scope(request);
        if (s.shopCode() == null) return;
        if (prodCode == null || mapper.countPrdProd(prodCode, s.shopCode()) == 0) throw denied();
    }

    public void requireAdjust(HttpServletRequest request, Integer adjustNo) {
        DataScope s = scope(request);
        if (!s.scoped()) return;
        if (adjustNo == null || mapper.countAdjust(String.valueOf(adjustNo), s.shopCode(), s.chnlCode()) == 0) throw denied();
    }

    /** 이미 조회한 단건 행의 shopCode/chnlCode 가 내 범위인가(행이 없으면 통과 — 새어나갈 게 없다). */
    public void requireRow(HttpServletRequest request, Object row, String rowShopCode, String rowChnlCode) {
        DataScope s = scope(request);
        if (!s.scoped() || row == null) return;
        if (s.shopCode() != null && !s.shopCode().equals(rowShopCode)) throw denied();
        if (s.chnlCode() != null && !s.chnlCode().equals(rowChnlCode)) throw denied();
    }

    /** 신규 등록/수정 시 body 의 상점코드를 믿지 않는다 — 상점 제한 계정은 항상 자기 상점. */
    public String shopFor(HttpServletRequest request, String requested) {
        String shop = scope(request).shopCode();
        return shop != null ? shop : requested;
    }

    private static ResponseStatusException denied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, DENIED);
    }
}
