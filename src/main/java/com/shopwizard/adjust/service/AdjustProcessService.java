package com.shopwizard.adjust.service;

import com.shopwizard.adjust.mapper.AdjustProcessMapper;
import com.shopwizard.adjust.model.AdjustProcessRequests.BatchResult;
import com.shopwizard.adjust.model.AdjustProcessRequests.CloseRequest;
import com.shopwizard.adjust.model.AdjustProcessRequests.CreateRequest;
import com.shopwizard.adjust.model.AdjustProcessRequests.PriceRequest;
import com.shopwizard.adjust.model.AdjustTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 정산 생성·마감 오케스트레이션(공급사 매입 정산). 건별 처리는 {@link AdjustDetailWriter} 가 각자 독립 트랜잭션으로 하고,
 * 여기서는 요청 검증과 건별 결과 집계만 한다(클래스 레벨 트랜잭션 없음).
 */
@Service
@RequiredArgsConstructor
public class AdjustProcessService {

    static final int MAX_ITEMS = 500;
    static final int MAX_SHOPS = 200;

    private final AdjustProcessMapper mapper;
    private final AdjustDetailWriter writer;

    // ── 조회 ──
    public List<AdjustTarget> selectTargetList(Map<String, Object> params) { return mapper.selectTargetList(params); }
    public int selectTargetCount(Map<String, Object> params) { return mapper.selectTargetCount(params); }
    public List<Map<String, Object>> selectTargetSum(Map<String, Object> params) { return mapper.selectTargetSum(params); }
    public List<Map<String, Object>> selectOpenTargetCountByShop(Map<String, Object> params) {
        return mapper.selectOpenTargetCountByShop(params);
    }

    // ── 정산자료 생성 ──
    public BatchResult create(CreateRequest req, String changeId, String changeName) {
        BatchResult result = new BatchResult();
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) return result;
        if (req.getItems().size() > MAX_ITEMS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 번에 " + MAX_ITEMS + "건까지 처리할 수 있습니다.");
        }
        for (CreateRequest.Item it : req.getItems()) {
            String label = it.getOrderNo() + "-" + it.getOrderProdNo() + "(" + it.getKind() + ")";
            try {
                String err = writer.createOne(it.getKind(), it.getOrderNo(), it.getOrderProdNo(), it.getOrderChangeNo(),
                        changeId, changeName);
                if (err == null) {
                    result.setSuccess(result.getSuccess() + 1);
                } else {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailDetails().add(label + " : " + err);
                }
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getFailDetails().add(label + " : " + e.getMessage());
            }
        }
        return result;
    }

    // ── 마감 전 생성 취소 ──
    public BatchResult cancel(List<Integer> adjustDetailNos, String changeId, String changeName) {
        BatchResult result = new BatchResult();
        if (adjustDetailNos == null) return result;
        if (adjustDetailNos.size() > MAX_ITEMS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 번에 " + MAX_ITEMS + "건까지 처리할 수 있습니다.");
        }
        for (Integer no : new LinkedHashSet<>(adjustDetailNos)) {
            try {
                String err = writer.cancelOne(no, changeId, changeName);
                if (err == null) {
                    result.setSuccess(result.getSuccess() + 1);
                } else {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailDetails().add("정산자료 " + no + " : " + err);
                }
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getFailDetails().add("정산자료 " + no + " : " + e.getMessage());
            }
        }
        return result;
    }

    // ── 금액 수정 ──
    /** 마감 전이면 보정하고 true, 이미 마감됐거나 없으면 false(호출자가 409). */
    public boolean updatePrices(PriceRequest req, String changeId, String changeName) {
        if (req == null || req.getAdjustDetailNo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정산자료 번호가 필요합니다.");
        }
        // 반품(−) 정산자료는 금액이 음수라 부호는 제한하지 않고, 입력 누락·터무니없는 크기만 막는다.
        requireAmount(req.getSupplyPrice(), "공급가");
        requireAmount(req.getDiscntSupplyPrice(), "할인공급가");
        requireAmount(req.getBuyPrice(), "매입가");
        if (req.getVatRate() == null || req.getVatRate() < 0 || req.getVatRate() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "부가세율은 0~100 이어야 합니다.");
        }
        return writer.updatePrices(req.getAdjustDetailNo(), req.getSupplyPrice(), req.getDiscntSupplyPrice(),
                req.getBuyPrice(), req.getVatRate(), changeId, changeName);
    }

    // ── 상점 마감 ──
    public BatchResult close(CloseRequest req, String svcCode, String changeId, String changeName) {
        if (req == null || req.getShopCodes() == null || req.getShopCodes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "마감할 상점을 선택하세요.");
        }
        if (req.getShopCodes().size() > MAX_SHOPS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 번에 " + MAX_SHOPS + "개 상점까지 마감할 수 있습니다.");
        }
        LocalDate start = parseDate(req.getStartDate(), "시작일");
        LocalDate end = parseDate(req.getEndDate(), "종료일");
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일이 시작일보다 빠릅니다.");
        }
        BatchResult result = new BatchResult();
        for (String shopCode : new LinkedHashSet<>(req.getShopCodes())) {
            try {
                int adjustNo = writer.closeShop(shopCode, start.toString(), end.toString(), svcCode, changeId, changeName);
                result.setSuccess(result.getSuccess() + 1);
                result.getSuccessDetails().add(shopCode + " → 정산번호 " + adjustNo);
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getFailDetails().add(shopCode + " : " + e.getMessage());
            }
        }
        return result;
    }

    private static LocalDate parseDate(String s, String label) {
        try {
            return LocalDate.parse(s == null ? "" : s.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "을(를) yyyy-MM-dd 형식으로 입력하세요.");
        }
    }

    private static void requireAmount(Long v, String label) {
        if (v == null || Math.abs(v) >= 1_000_000_000_000L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "을(를) 올바른 숫자로 입력하세요.");
        }
    }
}
