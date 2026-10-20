package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.MngrMapper;
import com.shopwizard.authority.model.Mngr;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

            if (BCRYPT_FORMAT.matcher(stored).matches()) {
                if (ENCODER.matches(passwd, stored)) return candidate;
            } else if (MessageDigest.isEqual(
                    stored.getBytes(StandardCharsets.UTF_8), passwd.getBytes(StandardCharsets.UTF_8))) {
                mngrMapper.updatePasswd(candidate.getMngrUid(), ENCODER.encode(passwd));
                return candidate;
            }
        }
        return null;
    }

    @Transactional
    public int insert(Mngr mngr) {
        encodePasswdIfPresent(mngr);
        return mngrMapper.insert(mngr);
    }

    @Transactional
    public int update(Mngr mngr) {
        encodePasswdIfPresent(mngr);
        return mngrMapper.update(mngr);
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
