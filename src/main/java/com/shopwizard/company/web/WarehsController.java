package com.shopwizard.company.web;

import com.shopwizard.company.model.Warehs;
import com.shopwizard.company.service.WarehsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/warehs")
@RequiredArgsConstructor
public class WarehsController {
    private final WarehsService warehsService;

    /** 관리자 전용 전체 필드 조회 (mngr_loginId 인증 필요 — WebMvcConfig 참고). 비밀번호(해시값이라도)는
     *  응답에 실을 이유가 없으므로 항상 비운다. */
    @GetMapping
    public List<Warehs> getList(@RequestParam Map<String, Object> params) {
        List<Warehs> list = warehsService.getList(params);
        list.forEach(w -> w.setPasswd(null));
        return list;
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return warehsService.getCount(params);
    }

    @GetMapping("/{warehsCode}")
    public Warehs get(@PathVariable String warehsCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("warehsCode", warehsCode);
        Warehs warehs = warehsService.get(params);
        if (warehs != null) warehs.setPasswd(null);
        return warehs;
    }

    /**
     * 로그인한 창고 계정 본인의 정보 조회 (warehs_code 인증 필요 — WebMvcConfig 참고).
     * warehsCode는 warehs_code 쿠키에서만 가져오므로 다른 창고 정보를 조회할 수 없다.
     */
    @GetMapping("/me")
    public Warehs selectMe(HttpServletRequest request) {
        String warehsCode = readWarehsCodeFromCookie(request);
        if (warehsCode == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("warehsCode", warehsCode);
        Warehs warehs = warehsService.get(params);
        if (warehs != null) warehs.setPasswd(null);
        return warehs;
    }

    private String readWarehsCodeFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if ("warehs_code".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                return c.getValue();
            }
        }
        return null;
    }

    /** 창고코드 = 소속회사코드 + "W" + 순번(1부터) 관례 — 신규 등록 화면에서 소속 회사를
     *  고르면 다음 순번을 자동으로 제안하기 위한 조회. */
    @GetMapping("/max")
    public String getMax(@RequestParam String compCode) {
        return warehsService.getMax(compCode);
    }

    @PostMapping
    public int insert(@RequestBody Warehs warehs) {
        return warehsService.insert(warehs);
    }

    @PutMapping
    public int update(@RequestBody Warehs warehs) {
        return warehsService.update(warehs);
    }

    /** 관리자 전용 비밀번호 재설정 (mngr_loginId 인증 필요 — WebMvcConfig 참고). 일반 수정(PUT
     *  /company/warehs)과 분리해, 프론트가 "변경 안 함"으로 보내는 기존 해시값을 다시 저장하다가
     *  통째로 재해싱해버리는 사고를 막는다. */
    @PutMapping("/passwd")
    public int updatePasswd(@RequestBody Map<String, Object> body) {
        String warehsCode = (String) body.get("warehsCode");
        String passwd     = (String) body.get("passwd");
        if (warehsCode == null || warehsCode.isEmpty() || passwd == null || passwd.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "창고코드와 비밀번호를 모두 입력하세요.");
        }
        String changeId   = (String) body.getOrDefault("changeId", "");
        String changeName = (String) body.getOrDefault("changeName", "");
        return warehsService.updatePasswd(warehsCode, passwd, changeId, changeName);
    }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (auth-mngr.js 등 기존 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     * (이전엔 @RequestBody라 실제로 호출하면 500이 나던 상태였다.)
     */
    @DeleteMapping("/{warehsCode}")
    public int delete(@PathVariable String warehsCode) {
        Warehs warehs = new Warehs();
        warehs.setWarehsCode(warehsCode);
        return warehsService.delete(warehs);
    }
}
