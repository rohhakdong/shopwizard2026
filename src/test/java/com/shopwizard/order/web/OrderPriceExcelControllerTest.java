package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderPriceResult;
import com.shopwizard.order.service.OrderPriceExcelService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 주문 엑셀 가격수정 컨트롤러 — 파일 종류 검사와 서비스 위임. */
class OrderPriceExcelControllerTest {

    private final OrderPriceExcelService service = mock(OrderPriceExcelService.class);
    private final OrderPriceExcelController controller = new OrderPriceExcelController(service);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "application/octet-stream", bytes);
    }

    @Test
    void 양식은_priceExcel_xls_로_내려간다() {
        when(service.buildTemplate()).thenReturn(new byte[]{1, 2});

        ResponseEntity<byte[]> res = controller.template();

        assertArrayEquals(new byte[]{1, 2}, res.getBody());
        assertTrue(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("priceExcel.xls"));
    }

    @Test
    void 엑셀이_아니거나_비어_있으면_400_이고_서비스를_부르지_않는다() {
        for (MockMultipartFile bad : new MockMultipartFile[]{file("a.csv", new byte[]{1}), file("a.xls", new byte[0]), file("noext", new byte[]{1})}) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.preview(bad, request));
            assertEquals(400, ex.getStatusCode().value());
            assertThrows(ResponseStatusException.class, () -> controller.importFile(bad, request));
        }
        assertThrows(ResponseStatusException.class, () -> controller.preview(null, request));
        verifyNoInteractions(service);
    }

    @Test
    void xls_와_xlsx_는_서비스로_넘긴다() throws Exception {
        OrderPriceResult result = new OrderPriceResult();
        when(service.preview(any(), eq(request))).thenReturn(result);
        when(service.importFile(any(), eq("P.XLSX"), eq(request))).thenReturn(result);

        assertSame(result, controller.preview(file("a.xls", new byte[]{1}), request));
        assertSame(result, controller.importFile(file("P.XLSX", new byte[]{1}), request));
    }
}
