package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Cust;
import com.shopwizard.profile.service.CustService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/cust")
public class CustController {

    private final CustService custService;

    @GetMapping("/list")
    public List<Cust> selectList(@RequestParam Map<String, Object> params) { return custService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custService.selectCount(params); }

    @GetMapping("/listEmp")
    public List<Cust> selectListEmp(@RequestParam Map<String, Object> params) { return custService.selectListEmp(params); }

    @GetMapping("/{custId}")
    public Cust select(@PathVariable Integer custId) { return custService.select(custId); }

    @GetMapping("/byLoginId")
    public Cust selectByLoginId(@RequestParam Map<String, Object> params) { return custService.selectByLoginId(params); }

    @GetMapping("/forIdpw")
    public Cust selectForIdpw(@RequestParam Map<String, Object> params) { return custService.selectForIdpw(params); }

    /** 관리자 전용 신규 회원 등록 (mngr_loginId 인증 필요 — WebMvcConfig 참고). */
    @PostMapping
    public int insert(@RequestBody Cust cust) { return custService.insert(cust); }

    /** 비회원의 자기 회원가입 (인증 불필요). shop.html doRegister()가 호출. */
    @PostMapping("/register")
    public int register(@RequestBody Cust cust) { return custService.insert(cust); }

    /** 관리자 전용 회원정보 수정 (mngr_loginId 인증 필요, custId는 요청 바디의 값을 그대로 사용). */
    @PutMapping
    public int update(@RequestBody Cust cust) { return custService.update(cust); }

    /**
     * 로그인한 회원 본인의 정보 수정 (cust_id 인증 필요 — WebMvcConfig 참고).
     * 요청 바디의 custId는 신뢰하지 않고 cust_id 쿠키 값으로 강제 덮어써서, 다른 회원의
     * 정보를 수정하는 것(IDOR)을 막는다.
     *
     * CustMapper.update는 State 등 일부 컬럼을 무조건 덮어쓰는 "전체 필드 수정" 쿼리라서,
     * 마이페이지의 "내 정보 저장"처럼 이름/연락처/이메일/주소만 보내는 부분 수정 요청을 그대로
     * 넘기면 State가 NOT NULL 컬럼인데 null로 넘어가 DB 오류가 난다. 클라이언트가 안 보낸
     * state는 기존 값을 조회해 채워 넣어 부분 수정처럼 동작하게 한다.
     */
    @PutMapping("/me")
    public int updateMe(@RequestBody Cust cust, HttpServletRequest request) {
        Integer custId = readCustIdFromCookie(request);
        if (custId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        cust.setCustId(custId);
        if (cust.getState() == null) {
            Cust existing = custService.select(custId);
            if (existing != null) cust.setState(existing.getState());
        }
        return custService.update(cust);
    }

    private Integer readCustIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if ("cust_id".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                try {
                    return Integer.parseInt(c.getValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @PutMapping("/passwd")
    public int updatePasswd(@RequestBody Cust cust) { return custService.updatePasswd(cust); }

    @PutMapping("/lastLoginTime")
    public int updateLastLoginTime(@RequestBody Cust cust) { return custService.updateLastLoginTime(cust); }

    @PutMapping("/infoOrder")
    public int updateInfoOrder(@RequestBody Map<String, Object> params) { return custService.updateInfoOrder(params); }

    @PutMapping("/infoOrderExcel")
    public int updateInfoOrderExcel(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderExcel(params); }

    @PutMapping("/infoOrderShoplinker")
    public int updateInfoOrderShoplinker(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderShoplinker(params); }

    @PutMapping("/infoShipDirect")
    public int updateInfoShipDirect(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirect(params); }

    @PutMapping("/infoShipDirectPrint")
    public int updateInfoShipDirectPrint(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirectPrint(params); }

    @PutMapping("/infoReturnDirect")
    public int updateInfoReturnDirect(@RequestBody Map<String, Object> params) { return custService.updateInfoReturnDirect(params); }

    @PutMapping("/infoOrderAll")
    public int updateInfoOrderAll(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderAll(params); }

    @PutMapping("/infoOrderExcelAll")
    public int updateInfoOrderExcelAll(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderExcelAll(params); }

    @PutMapping("/infoOrderShoplinkerAll")
    public int updateInfoOrderShoplinkerAll(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderShoplinkerAll(params); }

    @PutMapping("/infoShipDirectAll")
    public int updateInfoShipDirectAll(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirectAll(params); }

    @PutMapping("/infoShipDirectPrintAll")
    public int updateInfoShipDirectPrintAll(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirectPrintAll(params); }

    @PutMapping("/infoReturnDirectAll")
    public int updateInfoReturnDirectAll(@RequestBody Map<String, Object> params) { return custService.updateInfoReturnDirectAll(params); }

    @DeleteMapping("/{custId}")
    public int delete(@PathVariable Integer custId) { return custService.delete(custId); }
}
