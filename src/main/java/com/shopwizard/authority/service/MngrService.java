package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.MngrMapper;
import com.shopwizard.authority.mapper.RoleMapper;
import com.shopwizard.authority.model.Mngr;
import com.shopwizard.authority.model.Role;
import com.shopwizard.company.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MngrService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final Pattern BCRYPT_FORMAT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final MngrMapper mngrMapper;
    private final RoleMapper roleMapper;
    private final ShopMapper shopMapper;

    public List<Mngr> selectList(Map<String, Object> params) {
        return mngrMapper.selectList(params);
    }

    public int selectCount(Map<String, Object> params) {
        return mngrMapper.selectCount(params);
    }

    public Mngr select(Map<String, Object> params) {
        return mngrMapper.select(params);
    }

    public Mngr selectInfo(String mngrUid) {
        return mngrMapper.selectInfo(mngrUid);
    }

    public int selectProdApprovYn(String loginId) {
        return mngrMapper.selectProdApprovYn(loginId);
    }

    /**
     * 관리자 로그인. 저장값이 BCrypt 형식이면 BCrypt 로 검증하고, 아니면 아직 전환되지 않은 레거시
     * 평문으로 보고 상수시간 비교한다 — 평문이 일치해 로그인에 성공하면 그 자리에서 BCrypt 로 다시
     * 저장한다(로그인 시 자동 전환, 안 쓰는 계정은 평문이 남지만 서비스는 끊김 없이 점진 전환된다).
     * 빈 비밀번호는 어떤 경우에도 통과시키지 않는다.
     */
    public Mngr login(String loginId, String passwd, String chnlCode) {
        if (loginId == null || loginId.isBlank() || passwd == null || passwd.isEmpty()) return null;

        Map<String, Object> map = new HashMap<>();
        map.put("pLoginId", loginId);
        map.put("pChnlCode", chnlCode);
        for (Mngr candidate : mngrMapper.selectForLogin(map)) {
            String stored = candidate.getPasswd();
            if (stored == null || stored.isEmpty()) continue;
            boolean matched;
            boolean legacyPlain = false;
            if (BCRYPT_FORMAT.matcher(stored).matches()) {
                matched = ENCODER.matches(passwd, stored);
            } else {
                matched = MessageDigest.isEqual(
                        stored.getBytes(StandardCharsets.UTF_8), passwd.getBytes(StandardCharsets.UTF_8));
                legacyPlain = matched;
            }
            if (!matched) continue;
            // 미사용(State != 1) 계정은 비밀번호가 맞아도 로그인시키지 않는다 — 예전에는 State 를 전혀 보지 않아
            // "미사용"으로 둔 계정(개발 DB 52개)도 로그인됐다. 계정 존재 여부를 알려주지 않도록 일반 실패와 같게 취급한다.
            if (candidate.getState() == null || candidate.getState() != 1) continue;
            // 업체(협력사) 역할인데 상점이 없으면 상점 스코핑이 걸리지 않아(코드 없음 = 무제한) 모든 업체의 데이터가 보인다.
            // 등록·수정 단계에서 막지만, DB 직접 수정이나 역할 변경으로 생긴 계정도 여기서 fail-closed 로 막는다.
            if (candidate.getSupplyYn() == 1 && isBlank(candidate.getShopCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "상점이 지정되지 않은 업체 계정입니다. 관리자에게 문의하세요.");
            }
            if (legacyPlain) mngrMapper.updatePasswd(candidate.getMngrUid(), ENCODER.encode(passwd));
            return candidate;
        }
        return null;
    }

    @Transactional
    public int insert(Mngr mngr) {
        // MngrUid(PK, NOT NULL)를 화면이 보내지 않아 관리자 등록이 항상 500("Column 'MngrUid' cannot be null")이었다.
        // 클라이언트가 값을 줘도 믿지 않고 서버가 발급한다.
        mngr.setMngrUid(java.util.UUID.randomUUID().toString());
        mngr.setShopCode(blankToNull(mngr.getShopCode()));
        validateShopRule(mngr.getRoleUid(), mngr.getShopCode(), mngr.getState());
        encodePasswdIfPresent(mngr);
        return mngrMapper.insert(mngr);
    }

    @Transactional
    public int update(Mngr mngr) {
        // 요청에 안 실린 값은 DB 의 현재 값으로 판단한다 — 역할을 업체 역할로 바꾸거나 미사용 → 사용으로 켜면서 상점이 없는
        // 계정이 되는 경우도 막기 위해서다. shopCode 가 요청에 없으면(null) 상점은 그대로 유지된다(매퍼 조건과 동일).
        Mngr current = mngr.getMngrUid() == null ? null : mngrMapper.selectInfo(mngr.getMngrUid());
        if (current != null) {
            String roleUid = mngr.getRoleUid() != null ? mngr.getRoleUid() : current.getRoleUid();
            String shopCode = mngr.getShopCode() != null ? blankToNull(mngr.getShopCode()) : blankToNull(current.getShopCode());
            Integer state = mngr.getState() != null ? mngr.getState() : current.getState();
            validateShopRule(roleUid, shopCode, state);
            if (mngr.getShopCode() != null) mngr.setShopCode(shopCode == null ? "" : shopCode);   // "" = 해제(매퍼가 NULL 로 저장)
        }
        encodePasswdIfPresent(mngr);
        return mngrMapper.update(mngr);
    }

    /**
     * 업체(협력사) 역할이고 사용 중인 계정은 상점코드가 필수이고, 상점코드가 있으면 실제 존재하는 상점이어야 한다.
     * 업체 역할이 아닌 계정의 상점은 선택(가맹점 관리자처럼 상점을 가진 일반 역할이 있다), 미사용 계정은 상점 없이도 저장할 수 있다.
     */
    private void validateShopRule(String roleUid, String shopCode, Integer state) {
        Role role = roleUid == null ? null : roleMapper.selectByUid(roleUid);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 역할입니다.");
        }
        boolean active = state == null || state == 1;
        if (role.getSupplyYn() == 1 && active && isBlank(shopCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업체 역할 계정은 상점을 지정해야 합니다.");
        }
        if (!isBlank(shopCode) && shopMapper.select(shopCode) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 상점코드입니다: " + shopCode);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    @Transactional
    public int delete(String mngrUid) {
        return mngrMapper.delete(mngrUid);
    }

    /** 비밀번호가 들어온 경우에만 BCrypt 로 해시한다(비어 있으면 update SQL 이 Passwd 를 건드리지 않는다). */
    private void encodePasswdIfPresent(Mngr mngr) {
        String raw = mngr.getPasswd();
        if (raw != null && !raw.isEmpty()) {
            mngr.setPasswd(ENCODER.encode(raw));
        }
    }
}
