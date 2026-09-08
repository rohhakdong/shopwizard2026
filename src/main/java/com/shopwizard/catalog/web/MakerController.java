package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Maker;
import com.shopwizard.catalog.service.MakerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/maker")
@RequiredArgsConstructor
public class MakerController {
    private final MakerService makerService;

    @GetMapping("/list") public List<Maker> selectList(@RequestParam Map<String, Object> params) { return makerService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return makerService.selectCount(params); }
    @GetMapping public Maker select(@RequestParam Map<String, Object> params) { return makerService.select(params); }
    @PostMapping public int insert(@RequestBody Maker maker) { return makerService.insert(maker); }
    @PutMapping public int update(@RequestBody Maker maker) { return makerService.update(maker); }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (다른 관리 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     * (이전엔 @RequestBody라 실제로 호출하면 500이 나던 상태였다.)
     */
    @DeleteMapping("/{makerId}")
    public int delete(@PathVariable Integer makerId) {
        Map<String, Object> params = new HashMap<>();
        params.put("makerId", makerId);
        return makerService.delete(params);
    }
}
