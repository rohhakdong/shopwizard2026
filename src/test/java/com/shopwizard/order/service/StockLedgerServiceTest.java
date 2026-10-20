package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdStockLogMapper;
import com.shopwizard.product.service.ProdService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockLedgerServiceTest {

    @Mock private OrderProdStockLogMapper stockLogMapper;
    @Mock private ProdService productProdService;
    @InjectMocks private StockLedgerService ledger;

    @Test
    void 차감하면_이력을_남긴다() {
        ledger.recordDeduction(100, 1, "G000000001", 3, "admin");

        verify(stockLogMapper).insertDeduct(100, 1, "G000000001", 3, "admin");
    }

    @Test
    void 상품코드나_수량이_없으면_이력을_남기지_않는다() {
        ledger.recordDeduction(100, 1, null, 3, "admin");
        ledger.recordDeduction(100, 1, " ", 3, "admin");
        ledger.recordDeduction(100, 1, "G000000001", 0, "admin");
        ledger.recordDeduction(100, 1, "G000000001", null, "admin");
        ledger.recordDeduction(null, 1, "G000000001", 3, "admin");

        verifyNoInteractions(stockLogMapper);
    }

    @Test
    void 차감_이력이_있으면_이력에_기록된_상품과_수량_그대로_복원한다() {
        when(stockLogMapper.insertRestoreFromDeduct(100, 1, "admin")).thenReturn(1);
        when(stockLogMapper.selectDeduct(100, 1)).thenReturn(Map.of("prodCode", "G000000001", "qty", 3));

        int restored = ledger.restore(100, 1, "admin");

        assertThat(restored).isEqualTo(3);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> inc = ArgumentCaptor.forClass(Map.class);
        verify(productProdService).increaseSupplyQty(inc.capture());
        assertThat(inc.getValue()).containsEntry("prodCode", "G000000001").containsEntry("qty", 3);
    }

    @Test
    void 차감_이력이_없거나_이미_복원됐으면_재고를_건드리지_않는다() {
        // INSERT ... SELECT 가 0행: 차감 이력이 없거나 PK 때문에 이미 복원 이력이 있다
        when(stockLogMapper.insertRestoreFromDeduct(100, 1, "admin")).thenReturn(0);

        assertThat(ledger.restore(100, 1, "admin")).isZero();

        verify(stockLogMapper, never()).selectDeduct(anyInt(), anyInt());
        verifyNoInteractions(productProdService);
    }

    @Test
    void 주문번호가_없으면_아무것도_하지_않는다() {
        assertThat(ledger.restore(null, 1, "admin")).isZero();
        assertThat(ledger.restore(100, null, "admin")).isZero();

        verifyNoInteractions(stockLogMapper, productProdService);
    }
}
