package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.service.OrderLineLock;
import com.shopwizard.order.service.StockLedgerService;
import com.shopwizard.ship.mapper.ReturnDirectMapper;
import com.shopwizard.ship.model.ReturnDirect;
import com.shopwizard.ship.model.ReturnRequestBatchRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnDirectService {
    private final ReturnDirectMapper returnDirectMapper;
    private final OrderProdMapper orderProdMapper;
    private final OrderMapper orderMapper;
    private final ReturnRequestService returnRequestService;
    private final StockLedgerService stockLedgerService;
    private final OrderLineLock lineLock;   // 상태 확인~갱신 사이에 다른 요청이 끼어들지 못하게 행 잠금

    public List<ReturnDirect> selectList(Map<String, Object> params) { return returnDirectMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return returnDirectMapper.selectCount(params); }
    public ReturnDirect select(Map<String, Object> params) { return returnDirectMapper.select(params); }
    public int insert(ReturnDirect returnDirect) { return returnDirectMapper.insert(returnDirect); }
    public int insertReturnDirect(Map<String, Object> params) { return returnDirectMapper.insertReturnDirect(params); }

    /**
     * 반품확정 화면(ship-return.js) — 반품요청 건의 완료처리. 기존엔 tShpReturnDirect만 갱신하고
     * 실제 주문(tOrdOrderProd/tOrdOrder)은 그대로 뒀는데(레거시 runUpdateReturnDirect 대비 캐스케이드
     * 누락), 여기서 라인 상태를 반품완료로 동기화하고 그 주문의 모든 라인이 반품완료에 도달하면
     * 헤더도 승격한다(countOrderStateDiffer).
     */
    public int update(Map<String, Object> params) {
        // 헤더 → 라인 순으로 잠그고 라인이 반품요청 상태인지 확인한다. 0 = 처리 안 됨(이미 처리됐거나 상태가 바뀜).
        OrderProd line = lineLock.lockHeaderAndLine(toInt(params.get("orderNo")), toInt(params.get("orderProdNo")));
        if (line == null || !"반품요청".equals(line.getOrderState())) return 0;
        int updated = returnDirectMapper.update(params);
        if (updated > 0) {
            Map<String, Object> prodUpd = new HashMap<>();
            prodUpd.put("orderNo", params.get("orderNo"));
            prodUpd.put("orderProdNo", params.get("orderProdNo"));
            prodUpd.put("orderState", "반품완료");
            prodUpd.put("changeId", params.get("changeId"));
            prodUpd.put("changeName", params.get("changeName"));
            orderProdMapper.updateOrderState(prodUpd);

            Map<String, Object> diff = new HashMap<>();
            diff.put("orderNo", params.get("orderNo"));
            diff.put("orderState", "반품완료");
            if (orderProdMapper.countOrderStateDiffer(diff) == 0) {
                Map<String, Object> headerUpd = new HashMap<>();
                headerUpd.put("orderNo", params.get("orderNo"));
                headerUpd.put("orderState", "반품완료");
                headerUpd.put("changeId", params.get("changeId"));
                headerUpd.put("changeName", params.get("changeName"));
                orderMapper.updateOrderState(headerUpd);
            }

            // 반품 물건이 돌아왔다 — 이 시스템이 차감한 이력이 있는 라인만 재고를 되돌린다(재입고).
            stockLedgerService.restore(toInt(params.get("orderNo")), toInt(params.get("orderProdNo")),
                    params.get("changeId") == null ? null : String.valueOf(params.get("changeId")));
        }
        return updated;
    }

    private static Integer toInt(Object o) {
        return o instanceof Number ? Integer.valueOf(((Number) o).intValue()) : null;
    }

    /**
     * 반품요청 화면(ship-return-request.js) — 선택한 배송완료 주문라인들을 건별로 반품요청 전환한다.
     * 건별로 {@link ReturnRequestService#requestOne}이 REQUIRES_NEW로 독립 커밋되므로,
     * 한 건이 실패해도 나머지 건 처리에는 영향이 없다.
     */
    public ShipDirectIssueResult requestBatch(ReturnRequestBatchRequest req) {
        ShipDirectIssueResult result = new ShipDirectIssueResult();
        if (req.getLines() == null) return result;
        for (ReturnRequestBatchRequest.Line line : req.getLines()) {
            try {
                String err = returnRequestService.requestOne(
                        line.getOrderNo(), line.getOrderProdNo(), req.getReturnReason(), req.getReturnReasonDetail(),
                        req.getRegistId(), req.getRegistName());
                if (err != null) {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailDetails().add(line.getOrderNo() + "-" + line.getOrderProdNo() + " : " + err);
                } else {
                    result.setSuccess(result.getSuccess() + 1);
                }
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getFailDetails().add(line.getOrderNo() + "-" + line.getOrderProdNo() + " : " + e.getMessage());
            }
        }
        return result;
    }
    public int updateAdjustSelectDate(Map<String, Object> params) { return returnDirectMapper.updateAdjustSelectDate(params); }
    public int updateAdjustSelectDateSchedule(Map<String, Object> params) { return returnDirectMapper.updateAdjustSelectDateSchedule(params); }
    public int delete(ReturnDirect returnDirect) { return returnDirectMapper.delete(returnDirect); }
    public int deleteRefundCancel(Map<String, Object> params) { return returnDirectMapper.deleteRefundCancel(params); }
}
