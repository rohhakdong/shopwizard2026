package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ShopMapper;
import com.shopwizard.company.model.BusinessInfoView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** shop.html 하단 사업자 정보. 자사몰 채널({@code shop.chnl-code})의 회사 마스터에서 읽어 표시용으로 다듬는다. */
@Service
public class BusinessInfoService {
    private final ShopMapper shopMapper;
    private final String shopChnlCode;

    public BusinessInfoService(ShopMapper shopMapper, @Value("${shop.chnl-code}") String shopChnlCode) {
        this.shopMapper = shopMapper;
        this.shopChnlCode = shopChnlCode;
    }

    /** 회사 정보를 못 찾으면 빈 뷰 — 화면은 값이 없으면 푸터를 그리지 않는다. */
    public BusinessInfoView get() {
        BusinessInfoView v = shopMapper.selectBusinessInfo(shopChnlCode);
        if (v == null) return new BusinessInfoView();
        v.setBizNo(formatBizNo(v.getBizNo()));
        v.setCorpNo(formatCorpNo(v.getCorpNo()));
        return v;
    }

    /** 1208674775 → 120-86-74775. 형식이 다르면(법인 아닌 코드 등) 그대로 둔다. */
    static String formatBizNo(String s) {
        if (s == null) return null;
        String d = s.trim();
        return d.matches("\\d{10}") ? d.substring(0, 3) + "-" + d.substring(3, 5) + "-" + d.substring(5) : d;
    }

    /** 1101113114230 → 110111-3114230. */
    static String formatCorpNo(String s) {
        if (s == null) return null;
        String d = s.trim();
        return d.matches("\\d{13}") ? d.substring(0, 6) + "-" + d.substring(6) : d;
    }
}
