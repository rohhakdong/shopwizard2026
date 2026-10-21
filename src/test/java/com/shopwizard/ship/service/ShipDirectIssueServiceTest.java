package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.mapper.UpriceMatchMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.UpriceMatchDetail;
import com.shopwizard.ship.mapper.ShipDirectMapper;
import com.shopwizard.order.service.OrderLineLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 출고지시(지불완료 → 출고지시) 1건 처리의 순서·가드 검증. */
@ExtendWith(MockitoExtension.class)
class ShipDirectIssueServiceTest {

    @Mock private ShipDirectMapper shipDirectMapper;
    @Mock private OrderLineLock lineLock;
    @Mock private OrderProdMapper orderProdMapper;
    @Mock private UpriceMatchMapper upriceMatchMapper;
    @InjectMocks private ShipDirectIssueService service;

    private static OrderProd line() {
        OrderProd op = new OrderProd();
        op.setOrderState("지불완료");
        op.setProdName("번들 상품");
        op.setItemName("옵션A");
        op.setSalePrice(10000);
        op.setNvPrice(500);
        op.setSupplyPrice(8000);
        op.setPromotFeeAmt(100);
        return op;
    }

    private static UpriceMatchDetail detail(int qty, int uprice) {
        UpriceMatchDetail d = new UpriceMatchDetail();
        d.setQty(qty);
        d.setUprice(uprice);
        return d;
    }

    @Test
    void 주문라인이_없으면_아무것도_바꾸지_않는다() {
        when(lineLock.lockLine(any(), any())).thenReturn(null);

        assertThat(service.issueOne(1, 1, "admin", "관리자")).contains("찾을 수 없습니다");

        verify(shipDirectMapper, never()).insertShipDirect(any());
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 발주단가매칭이_없으면_출고지시를_막는다() {
        when(lineLock.lockLine(any(), any())).thenReturn(line());
        when(upriceMatchMapper.selectPriced(anyString(), anyString())).thenReturn(List.of());

        String err = service.issueOne(1, 1, "admin", "관리자");

        assertThat(err).contains("발주단가매칭이 필요합니다").contains("번들 상품").contains("옵션A");
        verify(orderProdMapper, never()).updatePrice(any());
        verify(shipDirectMapper, never()).insertShipDirect(any());
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 규칙이_있으면_발주단가_합계로_원가와_마진을_다시_계산한_뒤_출고지시로_옮긴다() {
        when(lineLock.lockLine(any(), any())).thenReturn(line());
        // 2개 x 3000 + 1개 x 1500 = 7500
        when(upriceMatchMapper.selectPriced("번들 상품", "옵션A")).thenReturn(List.of(detail(2, 3000), detail(1, 1500)));
        when(shipDirectMapper.insertShipDirect(any())).thenReturn(1);

        assertThat(service.issueOne(1, 2, "admin", "관리자")).isNull();

        ArgumentCaptor<OrderProd> priced = ArgumentCaptor.forClass(OrderProd.class);
        // 순서: 원가 갱신 → tShpShipDirect 로 이전(갱신된 BuyPrice 를 복사) → 라인 상태 전환
        InOrder order = inOrder(orderProdMapper, shipDirectMapper);
        order.verify(orderProdMapper).updatePrice(priced.capture());
        order.verify(shipDirectMapper).insertShipDirect(any());
        order.verify(orderProdMapper).updateOrderState(any());

        OrderProd op = priced.getValue();
        assertThat(op.getBuyPrice()).isEqualTo(7500);
        assertThat(op.getProdMargin()).isEqualTo(10000 + 500 - 7500);   // 판매가+옵션가-원가
        assertThat(op.getChnlMargin()).isEqualTo(10000 + 500 - 8000);   // 판매가+옵션가-공급가
        assertThat(op.getNetMargin()).isEqualTo(8000 - 7500 - 100);     // 공급가-원가-프로모션비
    }

    @Test
    void 이미_출고지시됐거나_지불완료가_아니면_상태를_바꾸지_않고_사유를_돌려준다() {
        when(lineLock.lockLine(any(), any())).thenReturn(line());
        when(upriceMatchMapper.selectPriced(anyString(), anyString())).thenReturn(List.of(detail(1, 1000)));
        when(shipDirectMapper.insertShipDirect(any())).thenReturn(0);

        String err = service.issueOne(1, 1, "admin", "관리자");

        assertThat(err).contains("지불완료 상태가 아니거나 이미 출고지시");
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 지불완료가_아닌_라인은_가격도_다시_계산하지_않고_아무것도_쓰지_않는다() {
        // 이미 발주확인·배송된 라인을 다시 출고지시해도 BuyPrice/마진이 최신 규칙으로 덮이면 안 된다
        OrderProd shipped = line();
        shipped.setOrderState("배송시작");
        when(lineLock.lockLine(any(), any())).thenReturn(shipped);

        String err = service.issueOne(1, 1, "admin", "관리자");

        assertThat(err).contains("지불완료 상태가 아니거나 이미 출고지시").contains("현재 상태: 배송시작");
        verifyNoInteractions(upriceMatchMapper, shipDirectMapper);
        verify(orderProdMapper, never()).updatePrice(any());
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 출고지시_상태전환은_출고지시로_기록한다() {
        when(lineLock.lockLine(any(), any())).thenReturn(line());
        when(upriceMatchMapper.selectPriced(anyString(), anyString())).thenReturn(List.of(detail(1, 1000)));
        when(shipDirectMapper.insertShipDirect(any())).thenReturn(1);

        service.issueOne(7, 3, "admin", "관리자");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> upd = ArgumentCaptor.forClass(Map.class);
        verify(orderProdMapper).updateOrderState(upd.capture());
        assertThat(upd.getValue()).containsEntry("orderNo", 7).containsEntry("orderProdNo", 3)
                .containsEntry("orderState", "출고지시").containsEntry("changeId", "admin");
    }
}
