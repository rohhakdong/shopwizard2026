package com.shopwizard.order.web;

import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.model.ProdCodeMatch;
import com.shopwizard.order.service.ProdCodeMatchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 상품매칭결과 화면(order-prod-match-result). 규칙에는 상점 컬럼이 없어 목록의 상점/서비스 제한은 규칙이 가리키는 판매 상품의
 * 상점으로 걸리고(DataScopeFilter 가 GET 파라미터로 고정), 단건·쓰기는 그 상품의 소유({@link DataScopeGuard#requirePrdProd})로 검사한다.
 */
@RestController
@RequestMapping("/order/prod-code-match")
@RequiredArgsConstructor
public class ProdCodeMatchController {
    private final ProdCodeMatchService service;
    private final DataScopeGuard scopeGuard;

    @GetMapping("/list")
    public List<ProdCodeMatch> selectList(@RequestParam Map<String, Object> params) { return service.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return service.selectCount(params); }

    @GetMapping
    public ProdCodeMatch select(@RequestParam("prodName") String prodName,
                                @RequestParam(value = "prodOption", defaultValue = "") String prodOption, HttpServletRequest request) {
        ProdCodeMatch row = service.find(prodName, prodOption);
        if (row != null) scopeGuard.requirePrdProd(request, row.getProdCode());
        return row;
    }

    @PostMapping
    public ProdCodeMatch create(@RequestBody ProdCodeMatch body, HttpServletRequest request) {
        scopeGuard.requirePrdProd(request, trim(body.getProdCode()));
        return service.create(body);
    }

    @PutMapping
    public ProdCodeMatch modify(@RequestBody ProdCodeMatch body, HttpServletRequest request) {
        ProdCodeMatch existing = service.find(body.getProdName(), body.getProdOption());
        if (existing != null) scopeGuard.requirePrdProd(request, existing.getProdCode());
        scopeGuard.requirePrdProd(request, trim(body.getProdCode()));   // 다른 상품으로 바꾸는 경우
        return service.modify(body);
    }

    @DeleteMapping
    public void remove(@RequestParam("prodName") String prodName,
                       @RequestParam(value = "prodOption", defaultValue = "") String prodOption, HttpServletRequest request) {
        ProdCodeMatch existing = service.find(prodName, prodOption);
        if (existing != null) scopeGuard.requirePrdProd(request, existing.getProdCode());
        service.remove(prodName, prodOption);
    }

    private static String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
