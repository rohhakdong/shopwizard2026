package com.shopwizard.company.service;

import com.shopwizard.company.mapper.WarehsMapper;
import com.shopwizard.company.model.Warehs;
import com.shopwizard.framework.util.EncryptUtil;
import com.shopwizard.framework.util.PasswordHasher;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehsService {
    private final WarehsMapper warehsMapper;

    public List<Warehs> getList(Map<String, Object> params) { return warehsMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return warehsMapper.selectCount(params); }
    public Warehs get(Map<String, Object> params) { return warehsMapper.select(params); }
    public String getMax(String compCode) { return warehsMapper.selectMax(compCode); }

    /** 신규 등록 시에만 여기서 비밀번호를 해싱한다 — 이후 일반 정보 수정(update)은 비밀번호를
     *  건드리지 않고, 비밀번호 변경은 updatePasswd로 완전히 분리되어 있다. */
    @Transactional
    public int insert(Warehs warehs) {
        if (warehs.getPasswd() != null && !warehs.getPasswd().isEmpty()) {
            warehs.setPasswd(PasswordHasher.hash(warehs.getPasswd()));   // BCrypt (레거시 SHA-1 은 로그인 시 자동 전환)
        }
        return warehsMapper.insert(warehs);
    }

    @Transactional
    public int update(Warehs warehs) { return warehsMapper.update(warehs); }

    /** 비밀번호 변경 전용 — 프론트가 "변경 시에만 입력"으로 보낸 새 평문을 여기서 해싱한다.
     *  일반 update()가 이 값을 다시 저장하는 일이 없어야 이중 해싱을 피할 수 있다. */
    @Transactional
    public int updatePasswd(String warehsCode, String rawPassword, String changeId, String changeName) {
        Map<String, Object> params = new HashMap<>();
        params.put("warehsCode", warehsCode);
        params.put("passwd", PasswordHasher.hash(rawPassword));
        params.put("changeId", changeId);
        params.put("changeName", changeName);
        return warehsMapper.updatePasswd(params);
    }

    @Transactional
    public int delete(Warehs warehs) { return warehsMapper.delete(warehs); }

    /** 창고 계정 로그인 — 저장값이 BCrypt 면 BCrypt 로, 레거시 SHA-1 이면 상수시간 비교 후 성공 시 BCrypt 로 자동 전환한다. */
    @Transactional
    public Warehs login(String loginId, String rawPassword) {
        Warehs warehs = warehsMapper.selectByLoginId(loginId);
        if (warehs == null || warehs.getPasswd() == null) return null;
        String stored = warehs.getPasswd();
        if (!PasswordHasher.matches(rawPassword, stored, () -> EncryptUtil.encryptBySHA(rawPassword))) return null;
        if (!PasswordHasher.isBcrypt(stored)) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("warehsCode", warehs.getWarehsCode());
                params.put("passwd", PasswordHasher.hash(rawPassword));
                params.put("changeId", "SYSTEM");
                params.put("changeName", "SYSTEM");
                warehsMapper.updatePasswd(params);
            } catch (ResponseStatusException ignored) {
                // 전환 불가(예: 72바이트 초과) — 다음 로그인에도 레거시 해시로 검증된다
            }
        }
        return warehs;
    }
}
