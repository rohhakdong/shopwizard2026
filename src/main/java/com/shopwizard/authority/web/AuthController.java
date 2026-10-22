package com.shopwizard.authority.web;

import com.shopwizard.authority.model.CustLoginRequest;
import com.shopwizard.authority.model.Mngr;
import com.shopwizard.authority.model.MngrLoginRequest;
import com.shopwizard.authority.model.ShopLoginRequest;
import com.shopwizard.authority.model.WarehsLoginRequest;
import com.shopwizard.authority.service.MngrService;
import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.Warehs;
import com.shopwizard.company.service.ShopService;
import com.shopwizard.company.service.WarehsService;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.AuthSession.Type;
import com.shopwizard.framework.auth.LoginAttemptLimiter;
import com.shopwizard.framework.auth.RolePermissionService;
import com.shopwizard.framework.auth.RolePermissions;
import com.shopwizard.framework.auth.ScreenRegistry;
import com.shopwizard.profile.model.Cust;
import com.shopwizard.profile.service.CustService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 로그인/로그아웃/내 정보. 로그인 상태는 서버 세션(AuthSession)에만 저장하고, 클라이언트가 보낸 쿠키
 * 값(예전의 mngr_*, cust_*, shop_*, warehs_* 쿠키)은 더 이상 신뢰하지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String LOCKED_MESSAGE = "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.";
    private static final String FAILED_MESSAGE = "아이디 또는 비밀번호가 일치하지 않습니다.";

    private final MngrService mngrService;
    private final CustService custService;
    private final ShopService shopService;
    private final WarehsService warehsService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final RolePermissionService rolePermissionService;

    // off 이면 권한 검사를 하지 않으므로 사이드바도 전부 보여준다(screens 를 내려주지 않음).
    @Value("${app.authz.mode:enforce}")
    private String authzMode;

    // ─── 관리자 로그인 ──────────────────────────────────────────────
    @PostMapping("/mngr/login")
    public ResponseEntity<Map<String, Object>> mngrLogin(
            @RequestBody MngrLoginRequest req, HttpServletRequest request) {

        return attemptLogin(Type.MNGR, req.getLoginId(), request,
                () -> mngrService.login(req.getLoginId(), req.getPasswd(), req.getChnlCode()),
                (Mngr mngr) -> {
                    Map<String, String> claims = new HashMap<>();
                    claims.put("loginId",    mngr.getLoginId());
                    claims.put("name",       mngr.getMngrName());
                    claims.put("roleUid",    mngr.getRoleUid());
                    claims.put("svcCode",    mngr.getSvcCode());
                    // 전체 권한(역할·메뉴·기능 관리 모두 가능) 계정만 서비스 경계 밖의 데이터를 본다 — DataScope 참고.
                    claims.put("svcAll",     rolePermissionService.get(mngr.getRoleUid()).full() ? "Y" : "N");
                    claims.put("chnlCode",   mngr.getChnlCode());
                    claims.put("compCode",   mngr.getCompCode());
                    claims.put("shopCode",   mngr.getShopCode());
                    claims.put("warehsCode", mngr.getWarehsCode());
                    return claims;
                },
                mngr -> { mngr.setPasswd(null); return Map.of("mngr", mngr); });
    }

    // ─── 관리자 로그아웃 ─────────────────────────────────────────────
    @PostMapping("/mngr/logout")
    public ResponseEntity<Map<String, Object>> mngrLogout(HttpServletRequest request, HttpServletResponse response) {
        return logout(Type.MNGR, "mngr_", request, response);
    }

    // ─── 관리자 세션 확인 ────────────────────────────────────────────
    @GetMapping("/mngr/me")
    public ResponseEntity<Map<String, Object>> mngrMe(HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> res =
                me(Type.MNGR, request, "loginId", "name", "roleUid", "svcCode", "chnlCode", "compCode", "shopCode", "warehsCode");
        Map<String, Object> body = res.getBody();
        if (body != null && Boolean.TRUE.equals(body.get("authenticated")) && !"off".equalsIgnoreCase(authzMode.trim())) {
            // 화면 key → "view"(조회만) | "write"(쓰기 가능). 사이드바 표시와 버튼 제어에 쓰며, 실제 차단은 RoleAccessInterceptor 가 한다.
            RolePermissions perms = rolePermissionService.get(String.valueOf(body.get("roleUid")));
            Map<String, String> screens = new HashMap<>();
            for (ScreenRegistry.Screen sc : ScreenRegistry.SCREENS) {
                if (perms.write().contains(sc.key())) screens.put(sc.key(), "write");
                else if (perms.view().contains(sc.key())) screens.put(sc.key(), "view");
            }
            body.put("screens", screens);
        }
        return res;
    }

    // ─── 고객 로그인 ─────────────────────────────────────────────────
    @PostMapping("/cust/login")
    public ResponseEntity<Map<String, Object>> custLogin(
            @RequestBody CustLoginRequest req, HttpServletRequest request) {

        return attemptLogin(Type.CUST, req.getLoginId(), request,
                () -> custService.login(req.getLoginId(), req.getPasswd(), req.getChnlCode(), req.isUseMd5()),
                (Cust cust) -> {
                    Map<String, String> claims = new HashMap<>();
                    claims.put("custId",       cust.getCustId() != null ? cust.getCustId().toString() : "0");
                    claims.put("groupCode",    cust.getCustGroupCode());
                    claims.put("name",         cust.getCustName());
                    claims.put("loginId",      cust.getLoginId());
                    claims.put("homePhoneNo",  cust.getHomePhoneNo());
                    claims.put("compCode",     cust.getCompCode());
                    claims.put("compName",     cust.getCompName());
                    claims.put("branchId",     cust.getBranchId() != null ? cust.getBranchId().toString() : "");
                    claims.put("branchName",   cust.getBranchName());
                    claims.put("deptCode",     cust.getDeptCode());
                    claims.put("deptName",     cust.getDeptName());
                    return claims;
                },
                cust -> { cust.setPasswd(null); return Map.of("cust", cust); });
    }

    // ─── 고객 로그아웃 ───────────────────────────────────────────────
    @PostMapping("/cust/logout")
    public ResponseEntity<Map<String, Object>> custLogout(HttpServletRequest request, HttpServletResponse response) {
        return logout(Type.CUST, "cust_", request, response);
    }

    // ─── 고객 세션 확인 ──────────────────────────────────────────────
    @GetMapping("/cust/me")
    public ResponseEntity<Map<String, Object>> custMe(HttpServletRequest request) {
        return me(Type.CUST, request, "custId", "groupCode", "name", "loginId", "homePhoneNo", "compCode",
                "compName", "branchId", "branchName", "deptCode", "deptName");
    }

    // ─── 상점(거래처) 계정 로그인 ─────────────────────────────────────
    // 관리자/회원과 완전히 분리된 별도 로그인 — 상점 계정이 인증되어도 관리자 화면(MNGR 로그인
    // 필요)이나 회원 전용 API(CUST 로그인 필요)에는 전혀 접근할 수 없다. WebMvcConfig에 SHOP
    // 인터셉터로 보호되는 상점 전용 API(내 정보/내 주문 조회)만 이 로그인으로 접근 가능하다.
    @PostMapping("/shop/login")
    public ResponseEntity<Map<String, Object>> shopLogin(
            @RequestBody ShopLoginRequest req, HttpServletRequest request) {

        return attemptLogin(Type.SHOP, req.getLoginId(), request,
                () -> shopService.login(req.getLoginId(), req.getPasswd()),
                (Shop shop) -> {
                    Map<String, String> claims = new HashMap<>();
                    claims.put("shopCode",   shop.getShopCode());
                    claims.put("name",       shop.getShopName());
                    claims.put("loginId",    shop.getLoginId());
                    claims.put("supplyCode", shop.getSupplyCode());
                    // 주문 조회 시 OrderProdMapper의 selectList/selectListCount가 pSvcCode 유무로
                    // 결제/상품/상점 조인 여부를 결정하므로(공용 쿼리, order-list.js도 항상 svcCode를 실어보냄)
                    // 세션에도 실어둔다 — 없으면 /order/orderprod/shop/list가 SQL 오류로 500난다.
                    claims.put("svcCode",    shop.getSvcCode());
                    return claims;
                },
                shop -> { shop.setPasswd(null); return Map.of("shop", shop); });
    }

    @PostMapping("/shop/logout")
    public ResponseEntity<Map<String, Object>> shopLogout(HttpServletRequest request, HttpServletResponse response) {
        return logout(Type.SHOP, "shop_", request, response);
    }

    @GetMapping("/shop/me")
    public ResponseEntity<Map<String, Object>> shopMe(HttpServletRequest request) {
        return me(Type.SHOP, request, "shopCode", "name", "loginId", "supplyCode", "svcCode");
    }

    // ─── 창고(거래처) 계정 로그인 ─────────────────────────────────────
    // 관리자/회원/상점과 완전히 분리된 별도 로그인 — 창고 계정이 인증되어도 다른 화면/API에는
    // 전혀 접근할 수 없다. WebMvcConfig에 WAREHS 인터셉터로 보호되는 본인 정보 조회만
    // 이 로그인으로 접근 가능하다 (현재 범위: 로그인 + 내 정보 확인까지).
    @PostMapping("/warehs/login")
    public ResponseEntity<Map<String, Object>> warehsLogin(
            @RequestBody WarehsLoginRequest req, HttpServletRequest request) {

        return attemptLogin(Type.WAREHS, req.getLoginId(), request,
                () -> warehsService.login(req.getLoginId(), req.getPasswd()),
                (Warehs warehs) -> {
                    Map<String, String> claims = new HashMap<>();
                    claims.put("warehsCode", warehs.getWarehsCode());
                    claims.put("name",       warehs.getWarehsName());
                    claims.put("loginId",    warehs.getLoginId());
                    return claims;
                },
                warehs -> { warehs.setPasswd(null); return Map.of("warehs", warehs); });
    }

    @PostMapping("/warehs/logout")
    public ResponseEntity<Map<String, Object>> warehsLogout(HttpServletRequest request, HttpServletResponse response) {
        return logout(Type.WAREHS, "warehs_", request, response);
    }

    @GetMapping("/warehs/me")
    public ResponseEntity<Map<String, Object>> warehsMe(HttpServletRequest request) {
        return me(Type.WAREHS, request, "warehsCode", "name", "loginId");
    }

    // ─── 공통 로직 ───────────────────────────────────────────────────

    /** 시도 제한 확인 → 인증 → 성공 시 세션 로그인(+응답 본문 구성), 실패 시 실패 횟수 기록. */
    private <T> ResponseEntity<Map<String, Object>> attemptLogin(
            Type type, String loginId, HttpServletRequest request,
            Supplier<T> authenticate,
            Function<T, Map<String, String>> claimsOf,
            Function<T, Map<String, Object>> bodyOf) {

        Map<String, Object> result = new HashMap<>();
        String limiterKey = LoginAttemptLimiter.key(type, loginId, request.getRemoteAddr());
        if (loginAttemptLimiter.isLocked(limiterKey)) {
            result.put("success", false);
            result.put("message", LOCKED_MESSAGE);
            return ResponseEntity.ok(result);
        }

        T principal = authenticate.get();
        if (principal == null) {
            loginAttemptLimiter.recordFailure(limiterKey);
            result.put("success", false);
            result.put("message", FAILED_MESSAGE);
            return ResponseEntity.ok(result);
        }

        loginAttemptLimiter.recordSuccess(limiterKey);
        AuthSession.login(request, type, claimsOf.apply(principal));
        result.put("success", true);
        result.putAll(bodyOf.apply(principal));
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> logout(
            Type type, String legacyCookiePrefix, HttpServletRequest request, HttpServletResponse response) {
        AuthSession.logout(request, type);
        expireLegacyCookies(legacyCookiePrefix, request, response);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> me(Type type, HttpServletRequest request, String... fields) {
        Map<String, String> claims = AuthSession.get(request, type);
        Map<String, Object> result = new HashMap<>();
        if (claims == null) {
            result.put("authenticated", false);
            return ResponseEntity.ok(result);
        }
        result.put("authenticated", true);
        for (String field : fields) {
            result.put(field, claims.get(field));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 예전 방식(클라이언트가 읽고 쓰던 mngr_*, cust_* 등 쿠키)이 브라우저에 남아있을 수 있어
     * 로그아웃 때 만료시켜 정리한다. 서버는 이 쿠키 값을 더 이상 어디에도 사용하지 않는다.
     */
    private void expireLegacyCookies(String prefix, HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie c : cookies) {
            if (c.getName().startsWith(prefix)) {
                Cookie expired = new Cookie(c.getName(), "");
                expired.setPath("/");
                expired.setMaxAge(0);
                response.addCookie(expired);
            }
        }
    }
}
