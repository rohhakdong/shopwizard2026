package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipCheckMapper;
import com.shopwizard.ship.model.ShipCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipCheckService {
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ShipCheckMapper shipCheckMapper;
    public List<ShipCheck> selectList(Map<String, Object> params) { return shipCheckMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return shipCheckMapper.selectCount(params); }
    public ShipCheck select(Map<String, Object> params) { return shipCheckMapper.select(params); }
    public int insert(ShipCheck shipCheck) { return shipCheckMapper.insert(shipCheck); }
    public int updatePrintCount(ShipCheck shipCheck) { return shipCheckMapper.updatePrintCount(shipCheck); }
    public int delete(ShipCheck shipCheck) { return shipCheckMapper.delete(shipCheck); }

    /** 발주 목록 한 건을 번호로 읽는다(없으면 null). */
    @Transactional(readOnly = true)
    public ShipCheck find(Integer shipCheckNo) {
        if (shipCheckNo == null) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("shipCheckNo", shipCheckNo);
        return shipCheckMapper.select(key);
    }

    /**
     * 한 번의 발주확인(같은 shipCheckDate)에서 확인된 주문을 상점별로 모아 발주 목록(tShpShipCheck) 헤더를 만든다.
     * 라인 처리는 건별로 이미 커밋돼 있으므로 헤더는 별도 트랜잭션이고, 실패해도 라인 처리를 되돌리지 않는다(호출자가 삼킨다).
     * 헤더와 발주서·출고 행은 ShipCheckDate 가 같다는 것으로 이어진다(별도 키 없음 — 레거시와 같은 구조).
     *
     * @return 만든 헤더 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int recordBatch(String shipCheckDate, String registId, String registName) {
        Map<String, Object> key = new HashMap<>();
        key.put("shipCheckDate", shipCheckDate);
        int created = 0;
        for (Map<String, Object> g : shipCheckMapper.selectBatchSummary(key)) {
            ShipCheck h = new ShipCheck();
            h.setShopCode(String.valueOf(g.get("shopCode")));
            h.setShipCheckDesc(describe(g.get("shopName") == null ? h.getShopCode() : String.valueOf(g.get("shopName")), shipCheckDate));
            h.setShipCheckDate(shipCheckDate);
            h.setOrderCount(((Number) g.get("orderCount")).intValue());
            h.setPrintCount(0);
            h.setState(1);
            h.setRegistId(registId);
            h.setRegistName(registName);
            h.setRegistDate(shipCheckDate);
            h.setChangeId(registId);
            h.setChangeName(registName);
            h.setChangeDate(shipCheckDate);
            created += shipCheckMapper.insert(h);
        }
        return created;
    }

    /** 레거시 발주서 이름: 발주_{상점명}_{yyyy}년{MM}월{dd}일{오전|오후}{HH}  (예: 발주_두리두리_2021년05월21일오후17). */
    static String describe(String shopName, String shipCheckDate) {
        LocalDateTime t = LocalDateTime.parse(shipCheckDate, DT);
        String desc = "발주_" + shopName + "_" + String.format("%04d년%02d월%02d일", t.getYear(), t.getMonthValue(), t.getDayOfMonth())
                + (t.getHour() < 12 ? "오전" : "오후") + String.format("%02d", t.getHour());
        return desc.length() > 100 ? desc.substring(0, 100) : desc;
    }
}
