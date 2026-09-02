package com.shopwizard.authority.web;

import com.shopwizard.authority.model.CustLoginRequest;
import com.shopwizard.authority.model.Mngr;
import com.shopwizard.authority.model.MngrLoginRequest;
import com.shopwizard.authority.service.MngrService;
import com.shopwizard.profile.model.Cust;
import com.shopwizard.profile.service.CustService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final MngrService mngrService;
    private final CustService custService;

    // ─── 관리자 로그인 ──────────────────────────────────────────────
    @PostMapping("/mngr/login")
    public ResponseEntity<Map<String, Object>> mngrLogin(
            @RequestBody MngrLoginRequest req,
            HttpServletResponse response) {

        Mngr mngr = mngrService.login(req.getLoginId(), req.getPasswd(), req.getChnlCode());

        Map<String, Object> result = new HashMap<>();
        if (mngr == null) {
            result.put("success", false);
            result.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return ResponseEntity.ok(result);
        }

        mngr.setPasswd(null);
        setCookie(response, "mngr_loginId",    mngr.getLoginId()                        );
        setCookie(response, "mngr_name",       mngr.getMngrName()                       );
        setCookie(response, "mngr_roleUid",    mngr.getRoleUid()                        );
        setCookie(response, "mngr_svcCode",    mngr.getSvcCode()                        );
        setCookie(response, "mngr_chnlCode",   nvl(mngr.getChnlCode())                  );
        setCookie(response, "mngr_compCode",   nvl(mngr.getCompCode())                  );
        setCookie(response, "mngr_shopCode",   nvl(mngr.getShopCode())                  );
        setCookie(response, "mngr_warehsCode", nvl(mngr.getWarehsCode())                );

        result.put("success", true);
        result.put("mngr", mngr);
        return ResponseEntity.ok(result);
    }

    // ─── 관리자 로그아웃 ─────────────────────────────────────────────
    @PostMapping("/mngr/logout")
    public ResponseEntity<Map<String, Object>> mngrLogout(HttpServletResponse response) {
        clearCookie(response, "mngr_loginId");
        clearCookie(response, "mngr_name");
        clearCookie(response, "mngr_roleUid");
        clearCookie(response, "mngr_svcCode");
        clearCookie(response, "mngr_chnlCode");
        clearCookie(response, "mngr_compCode");
        clearCookie(response, "mngr_shopCode");
        clearCookie(response, "mngr_warehsCode");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    // ─── 관리자 세션 확인 ────────────────────────────────────────────
    @GetMapping("/mngr/me")
    public ResponseEntity<Map<String, Object>> mngrMe(HttpServletRequest request) {
        String loginId = getCookieValue(request, "mngr_loginId");
        Map<String, Object> result = new HashMap<>();
        if (loginId == null || loginId.isEmpty()) {
            result.put("authenticated", false);
            return ResponseEntity.ok(result);
        }
        result.put("authenticated", true);
        result.put("loginId",    loginId);
        result.put("name",       getCookieValue(request, "mngr_name"));
        result.put("roleUid",    getCookieValue(request, "mngr_roleUid"));
        result.put("svcCode",    getCookieValue(request, "mngr_svcCode"));
        result.put("chnlCode",   getCookieValue(request, "mngr_chnlCode"));
        result.put("compCode",   getCookieValue(request, "mngr_compCode"));
        result.put("shopCode",   getCookieValue(request, "mngr_shopCode"));
        result.put("warehsCode", getCookieValue(request, "mngr_warehsCode"));
        return ResponseEntity.ok(result);
    }

    // ─── 고객 로그인 ─────────────────────────────────────────────────
    @PostMapping("/cust/login")
    public ResponseEntity<Map<String, Object>> custLogin(
            @RequestBody CustLoginRequest req,
            HttpServletResponse response) {

        Cust cust = custService.login(req.getLoginId(), req.getPasswd(), req.getChnlCode(), req.isUseMd5());

        Map<String, Object> result = new HashMap<>();
        if (cust == null) {
            result.put("success", false);
            result.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return ResponseEntity.ok(result);
        }

        cust.setPasswd(null);
        setCookie(response, "cust_id",          nvl(cust.getCustId() != null ? cust.getCustId().toString() : "0"));
        setCookie(response, "cust_groupCode",    nvl(cust.getCustGroupCode())  );
        setCookie(response, "cust_name",         nvl(cust.getCustName())       );
        setCookie(response, "cust_loginId",      nvl(cust.getLoginId())        );
        setCookie(response, "cust_homePhoneNo",  nvl(cust.getHomePhoneNo())    );
        setCookie(response, "cust_compCode",     nvl(cust.getCompCode())       );
        setCookie(response, "cust_compName",     nvl(cust.getCompName())       );
        setCookie(response, "cust_compLogoImg",  nvl(cust.getCompLogoImg())    );
        setCookie(response, "cust_branchId",     nvl(cust.getBranchId() != null ? cust.getBranchId().toString() : ""));
        setCookie(response, "cust_branchName",   nvl(cust.getBranchName())     );
        setCookie(response, "cust_deptCode",     nvl(cust.getDeptCode())       );
        setCookie(response, "cust_deptName",     nvl(cust.getDeptName())       );

        result.put("success", true);
        result.put("cust", cust);
        return ResponseEntity.ok(result);
    }

    // ─── 고객 로그아웃 ───────────────────────────────────────────────
    @PostMapping("/cust/logout")
    public ResponseEntity<Map<String, Object>> custLogout(HttpServletResponse response) {
        clearCookie(response, "cust_id");
        clearCookie(response, "cust_groupCode");
        clearCookie(response, "cust_name");
        clearCookie(response, "cust_loginId");
        clearCookie(response, "cust_homePhoneNo");
        clearCookie(response, "cust_compCode");
        clearCookie(response, "cust_compName");
        clearCookie(response, "cust_compLogoImg");
        clearCookie(response, "cust_branchId");
        clearCookie(response, "cust_branchName");
        clearCookie(response, "cust_deptCode");
        clearCookie(response, "cust_deptName");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    // ─── 고객 세션 확인 ──────────────────────────────────────────────
    @GetMapping("/cust/me")
    public ResponseEntity<Map<String, Object>> custMe(HttpServletRequest request) {
        String custId = getCookieValue(request, "cust_id");
        Map<String, Object> result = new HashMap<>();
        if (custId == null || custId.isEmpty()) {
            result.put("authenticated", false);
            return ResponseEntity.ok(result);
        }
        result.put("authenticated", true);
        result.put("custId",       custId);
        result.put("groupCode",    getCookieValue(request, "cust_groupCode"));
        result.put("name",         getCookieValue(request, "cust_name"));
        result.put("loginId",      getCookieValue(request, "cust_loginId"));
        result.put("homePhoneNo",  getCookieValue(request, "cust_homePhoneNo"));
        result.put("compCode",     getCookieValue(request, "cust_compCode"));
        result.put("compName",     getCookieValue(request, "cust_compName"));
        result.put("branchId",     getCookieValue(request, "cust_branchId"));
        result.put("branchName",   getCookieValue(request, "cust_branchName"));
        result.put("deptCode",     getCookieValue(request, "cust_deptCode"));
        result.put("deptName",     getCookieValue(request, "cust_deptName"));
        return ResponseEntity.ok(result);
    }

    // ─── 쿠키 유틸 ───────────────────────────────────────────────────
    private void setCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value == null ? "" : value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 8); // 8시간
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
