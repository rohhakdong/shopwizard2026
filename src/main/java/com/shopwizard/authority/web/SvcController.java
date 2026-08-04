package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Svc;
import com.shopwizard.authority.service.SvcService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/svc")
@RequiredArgsConstructor
public class SvcController {

    private final SvcService svcService;

    @GetMapping("/list")
    public List<Svc> selectList(@RequestParam Map<String, Object> params) {
        return svcService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return svcService.selectCount(params);
    }

    @GetMapping
    public Svc select(@RequestParam Map<String, Object> params) {
        return svcService.select(params);
    }

    @PostMapping
    public int insert(@RequestBody Svc svc) {
        return svcService.insert(svc);
    }

    @PutMapping
    public int update(@RequestBody Svc svc) {
        return svcService.update(svc);
    }

    @DeleteMapping
    public int delete(@RequestBody Svc svc) {
        return svcService.delete(svc);
    }
}
