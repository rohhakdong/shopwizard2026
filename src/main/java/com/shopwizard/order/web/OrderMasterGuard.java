package com.shopwizard.order.web;

import com.shopwizard.framework.auth.DataScope;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 발주 마스터(발주상품코드·발주단가표)의 상점 단위 데이터 범위 검사. 행의 상점코드가 비어 있으면 "공통" 행이다
 * (발주단가매칭 모달에서 새 발주단가코드를 만들면 상점 없이 저장된다) — 서비스 제한 계정은 공통 행을 다룰 수 있지만
 * 상점·채널 제한 계정은 다룰 수 없다.
 */
@Component
@RequiredArgsConstructor
public class OrderMasterGuard {
    private final DataScopeGuard scopeGuard;

    /** 이 상점의 행을 읽고/쓸 수 있는 계정인가. 아니면 403. */
    public void requireShop(HttpServletRequest request, String shopCode) {
        DataScope scope = scopeGuard.scope(request);
        boolean common = shopCode == null || shopCode.isBlank();
        if (common) {
            if (scope.shopCode() != null || scope.chnlCode() != null) throw denied();
            return;
        }
        if (scope.chnlCode() != null) throw denied();   // 채널 제한 계정은 상점 마스터를 다루지 않는다
        if (scope.shopCode() != null && !scope.shopCode().equals(shopCode)) throw denied();
        scopeGuard.requireShopSvc(request, shopCode);
    }

    private static ResponseStatusException denied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 업체(채널)의 데이터는 처리할 수 없습니다.");
    }
}
