package com.shopwizard.product.web;

import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/prod")
public class ProdController {
    private final ProdService prodService;
    private final com.shopwizard.framework.auth.DataScopeGuard scopeGuard;   // 상점 제한 관리자는 자기 상점 상품만 수정/삭제

    @GetMapping("/list")
    public List<Prod> selectList(@RequestParam Map<String, Object> params) { return prodService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return prodService.selectCount(params); }

    @GetMapping
    public Prod select(@RequestParam Map<String, Object> params) { return prodService.select(params); }

    @GetMapping("/byshopprodcode")
    public Prod selectByShopProdCode(@RequestParam Map<String, Object> params) { return prodService.selectByShopProdCode(params); }

    @GetMapping("/shopprodcode")
    public String selectShopProdCode(@RequestParam String shopProdCode) { return prodService.selectShopProdCode(shopProdCode); }

    @DeleteMapping
    public void delete(@RequestParam String prodCode, jakarta.servlet.http.HttpServletRequest request) {
        scopeGuard.requirePrdProd(request, prodCode);   // 상점 제한 관리자는 자기 상점 상품만
        prodService.delete(prodCode);
    }

    @GetMapping("/search/list")
    public List<Prod> selectListSearch(@RequestParam Map<String, Object> params) { return prodService.selectListSearch(params); }

    @GetMapping("/search/count")
    public int selectCountSearch(@RequestParam Map<String, Object> params) { return prodService.selectCountSearch(params); }

    @GetMapping("/keyword/list")
    public List<Prod> selectListKeyword(@RequestParam Map<String, Object> params) { return prodService.selectListKeyword(params); }

    @GetMapping("/brand/list")
    public List<Prod> selectListBrand(@RequestParam Map<String, Object> params) { return prodService.selectListBrand(params); }

    @GetMapping("/supplyqty")
    public int selectSupplyQty(@RequestParam Map<String, Object> params) { return prodService.selectSupplyQty(params); }

    @GetMapping("/supplyqty/item")
    public int selectSupplyQtyItem(@RequestParam Map<String, Object> params) { return prodService.selectSupplyQtyItem(params); }

    @PutMapping("/supplyqty")
    public void updateSupplyQty(@RequestBody Map<String, Object> params, jakarta.servlet.http.HttpServletRequest request) {
        scopeGuard.requirePrdProd(request, params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode")));
        // 화면은 절대값(supplyQty)과 자기가 보던 값(expectedQty)을 보낸다. 예전에는 차감용 쿼리(SupplyQty - qty)를 불러서
        // qty 가 없는 이 요청이 재고를 NULL(무제한)로 만들었다.
        Object qty = params.get("supplyQty");
        if (!(qty instanceof Number) || ((Number) qty).intValue() < 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "재고수량은 0 이상의 숫자여야 합니다.");
        }
        if (!params.containsKey("expectedQty")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "현재 재고 값(expectedQty)이 필요합니다.");
        }
        if (!prodService.setSupplyQty(params)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "그 사이 재고가 변경되었습니다. 새로고침 후 다시 시도하세요.");
        }
    }

    @PutMapping("/saleyn")
    public void updateSaleYn(@RequestBody Map<String, Object> params, jakarta.servlet.http.HttpServletRequest request) {
        scopeGuard.requirePrdProd(request, params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode")));
        prodService.updateSaleYn(params);
    }

    @GetMapping("/naver/list")
    public List<Map<String, Object>> selectListNaver(@RequestParam Map<String, Object> params) { return prodService.selectListNaver(params); }
}
