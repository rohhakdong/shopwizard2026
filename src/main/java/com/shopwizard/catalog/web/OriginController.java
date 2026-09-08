package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Origin;
import com.shopwizard.catalog.service.OriginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/origin")
@RequiredArgsConstructor
public class OriginController {
    private final OriginService originService;

    @GetMapping("/list") public List<Origin> selectList(@RequestParam Map<String, Object> params) { return originService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return originService.selectCount(params); }
    @GetMapping public Origin select(@RequestParam Map<String, Object> params) { return originService.select(params); }
    @PostMapping public int insert(@RequestBody Origin origin) { return originService.insert(origin); }
    @PutMapping public int update(@RequestBody Origin origin) { return originService.update(origin); }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (다른 관리 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     * (이전엔 @RequestBody라 실제로 호출하면 500이 나던 상태였다.)
     */
    @DeleteMapping("/{originId}")
    public int delete(@PathVariable Integer originId) {
        Map<String, Object> params = new HashMap<>();
        params.put("originId", originId);
        return originService.delete(params);
    }
}
