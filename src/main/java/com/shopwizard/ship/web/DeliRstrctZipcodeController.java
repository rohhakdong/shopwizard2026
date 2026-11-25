package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.ship.model.DeliRstrctZipcode;
import com.shopwizard.ship.service.DeliRstrctZipcodeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 배송제한 우편번호 화면(ship-rstrct-zipcode). 전 상점이 함께 쓰는 마스터라 조회는 화면 권한만, <b>쓰기(등록·삭제)는 상점·채널·서비스 제한이 없는 계정만</b> 한다.
 * 일괄 등록은 붙여넣은 텍스트를 서버가 읽는다(미리보기는 저장하지 않고 인식 결과만 돌려준다).
 */
@RestController
@RequestMapping("/ship/rstrct-zipcode")
@RequiredArgsConstructor
public class DeliRstrctZipcodeController {

    private final DeliRstrctZipcodeService service;
    private final DataScopeGuard scopeGuard;

    @GetMapping("/sects")
    public List<Map<String, Object>> sects() { return service.sects(); }

    @GetMapping("/list")
    public List<DeliRstrctZipcode> list(@RequestParam Map<String, Object> params) { return service.selectList(params); }

    @GetMapping("/count")
    public int count(@RequestParam Map<String, Object> params) { return service.selectCount(params); }

    @PostMapping("/preview")
    public DeliRstrctZipcodeService.Parsed preview(@RequestBody Map<String, String> body) {
        return service.preview(body == null ? null : body.get("zipcodes"));
    }

    @PostMapping("/bulk")
    public DeliRstrctZipcodeService.BulkResult bulk(@RequestBody Map<String, String> body, HttpServletRequest request) {
        requireGlobal(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.bulkAdd(body == null ? null : body.get("sect"), body == null ? null : body.get("zipcodes"),
                actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    /** 우편번호 하나 삭제(zipcode 가 있으면) 또는 구분 전체 삭제(zipcode 가 없으면). */
    @DeleteMapping
    public int delete(@RequestParam("sect") String sect, @RequestParam(value = "zipcode", required = false) String zipcode, HttpServletRequest request) {
        requireGlobal(request);
        return zipcode == null || zipcode.isBlank() ? service.removeSect(sect) : service.remove(sect, zipcode);
    }

    /** 전 서비스가 함께 쓰는 마스터라 상점·채널 제한 계정뿐 아니라 서비스 제한 계정도 쓰기를 못 한다. */
    private void requireGlobal(HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        scopeGuard.requireAllServices(request);
    }
}
