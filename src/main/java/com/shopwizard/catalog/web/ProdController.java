package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdDashBoard;
import com.shopwizard.catalog.service.ProdImgUploadService;
import com.shopwizard.catalog.service.ProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController("catalogProdController")
@RequestMapping("/catalog/prod")
@RequiredArgsConstructor
public class ProdController {
    private final ProdService prodService;
    private final ProdImgUploadService prodImgUploadService;

    /**
     * 상품 대표이미지 업로드. 원본 1장을 받아 50/80/100/160/220/280px 썸네일 6장을 자동 생성하고,
     * 저장된 각 파일의 URL만 반환한다 — DB에는 이 URL 문자열들만 ImgUrl/ImgUrl50~280 컬럼에 넣으면 된다.
     * (프론트는 아직 ProdCode가 확정되지 않은 신규 등록 화면에서도 호출하므로 prodCode를 별도 파라미터로 받는다.)
     */
    @PostMapping("/img/upload")
    public Map<String, String> uploadImg(@RequestParam("file") MultipartFile file,
                                          @RequestParam String shopCode,
                                          @RequestParam String prodCode) throws IOException {
        return prodImgUploadService.upload(file, shopCode, prodCode);
    }

    @GetMapping("/list") public List<Prod> selectList(@RequestParam Map<String, Object> params) { return prodService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodService.selectCount(params); }
    @GetMapping("/{prodCode}") public Prod select(@PathVariable String prodCode) { return prodService.select(prodCode); }
    @GetMapping("/desc/{prodCode}") public String selectProdDesc(@PathVariable String prodCode) { return prodService.selectProdDesc(prodCode); }
    @GetMapping("/code/{shopProdCode}") public String selectProdCode(@PathVariable String shopProdCode) { return prodService.selectProdCode(shopProdCode); }
    @GetMapping("/dashboard") public List<ProdDashBoard> selectListDashboard(@RequestParam Map<String, Object> params) { return prodService.selectListDashboard(params); }
    @PostMapping public int insert(@RequestBody Prod prod) { return prodService.insert(prod); }
    @PutMapping public int update(@RequestBody Prod prod) { return prodService.update(prod); }
    @PutMapping("/price") public int updatePrice(@RequestBody Map<String, Object> params) { return prodService.updatePrice(params); }
    @PutMapping("/approv") public int approv(@RequestBody Map<String, Object> params) { return prodService.approv(params); }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (다른 관리 화면들도 대부분 이 방식), @RequestBody 대신 쿼리 파라미터로 받는다.
     * (이 화면에 등록/수정 기능이 없던 동안 삭제 버튼도 실제로는 한 번도 눌려본 적이 없어
     * 이번에 신규 등록 기능을 만들며 처음 눌러보고 나서야 500이 나는 걸 발견했다.)
     */
    @DeleteMapping
    public int delete(@RequestParam String prodCode) {
        Prod prod = new Prod();
        prod.setProdCode(prodCode);
        return prodService.delete(prod);
    }
    @PostMapping("/copy2shopion") public int copy2Shopion(@RequestBody Map<String, Object> params) { return prodService.copy2Shopion(params); }
    @DeleteMapping("/shopion/{prodCode}") public int delete2Shopion(@PathVariable String prodCode) { return prodService.delete2Shopion(prodCode); }
}
