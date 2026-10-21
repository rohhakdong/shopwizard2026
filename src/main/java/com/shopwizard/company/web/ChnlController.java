package com.shopwizard.company.web;

import com.shopwizard.company.model.Chnl;
import com.shopwizard.company.service.ChnlService;
import com.shopwizard.framework.auth.DataScope;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final DataScopeGuard scopeGuard;

    @GetMapping
    public List<Chnl> getList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        return scoped(chnlService.getList(params), scopeGuard.scope(request));
    }

    /**
     * 채널 제한 계정은 자기 채널만(전체 필드), 상점 제한 계정은 다른 채널의 담당자·연락처·로그인 정보 없이
     * 코드/이름만 본다(주문 필터 드롭다운용). 제한 없는 계정은 그대로. 비밀번호는 누구에게도 내려주지 않는다.
     */
    private static List<Chnl> scoped(List<Chnl> list, DataScope scope) {
        list.forEach(c -> c.setPasswd(null));
        if (scope.chnlCode() != null) {
            return list.stream().filter(c -> scope.chnlCode().equals(c.getChnlCode())).toList();
        }
        if (scope.shopCode() != null) {
            return list.stream().map(c -> {
                Chnl t = new Chnl();
                t.setChnlCode(c.getChnlCode());
                t.setChnlName(c.getChnlName());
                t.setSvcCode(c.getSvcCode());
                return t;
            }).toList();
        }
        return list;
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        DataScope scope = scopeGuard.scope(request);
        if (scope.chnlCode() != null) return scoped(chnlService.getList(params), scope).size();
        return chnlService.getCount(params);
    }

    @GetMapping("/{chnlCode}")
    public Chnl get(@PathVariable String chnlCode, HttpServletRequest request) {
        DataScope scope = scopeGuard.scope(request);
        if (scope.chnlCode() != null && !scope.chnlCode().equals(chnlCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 채널의 정보는 조회할 수 없습니다.");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("chnlCode", chnlCode);
        Chnl chnl = chnlService.get(params);
        if (chnl != null) chnl.setPasswd(null);
        return chnl;
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
