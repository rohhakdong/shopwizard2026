package com.shopwizard.profile.service;

import com.shopwizard.framework.util.EncryptUtil;
import com.shopwizard.framework.util.PasswordHasher;
import org.springframework.web.server.ResponseStatusException;
import com.shopwizard.profile.mapper.CustMapper;
import com.shopwizard.profile.model.Cust;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustService {

    private final CustMapper custMapper;

    public List<Cust> selectList(Map<String, Object> params) { return custMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custMapper.selectCount(params); }
    public List<Cust> selectListEmp(Map<String, Object> params) { return custMapper.selectListEmp(params); }
    public Cust select(Integer custId) { return custMapper.select(custId); }
    public Cust selectByLoginId(Map<String, Object> params) { return custMapper.selectByLoginId(params); }
    public Cust selectForIdpw(Map<String, Object> params) { return custMapper.selectForIdpw(params); }
    /** 신규 회원 — 비밀번호는 BCrypt 로 저장한다(비어 있거나 너무 길면 400). */
    public int insert(Cust cust) {
        cust.setPasswd(PasswordHasher.hash(cust.getPasswd()));
        return custMapper.insert(cust);
    }

    /**
     * 일반 정보 수정. 비밀번호는 여기서 바꾸지 않는다 — 요청 바디의 passwd 는 버린다(예전엔 이 경로로 들어온 값이 해시 없이
     * 그대로 저장돼, 마이페이지 수정 요청 하나로 자기 비밀번호를 평문으로 바꿀 수 있었다). 변경은 {@link #updatePasswd} 로만.
     */
    public int update(Cust cust) {
        cust.setPasswd(null);
        return custMapper.update(cust);
    }

    /** 비밀번호 재설정 — 새 평문을 BCrypt 로 저장한다. */
    public int updatePasswd(Cust cust) {
        cust.setPasswd(PasswordHasher.hash(cust.getPasswd()));
        return custMapper.updatePasswd(cust);
    }
    public int updateLastLoginTime(Cust cust) { return custMapper.updateLastLoginTime(cust); }
    public int updateInfoOrder(Map<String, Object> params) { return custMapper.updateInfoOrder(params); }
    public int updateInfoOrderExcel(Map<String, Object> params) { return custMapper.updateInfoOrderExcel(params); }
    public int updateInfoOrderShoplinker(Map<String, Object> params) { return custMapper.updateInfoOrderShoplinker(params); }
    public int updateInfoShipDirect(Map<String, Object> params) { return custMapper.updateInfoShipDirect(params); }
    public int updateInfoShipDirectPrint(Map<String, Object> params) { return custMapper.updateInfoShipDirectPrint(params); }
    public int updateInfoReturnDirect(Map<String, Object> params) { return custMapper.updateInfoReturnDirect(params); }
    public int updateInfoOrderAll(Map<String, Object> params) { return custMapper.updateInfoOrderAll(params); }
    public int updateInfoOrderExcelAll(Map<String, Object> params) { return custMapper.updateInfoOrderExcelAll(params); }
    public int updateInfoOrderShoplinkerAll(Map<String, Object> params) { return custMapper.updateInfoOrderShoplinkerAll(params); }
    public int updateInfoShipDirectAll(Map<String, Object> params) { return custMapper.updateInfoShipDirectAll(params); }
    public int updateInfoShipDirectPrintAll(Map<String, Object> params) { return custMapper.updateInfoShipDirectPrintAll(params); }
    public int updateInfoReturnDirectAll(Map<String, Object> params) { return custMapper.updateInfoReturnDirectAll(params); }
    public int delete(Integer custId) { return custMapper.delete(custId); }

    public Cust login(String loginId, String rawPassword, String chnlCode, boolean useMd5) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginId", loginId);
        params.put("memberChnlCode", chnlCode);
        Cust cust = custMapper.selectByLoginId(params);
        if (cust == null) return null;

        // 저장값이 BCrypt 면 BCrypt 로, 아니면 레거시 해시(SHA-1 / PHP MD5)와 상수시간 비교한다. 빈 비밀번호는 통과시키지 않는다.
        String stored = cust.getPasswd();
        if (!PasswordHasher.matches(rawPassword, stored, () -> useMd5
                ? EncryptUtil.encryptByMD5WithPHP(rawPassword)
                : EncryptUtil.encryptBySHA(rawPassword))) return null;

        // 레거시 해시로 로그인에 성공했으면 그 자리에서 BCrypt 로 다시 저장한다(자동 전환). 너무 긴 비밀번호 등으로 해시를
        // 못 만들면 전환만 건너뛰고 로그인은 그대로 허용한다.
        if (!PasswordHasher.isBcrypt(stored)) {
            try {
                Cust upgrade = new Cust();
                upgrade.setCustId(cust.getCustId());
                upgrade.setPasswd(PasswordHasher.hash(rawPassword));
                upgrade.setChangeName("SYSTEM");
                custMapper.updatePasswd(upgrade);
            } catch (ResponseStatusException ignored) {
                // 전환 불가(예: 72바이트 초과) — 다음 로그인에도 레거시 해시로 검증된다
            }
        }

        custMapper.updateLastLoginTime(cust);
        return cust;
    }
}
