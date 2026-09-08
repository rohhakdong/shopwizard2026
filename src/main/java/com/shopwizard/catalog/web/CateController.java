package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Cate;
import com.shopwizard.catalog.service.CateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/cate")
@RequiredArgsConstructor
public class CateController {
    private final CateService cateService;

    @GetMapping("/list") public List<Cate> selectList(@RequestParam Map<String, Object> params) { return cateService.selectList(params); }
    @GetMapping("/code-max") public String selectCateCodeMax(@RequestParam Map<String, Object> params) { return cateService.selectCateCodeMax(params); }
    @GetMapping public Cate select(@RequestParam Map<String, Object> params) { return cateService.select(params); }
    @GetMapping("/list-level") public List<Cate> selectListLevel(@RequestParam Map<String, Object> params) { return cateService.selectListLevel(params); }
    @GetMapping("/list-final-table") public List<Cate> selectListFinalTable(@RequestParam Map<String, Object> params) { return cateService.selectListFinalTable(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return cateService.selectCount(params); }
    @GetMapping("/count-child") public int selectCountChild(@RequestParam Map<String, Object> params) { return cateService.selectCountChild(params); }
    @PostMapping public int insert(@RequestBody Cate cate) { return cateService.insert(cate); }
    @PutMapping public int update(@RequestBody Cate cate) { return cateService.update(cate); }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (다른 관리 화면들도 대부분 이 방식), @RequestBody 대신 쿼리 파라미터로 받는다.
     * (이전엔 @RequestBody라 실제로 호출하면 500이 나던 상태였다.) 복합키(svcCode+cateCode)라
     * 경로변수 하나로는 표현이 안 돼 쿼리 파라미터 둘을 받는다.
     */
    @DeleteMapping
    public int delete(@RequestParam String svcCode, @RequestParam String cateCode) {
        Cate cate = new Cate();
        cate.setSvcCode(svcCode);
        cate.setCateCode(cateCode);
        return cateService.delete(cate);
    }
}
