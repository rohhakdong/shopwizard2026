package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Mngr;
import com.shopwizard.authority.service.MngrService;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/mngr")
@RequiredArgsConstructor
public class MngrController {

    private final MngrService mngrService;

    private final DataScopeGuard scopeGuard;

    @GetMapping("/list")
    public List<Mngr> selectList(@RequestParam Map<String, Object> params) {
        return mngrService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return mngrService.selectCount(params);
    }

    @GetMapping
    public Mngr select(@RequestParam Map<String, Object> params) {
        return mngrService.select(params);
    }

    @GetMapping("/info/{mngrUid}")
    public Mngr selectInfo(@PathVariable String mngrUid) {
        return mngrService.selectInfo(mngrUid);
    }

    @GetMapping("/prod-approv-yn/{loginId}")
    public int selectProdApprovYn(@PathVariable String loginId) {
        return mngrService.selectProdApprovYn(loginId);
    }

    @PostMapping
    public int insert(@RequestBody Mngr mngr, HttpServletRequest request) {
        scopeGuard.requireRoleSvc(request, mngr.getRoleUid());   // 다른 서비스의 역할(예: 전체 권한 역할)로 계정을 만들 수 없다
        return mngrService.insert(mngr);
    }

    @PutMapping
    public int update(@RequestBody Mngr mngr, HttpServletRequest request) {
        scopeGuard.requireMngrSvc(request, mngr.getMngrUid());
        if (mngr.getRoleUid() != null) scopeGuard.requireRoleSvc(request, mngr.getRoleUid());
        return mngrService.update(mngr);
    }

    @DeleteMapping("/{mngrUid}")
    public int delete(@PathVariable String mngrUid, HttpServletRequest request) {
        scopeGuard.requireMngrSvc(request, mngrUid);
        return mngrService.delete(mngrUid);
    }
}
