package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipAutoOrderMapper;
import com.shopwizard.ship.model.ShipCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/** 자동발주 화면이 쓰는 조회(현황·발주서 행·발주확인 대상)와 발주서 엑셀 만들기. 상태를 바꾸지 않는다(배송 데이터 생성은 {@link ShipDataBuildService}). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipAutoOrderService {

    /** 한 번에 발주확인 대상으로 내려 주는 라인 수 상한 — 건별 커밋이라 너무 많으면 한 요청이 오래 걸린다. */
    public static final int MAX_PENDING_LINES = 3000;
    static final int MAX_PAGE = 500;
    private static final DateTimeFormatter FILE_DT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final ShipAutoOrderMapper mapper;
    private final ShipCheckService shipCheckService;

    /** 엑셀 응답: 파일명과 내용. */
    public record Download(String fileName, byte[] bytes) {}

    public Map<String, Object> summary(String shopCode) {
        String shop = requireShop(shopCode);
        Map<String, Object> out = new LinkedHashMap<>(mapper.selectSummary(shop));
        out.put("shopCode", shop);
        out.put("shopName", mapper.selectShopName(shop));
        return out;
    }

    /** 발주서 대기 행(쪽 나눔). total 은 전체 행 수. */
    public Map<String, Object> sheet(String shopCode, Integer offset, Integer limit) {
        String shop = requireShop(shopCode);
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit < 1 ? 100 : Math.min(limit, MAX_PAGE);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", mapper.selectSheetCount(shop, null));
        out.put("rows", mapper.selectSheetRows(shop, null, off, lim));
        return out;
    }

    /** 발주확인 대상 라인(주문번호·라인번호) — 화면이 이 목록 그대로 발주확인을 요청해 "받은 발주서 = 확인하는 건" 이 되게 한다. */
    public List<Map<String, Object>> pendingLines(String shopCode) {
        return mapper.selectPendingLines(requireShop(shopCode), MAX_PENDING_LINES);
    }

    /**
     * 발주서 엑셀. shipCheckNo 가 없으면 지금 발주확인 전인 행, 있으면 그 발주 목록 건(같은 확인 시각)의 행을 다시 뽑는다.
     * 다시 뽑을 때만 그 건의 출력 횟수가 올라간다(발주서를 만드는 것 자체는 상태를 바꾸지 않는다).
     */
    @Transactional(rollbackFor = Exception.class)
    public Download download(String shopCode, Integer shipCheckNo, String actorId, String actorName) {
        if (shipCheckNo == null) {
            String shop = requireShop(shopCode);
            List<Map<String, Object>> rows = mapper.selectSheetRows(shop, null, null, null);
            return new Download(fileName(mapper.selectShopName(shop), LocalDateTime.now()), ShipOrderSheetExcel.build(rows));
        }
        ShipCheck header = shipCheckService.find(shipCheckNo);
        if (header == null) throw new ResponseStatusException(NOT_FOUND, "발주 목록을 찾을 수 없습니다.");
        List<Map<String, Object>> rows = mapper.selectSheetRows(header.getShopCode(), header.getShipCheckDate(), null, null);
        ShipCheck bump = new ShipCheck();
        bump.setShipCheckNo(shipCheckNo);
        bump.setChangeId(actorId);
        bump.setChangeName(actorName);
        shipCheckService.updatePrintCount(bump);
        String name = header.getShipCheckDesc() == null || header.getShipCheckDesc().isBlank() ? "발주서_" + shipCheckNo : header.getShipCheckDesc();
        return new Download(sanitize(name) + ".xls", ShipOrderSheetExcel.build(rows));
    }

    /** 발주 목록 건의 상점(없는 번호면 404) — 컨트롤러가 데이터 범위를 검사할 때 쓴다. */
    public String shopOfCheck(Integer shipCheckNo) {
        ShipCheck header = shipCheckService.find(shipCheckNo);
        if (header == null) throw new ResponseStatusException(NOT_FOUND, "발주 목록을 찾을 수 없습니다.");
        return header.getShopCode();
    }

    static String fileName(String shopName, LocalDateTime now) {
        String shop = shopName == null || shopName.isBlank() ? "발주서" : shopName;
        return sanitize("발주서_" + shop + "_" + now.format(FILE_DT)) + ".xls";
    }

    private static String sanitize(String s) { return s.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_"); }

    private static String requireShop(String shopCode) {
        String shop = shopCode == null ? "" : shopCode.trim();
        if (shop.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "상점을 선택하세요.");
        return shop;
    }

}
