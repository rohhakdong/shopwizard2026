package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.ProdItem;
import com.shopwizard.catalog.service.ProdItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("catalogProdItemController")
@RequestMapping("/catalog/prod-item")
@RequiredArgsConstructor
public class ProdItemController {
    private final ProdItemService prodItemService;

    @GetMapping("/list") public List<ProdItem> selectList(@RequestParam Map<String, Object> params) { return prodItemService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodItemService.selectCount(params); }
    @GetMapping public ProdItem select(@RequestParam Map<String, Object> params) { return prodItemService.select(params); }
    @GetMapping("/item-code-by-attr") public String selectItemCodeByAttrVal(@RequestParam Map<String, Object> params) { return prodItemService.selectItemCodeByAttrVal(params); }
    @PostMapping public int insert(@RequestBody ProdItem prodItem) { return prodItemService.insert(prodItem); }
    @PutMapping public int update(@RequestBody ProdItem prodItem) { return prodItemService.update(prodItem); }
    @PutMapping("/itemlist") public int updateItemlist(@RequestBody Map<String, Object> params) { return prodItemService.updateItemlist(params); }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (이 프로젝트의 다른 관리 화면들과 동일한 패턴), @RequestBody 대신 쿼리 파라미터로 받는다.
     * PK가 (ProdCode, ItemCode) 복합키라 @PathVariable 한 칸으로는 못 받고 @RequestParam으로 받는다.
     */
    @DeleteMapping
    public int delete(@RequestParam String prodCode, @RequestParam Integer itemCode) {
        ProdItem prodItem = new ProdItem();
        prodItem.setProdCode(prodCode);
        prodItem.setItemCode(itemCode);
        return prodItemService.delete(prodItem);
    }
    @PutMapping("/disable/{prodCode}") public int disable(@PathVariable String prodCode) { return prodItemService.disable(prodCode); }
    @PutMapping("/enable") public int enable(@RequestBody Map<String, Object> params) { return prodItemService.enable(params); }

    // 서비스(ProdItemService.copy2Shopion/delete2Shopion)는 이미 있었지만 REST로 호출할 방법이
    // 없어서 catalog.ProdController.copy2Shopion(상품 자체만 복사)과 짝을 이루지 못하고 있었다 —
    // 상품 승인 시 옵션까지 함께 shopion으로 복사되도록 여기서 엔드포인트로 노출한다.
    @PostMapping("/copy2shopion") public int copy2Shopion(@RequestBody Map<String, Object> params) { return prodItemService.copy2Shopion(params); }
    @DeleteMapping("/shopion/{prodCode}") public int delete2Shopion(@PathVariable String prodCode) { return prodItemService.delete2Shopion(prodCode); }
}
