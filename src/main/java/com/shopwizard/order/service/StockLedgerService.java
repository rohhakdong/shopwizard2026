package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdStockLogMapper;
import com.shopwizard.product.service.ProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 주문라인별 재고 차감/복원 이력. 재고(SupplyQty)는 샵링커 수집과 상품매칭에서만 차감되고 그 이력이
 * 어디에도 남지 않아서, 이력 없이 "취소/반품이니까 되돌린다"고 하면 차감한 적 없는 건(스토어프론트 결제,
 * 이 시스템 도입 전 이관 데이터)을 더하거나 같은 건을 두 번 더하게 된다. 그래서 차감할 때 이력을 남기고,
 * 복원은 <b>이력이 있는 라인에 한해 정확히 1회</b>(이력 테이블 PK가 이중 복원을 막는다)만 한다.
 *
 * <p>호출자의 트랜잭션에 참여한다(주문 상태 변경과 재고 복원이 함께 커밋/롤백되도록).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class StockLedgerService {

    private final OrderProdStockLogMapper stockLogMapper;
    private final ProdService productProdService;   // com.shopwizard.product.service.ProdService

    /** 재고를 실제로 차감한 직후에 호출해 이력을 남긴다. 값이 부족하거나 이미 기록돼 있으면 아무것도 안 한다. */
    public void recordDeduction(Integer orderNo, Integer orderProdNo, String prodCode, Integer qty, String registId) {
        if (orderNo == null || orderProdNo == null || prodCode == null || prodCode.isBlank()
                || qty == null || qty <= 0) {
            return;
        }
        stockLogMapper.insertDeduct(orderNo, orderProdNo, prodCode, qty, registId);
    }

    /**
     * 그 라인에 차감 이력이 있고 아직 복원된 적이 없으면 이력에 기록된 상품코드/수량 그대로 재고를 되돌린다.
     *
     * @return 복원한 수량. 이력이 없거나 이미 복원됐으면 0(재고를 건드리지 않음).
     */
    public int restore(Integer orderNo, Integer orderProdNo, String registId) {
        if (orderNo == null || orderProdNo == null) return 0;
        if (stockLogMapper.insertRestoreFromDeduct(orderNo, orderProdNo, registId) == 0) return 0;

        Map<String, Object> deduct = stockLogMapper.selectDeduct(orderNo, orderProdNo);
        if (deduct == null) return 0;
        int qty = ((Number) deduct.get("qty")).intValue();

        Map<String, Object> inc = new HashMap<>();
        inc.put("prodCode", deduct.get("prodCode"));
        inc.put("qty", qty);
        productProdService.increaseSupplyQty(inc);
        return qty;
    }
}
