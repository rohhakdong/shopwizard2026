package com.shopwizard.order.service;

import com.shopwizard.order.mapper.LegacyUpriceMigrationMapper;
import com.shopwizard.order.mapper.UpriceMatchMapper;
import com.shopwizard.order.model.UpriceMatchDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 이관 규칙 한 키를 저장한다 — 키 단위 {@code REQUIRES_NEW} 라 한 키가 실패해도 나머지는 그대로 들어간다. */
@Service
@RequiredArgsConstructor
public class LegacyUpriceMigrationWriter {

    private final LegacyUpriceMigrationMapper migrationMapper;
    private final UpriceMatchMapper upriceMatchMapper;

    /** @return 저장했으면 true, 그 사이 같은 키의 규칙이 생겨 건너뛰었으면 false */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean insertRule(String prodName, String itemName, List<LegacyUpriceMigrationService.Line> lines,
                              Integer sampleOrderNo, String actorName) {
        if (migrationMapper.countKey(prodName, itemName) > 0) return false;   // 덮어쓰지 않는다
        int seq = 1;
        for (LegacyUpriceMigrationService.Line l : lines) {
            UpriceMatchDetail d = new UpriceMatchDetail();
            d.setProdName(prodName);
            d.setItemName(itemName);
            d.setMatchSeq(seq++);
            d.setMatchUpriceCode(l.code());
            d.setQty(l.qty());
            d.setRemark("LEG:" + sampleOrderNo);
            d.setRegistId(LegacyUpriceMigrationService.MIGRATE_ID);
            d.setRegistName(actorName == null ? LegacyUpriceMigrationService.MIGRATE_ID : actorName);
            upriceMatchMapper.insertDetail(d);
        }
        return true;
    }
}
