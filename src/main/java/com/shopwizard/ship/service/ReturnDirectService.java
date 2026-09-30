package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
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
        }
        return updated;
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
