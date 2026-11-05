package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.SlinkSendRequest;
import com.shopwizard.catalog.model.SlinkSendResult;
import com.shopwizard.catalog.service.SlinkProdService;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.product.model.SlinkLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 승인(게시)된 상품을 샵링커로 전송(수동). 실제 호출은 운영 샵링커에 상품이 등록·수정되므로
 * 전송은 승인 권한({@code catalog-prod-approve})이 있어야 하고, 기본 모드는 보내지 않는 시험(dry-run)이다.
 */
@RestController
@RequestMapping("/catalog/prod/slinker")
@RequiredArgsConstructor
public class SlinkProdController {
    private final SlinkProdService slinkProdService;
    private final DataScopeGuard scopeGuard;

    @PostMapping("/send")
    public SlinkSendResult send(@RequestBody SlinkSendRequest body, HttpServletRequest request) {
        return slinkProdService.send(body.getProdCodes(), request);
    }

    /** 현재 전송 모드(dry-run | live) — 화면이 확인창에 "시험/실전송"을 보여주는 데 쓴다. 계정 ID 자체는 내려주지 않는다. */
    @GetMapping("/mode")
    public Map<String, Object> mode() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", slinkProdService.mode());
        m.put("customerIdConfigured", slinkProdService.customerIdConfigured());
        m.put("svcCode", slinkProdService.svcCode());
        m.put("maxProducts", SlinkProdService.MAX_PRODUCTS);
        return m;
    }

    /** 한 상품의 최근 전송 이력(큰 요청 주소는 제외). */
    @GetMapping("/log")
    public List<SlinkLog> log(@RequestParam String prodCode, @RequestParam(defaultValue = "20") int limit,
                              HttpServletRequest request) {
        scopeGuard.requireCatProd(request, prodCode);
        return slinkProdService.logs(prodCode, limit);
    }
}
