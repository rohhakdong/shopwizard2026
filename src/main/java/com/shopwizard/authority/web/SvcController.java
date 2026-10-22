package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Svc;
import com.shopwizard.authority.service.SvcService;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/svc")
@RequiredArgsConstructor
public class SvcController {

    private final SvcService svcService;

    private final DataScopeGuard scopeGuard;

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
    public int insert(@RequestBody Svc svc, HttpServletRequest request) {
        scopeGuard.requireAllServices(request);   // 서비스 마스터는 전체 권한 계정만
        return svcService.insert(svc);
    }

    @PutMapping
    public int update(@RequestBody Svc svc, HttpServletRequest request) {
        scopeGuard.requireAllServices(request);
        return svcService.update(svc);
    }

    @DeleteMapping
    public int delete(@RequestBody Svc svc, HttpServletRequest request) {
        scopeGuard.requireAllServices(request);
        return svcService.delete(svc);
    }
}
