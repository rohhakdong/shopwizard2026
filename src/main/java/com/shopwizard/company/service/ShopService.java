package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ShopMapper;
import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.ShopPublicView;
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
public class ShopService {
    private final ShopMapper shopMapper;

    public List<Shop> getList(Map<String, Object> params) { return shopMapper.selectList(params); }
    public List<ShopPublicView> getPublicList() { return shopMapper.selectPublicList(); }
    public int getCount(Map<String, Object> params) { return shopMapper.selectCount(params); }
    public String getMax(String supplyCode) { return shopMapper.selectMax(supplyCode); }
    public Shop get(String shopCode) { return shopMapper.select(shopCode); }
    public Shop getByName(String shopName) { return shopMapper.selectByName(shopName); }
    public int getLoginIdCount(Map<String, Object> params) { return shopMapper.selectLoginIdCount(params); }

    /** 신규 등록 시에만 여기서 비밀번호를 해싱한다 — 이후 일반 정보 수정(update)은 비밀번호를
     *  건드리지 않고, 비밀번호 변경은 updatePasswd로 완전히 분리되어 있다. */
    @Transactional
    public int insert(Shop shop) {
        if (shop.getPasswd() != null && !shop.getPasswd().isEmpty()) {
            shop.setPasswd(PasswordHasher.hash(shop.getPasswd()));   // BCrypt (레거시 SHA-1 은 로그인 시 자동 전환)
        }
        return shopMapper.insert(shop);
    }

    @Transactional
    public int update(Shop shop) { return shopMapper.update(shop); }

    /** 비밀번호 변경 전용 — 프론트가 "변경 시에만 입력"으로 보낸 새 평문을 여기서 해싱한다.
     *  일반 update()가 이 값을 다시 저장하는 일이 없어야 이중 해싱을 피할 수 있다. */
    @Transactional
    public int updatePasswd(String shopCode, String rawPassword, String changeId, String changeName) {
        Map<String, Object> params = new HashMap<>();
        params.put("shopCode", shopCode);
        params.put("passwd", PasswordHasher.hash(rawPassword));
        params.put("changeId", changeId);
        params.put("changeName", changeName);
        return shopMapper.updatePasswd(params);
    }

    @Transactional
    public int delete(Shop shop) { return shopMapper.delete(shop); }

    /** 상점 계정 로그인 — 저장값이 BCrypt 면 BCrypt 로, 레거시 SHA-1 이면 상수시간 비교 후 성공 시 BCrypt 로 자동 전환한다. */
    @Transactional
    public Shop login(String loginId, String rawPassword) {
        Shop shop = shopMapper.selectByLoginId(loginId);
        if (shop == null || shop.getPasswd() == null) return null;
        String stored = shop.getPasswd();
        if (!PasswordHasher.matches(rawPassword, stored, () -> EncryptUtil.encryptBySHA(rawPassword))) return null;
        if (!PasswordHasher.isBcrypt(stored)) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("shopCode", shop.getShopCode());
                params.put("passwd", PasswordHasher.hash(rawPassword));
                params.put("changeId", "SYSTEM");
                params.put("changeName", "SYSTEM");
                shopMapper.updatePasswd(params);
            } catch (ResponseStatusException ignored) {
                // 전환 불가(예: 72바이트 초과) — 다음 로그인에도 레거시 해시로 검증된다
            }
        }
        return shop;
    }
}
