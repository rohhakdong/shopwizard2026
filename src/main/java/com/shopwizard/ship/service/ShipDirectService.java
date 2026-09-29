package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.ship.mapper.ShipDirectMapper;
import com.shopwizard.ship.model.ShipDirect;
import com.shopwizard.ship.model.ShipDirectCheckRequest;
import com.shopwizard.ship.model.ShipDirectIssueRequest;
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
public class ShipDirectService {
    private final ShipDirectMapper shipDirectMapper;
    private final OrderProdMapper orderProdMapper;
    private final ShipDirectIssueService shipDirectIssueService;
    private final ShipDirectCheckService shipDirectCheckService;
    private final ShipDirectCompleteService shipDirectCompleteService;

    /**
     * 선택한 지불완료 주문라인들을 건별로 출고지시 전환한다 (출고지시 화면, ship-direct-issue.js).
     * 건별로 {@link ShipDirectIssueService#issueOne} 이 REQUIRES_NEW 로 독립 커밋되므로,
     * 한 건이 실패해도 나머지 건 처리에는 영향이 없다.
     */
    public ShipDirectIssueResult issueBatch(ShipDirectIssueRequest req) {
        ShipDirectIssueResult result = new ShipDirectIssueResult();
        if (req.getLines() == null) return result;
        for (ShipDirectIssueRequest.Line line : req.getLines()) {
            try {
                String err = shipDirectIssueService.issueOne(
                        line.getOrderNo(), line.getOrderProdNo(), req.getRegistId(), req.getRegistName());
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

    /**
     * 선택한 출고지시 건들을 건별로 발주확인 전환한다 (발주확인 화면, ship-direct-check.js).
     * 건별로 {@link ShipDirectCheckService#checkOne} 이 REQUIRES_NEW 로 독립 커밋되므로,
     * 한 건이 실패해도 나머지 건 처리에는 영향이 없다.
     */
    public ShipDirectIssueResult checkBatch(ShipDirectCheckRequest req) {
        ShipDirectIssueResult result = new ShipDirectIssueResult();
        if (req.getLines() == null) return result;
        for (ShipDirectCheckRequest.Line line : req.getLines()) {
            try {
                String err = shipDirectCheckService.checkOne(
                        line.getOrderNo(), line.getOrderProdNo(), line.getOrderChangeNo(), req.getRegistId(), req.getRegistName());
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

    /**
     * 선택한 배송시작 건들을 건별로 배송완료 전환한다 (배송완료 화면, ship-direct-complete.js).
     * 건별로 {@link ShipDirectCompleteService#completeOne} 이 REQUIRES_NEW 로 독립 커밋되므로,
     * 한 건이 실패해도 나머지 건 처리에는 영향이 없다.
     */
    public ShipDirectIssueResult completeBatch(ShipDirectCheckRequest req) {
        ShipDirectIssueResult result = new ShipDirectIssueResult();
        if (req.getLines() == null) return result;
        for (ShipDirectCheckRequest.Line line : req.getLines()) {
            try {
                String err = shipDirectCompleteService.completeOne(
                        line.getOrderNo(), line.getOrderProdNo(), line.getOrderChangeNo(), req.getRegistId(), req.getRegistName());
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
    public List<ShipDirect> selectList(Map<String, Object> params) { return shipDirectMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return shipDirectMapper.selectCount(params); }
    public ShipDirect select(Map<String, Object> params) { return shipDirectMapper.select(params); }
    public ShipDirect selectSimple(Map<String, Object> params) { return shipDirectMapper.selectSimple(params); }
    public List<ShipDirect> selectListShipPlace(Map<String, Object> params) { return shipDirectMapper.selectListShipPlace(params); }
    public List<ShipDirect> selectListPrint2(Map<String, Object> params) { return shipDirectMapper.selectListPrint2(params); }
    public int selectCountPrint2(Map<String, Object> params) { return shipDirectMapper.selectCountPrint2(params); }
    public int insert(ShipDirect shipDirect) { return shipDirectMapper.insert(shipDirect); }
    public int insertShipDirect(Map<String, Object> params) { return shipDirectMapper.insertShipDirect(params); }
    public int insertShipDirectPrint(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrint(params); }
    public int insertShipDirectPrintByMatching(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrintByMatching(params); }
    public int checkShipDirectPrintByMatchingSocial(Map<String, Object> params) { return shipDirectMapper.checkShipDirectPrintByMatchingSocial(params); }
    public int insertShipDirectPrintByMatchingSocial(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrintByMatchingSocial(params); }
    public int insertShipDirectPrintWithGift(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrintWithGift(params); }
    public int update(Map<String, Object> params) { return shipDirectMapper.update(params); }
    public int updatePrint(Map<String, Object> params) { return shipDirectMapper.updatePrint(params); }
    public int updatePrintSortName(Map<String, Object> params) { return shipDirectMapper.updatePrintSortName(params); }
    public int updateRefundCancel(Map<String, Object> params) { return shipDirectMapper.updateRefundCancel(params); }
    /**
     * 송장입력(구 직배송 관리, ship-direct.js) — 발주확인 상태에서만 성공하며(updateInvoice 의
     * WHERE OrderState='발주확인' 가드), 성공 시 tOrdOrderProd.OrderState 도 함께 '배송시작' 으로
     * 동기화한다(출고지시/발주확인과 동일한 라인 단위 동기화 컨벤션).
     */
    public int updateInvoice(Map<String, Object> params) {
        int updated = shipDirectMapper.updateInvoice(params);
        if (updated > 0) {
            Map<String, Object> prodUpd = new HashMap<>();
            prodUpd.put("orderNo", params.get("orderNo"));
            prodUpd.put("orderProdNo", params.get("orderProdNo"));
            prodUpd.put("orderState", params.get("orderState"));
            prodUpd.put("changeId", params.get("changeId"));
            prodUpd.put("changeName", params.get("changeName"));
            orderProdMapper.updateOrderState(prodUpd);
        }
        return updated;
    }
    public int updateAdjustSelectDate(Map<String, Object> params) { return shipDirectMapper.updateAdjustSelectDate(params); }
    public int updateAdjustSelectDateSchedule(Map<String, Object> params) { return shipDirectMapper.updateAdjustSelectDateSchedule(params); }
    public int countInvoice(Map<String, Object> params) { return shipDirectMapper.countInvoice(params); }
    public int delete(ShipDirect shipDirect) { return shipDirectMapper.delete(shipDirect); }
    public List<ShipDirect> selectCmpletList(Map<String, Object> params) { return shipDirectMapper.selectCmpletList(params); }
    public int selectCmpletCount(Map<String, Object> params) { return shipDirectMapper.selectCmpletCount(params); }
    public List<ShipDirect> selectCmpletListByOrder(Map<String, Object> params) { return shipDirectMapper.selectCmpletListByOrder(params); }
    public int selectCmpletCountByOrder(Map<String, Object> params) { return shipDirectMapper.selectCmpletCountByOrder(params); }
}
