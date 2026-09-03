package com.shopwizard.company.web;

import com.shopwizard.company.model.DeliComp;
import com.shopwizard.company.service.DeliCompService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/deli-comp")
@RequiredArgsConstructor
public class DeliCompController {
    private final DeliCompService deliCompService;

    @GetMapping
    public List<DeliComp> getList(@RequestParam Map<String, Object> params) {
        return deliCompService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return deliCompService.getCount(params);
    }

    @GetMapping("/{deliCompCode}")
    public DeliComp get(@PathVariable int deliCompCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("deliCompCode", deliCompCode);
        return deliCompService.get(params);
    }

    @PostMapping
    public int insert(@RequestBody DeliComp deliComp) {
        return deliCompService.insert(deliComp);
    }

    @PutMapping
    public int update(@RequestBody DeliComp deliComp) {
        return deliCompService.update(deliComp);
    }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (auth-mngr.js 등 기존 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     */
    @DeleteMapping("/{deliCompCode}")
    public int delete(@PathVariable int deliCompCode) {
        DeliComp deliComp = new DeliComp();
        deliComp.setDeliCompCode(deliCompCode);
        return deliCompService.delete(deliComp);
    }
}
