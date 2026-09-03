package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ShopMapper;
import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.ShopPublicView;
import com.shopwizard.framework.util.EncryptUtil;
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
            shop.setPasswd(EncryptUtil.encryptBySHA(shop.getPasswd()));
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
        params.put("passwd", EncryptUtil.encryptBySHA(rawPassword));
        params.put("changeId", changeId);
        params.put("changeName", changeName);
        return shopMapper.updatePasswd(params);
    }

    @Transactional
    public int delete(Shop shop) { return shopMapper.delete(shop); }

    /** 상점 계정 로그인 — LoginId로 조회 후 SHA 해싱한 입력값과 저장된 해시를 비교한다. */
    public Shop login(String loginId, String rawPassword) {
        Shop shop = shopMapper.selectByLoginId(loginId);
        if (shop == null || shop.getPasswd() == null) return null;
        if (!EncryptUtil.encryptBySHA(rawPassword).equals(shop.getPasswd())) return null;
        return shop;
    }
}
