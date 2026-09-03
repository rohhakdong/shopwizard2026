package com.shopwizard.company.web;

import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.ShopPublicView;
import com.shopwizard.company.service.ShopService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/shop")
@RequiredArgsConstructor
public class ShopController {
    private final ShopService shopService;

    /**
     * 비회원도 보는 shop.html "전체 쇼핑몰" 드롭다운용 (인증 불필요 — WebMvcConfig 참고).
     * shopCode/shopName만 반환한다. 관리자용 {@link #getList}는 loginId/passwd 등
     * 민감 정보를 포함하므로 절대 이 경로를 관리자 화면에서 재사용하지 말 것.
     */
    @GetMapping("/public")
    public List<ShopPublicView> getPublicList() {
        return shopService.getPublicList();
    }

    /** 관리자 전용 전체 필드 조회 (mngr_loginId 인증 필요 — WebMvcConfig 참고). 비밀번호(해시값이라도)는
     *  응답에 실을 이유가 없으므로 항상 비운다. */
    @GetMapping
    public List<Shop> getList(@RequestParam Map<String, Object> params) {
        List<Shop> list = shopService.getList(params);
        list.forEach(s -> s.setPasswd(null));
        return list;
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return shopService.getCount(params);
    }

    @GetMapping("/{shopCode}")
    public Shop get(@PathVariable String shopCode) {
        Shop shop = shopService.get(shopCode);
        if (shop != null) shop.setPasswd(null);
        return shop;
    }

    /**
     * 로그인한 상점 계정 본인의 정보 조회 (shop_code 인증 필요 — WebMvcConfig 참고).
     * shopCode는 shop_code 쿠키에서만 가져오므로 다른 상점 정보를 조회할 수 없다.
     */
    @GetMapping("/me")
    public Shop selectMe(HttpServletRequest request) {
        String shopCode = readShopCodeFromCookie(request);
        if (shopCode == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Shop shop = shopService.get(shopCode);
        if (shop != null) shop.setPasswd(null);
        return shop;
    }

    private String readShopCodeFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if ("shop_code".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                return c.getValue();
            }
        }
        return null;
    }

    /** 관리자 전용 비밀번호 재설정 (mngr_loginId 인증 필요 — WebMvcConfig 참고). 일반 수정(PUT
     *  /company/shop)과 분리해, 프론트가 "변경 안 함"으로 보내는 기존 해시값을 다시 저장하다가
     *  통째로 재해싱해버리는 사고를 막는다. */
    @PutMapping("/passwd")
    public int updatePasswd(@RequestBody Map<String, Object> body) {
        String shopCode = (String) body.get("shopCode");
        String passwd   = (String) body.get("passwd");
        if (shopCode == null || shopCode.isEmpty() || passwd == null || passwd.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상점코드와 비밀번호를 모두 입력하세요.");
        }
        String changeId   = (String) body.getOrDefault("changeId", "");
        String changeName = (String) body.getOrDefault("changeName", "");
        return shopService.updatePasswd(shopCode, passwd, changeId, changeName);
    }

    @GetMapping("/max")
    public String getMax(@RequestParam String supplyCode) {
        return shopService.getMax(supplyCode);
    }

    @PostMapping
    public int insert(@RequestBody Shop shop) {
        return shopService.insert(shop);
    }

    @PutMapping
    public int update(@RequestBody Shop shop) {
        return shopService.update(shop);
    }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (auth-mngr.js 등 기존 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     * (이전엔 @RequestBody라 실제로 호출하면 500이 나던 상태였다 — company-shop.js의 삭제 버튼이
     * 한 번도 정상 동작한 적이 없었다는 뜻.)
     */
    @DeleteMapping("/{shopCode}")
    public int delete(@PathVariable String shopCode) {
        Shop shop = new Shop();
        shop.setShopCode(shopCode);
        return shopService.delete(shop);
    }
}
