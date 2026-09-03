package com.shopwizard.company.web;

import com.shopwizard.company.model.Chnl;
import com.shopwizard.company.service.ChnlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("companyChnlController")
@RequestMapping("/company/chnl")
@RequiredArgsConstructor
public class ChnlController {
    private final ChnlService chnlService;

    @GetMapping
    public List<Chnl> getList(@RequestParam Map<String, Object> params) {
        return chnlService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return chnlService.getCount(params);
    }

    @GetMapping("/{chnlCode}")
    public Chnl get(@PathVariable String chnlCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("chnlCode", chnlCode);
        return chnlService.get(params);
    }

    @GetMapping("/max")
    public String getMax(@RequestParam String saleCompCode) {
        return chnlService.getMax(saleCompCode);
    }

    @PostMapping
    public int insert(@RequestBody Chnl chnl) {
        return chnlService.insert(chnl);
    }

    @PutMapping
    public int update(@RequestBody Chnl chnl) {
        return chnlService.update(chnl);
    }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (auth-mngr.js 등 기존 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     */
    @DeleteMapping("/{chnlCode}")
    public int delete(@PathVariable String chnlCode) {
        Chnl chnl = new Chnl();
        chnl.setChnlCode(chnlCode);
        return chnlService.delete(chnl);
    }
}
