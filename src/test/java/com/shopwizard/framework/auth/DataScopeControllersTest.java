package com.shopwizard.framework.auth;

import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.service.ProdImgUploadService;
import com.shopwizard.catalog.service.ProdService;
import com.shopwizard.company.model.Chnl;
import com.shopwizard.company.model.Shop;
import com.shopwizard.company.service.ChnlService;
import com.shopwizard.company.service.ShopService;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.ship.model.ReturnRequestBatchRequest;
import com.shopwizard.ship.model.ShipDirect;
import com.shopwizard.ship.model.ShipDirectCheckRequest;
import com.shopwizard.ship.service.ReturnDirectService;
import com.shopwizard.ship.service.ShipDirectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 상점/채널 제한 관리자가 남의 행을 건드리는 호출은 서비스에 닿기 전에 403 으로 막힌다. */
class DataScopeControllersTest {

    private DataScopeMapper mapper;
    private DataScopeGuard guard;

    @BeforeEach
    void setUp() {
        mapper = mock(DataScopeMapper.class);
        guard = new DataScopeGuard(mapper);
        ReflectionTestUtils.setField(guard, "mode", "enforce");
    }

    private static MockHttpServletRequest shopAdmin(String shop) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("loginId", "u", "shopCode", shop));
        return req;
    }

    private static MockHttpServletRequest chnlAdmin(String chnl) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("loginId", "u", "chnlCode", chnl));
        return req;
    }

    private static ShipDirectCheckRequest.Line line(int orderNo, int orderProdNo) {
        ShipDirectCheckRequest.Line l = new ShipDirectCheckRequest.Line();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(orderProdNo);
        return l;
    }

    @Test
    void 발주확인_배치에_남의_라인이_섞이면_서비스를_호출하지_않고_403() {
        ShipDirectService service = mock(ShipDirectService.class);
        var controller = new com.shopwizard.ship.web.ShipDirectController(service, guard);
        when(mapper.countLines(1, 1, "MINE", null)).thenReturn(1);
        when(mapper.countLines(2, 1, "MINE", null)).thenReturn(0);

        ShipDirectCheckRequest req = new ShipDirectCheckRequest();
        req.setLines(List.of(line(1, 1), line(2, 1)));

        assertThrows(ResponseStatusException.class, () -> controller.checkBatch(req, shopAdmin("MINE")));
        verifyNoInteractions(service);

        req.setLines(List.of(line(1, 1)));
        controller.checkBatch(req, shopAdmin("MINE"));
        verify(service).checkBatch(req);
    }

    @Test
    void 송장입력은_남의_라인이면_403_내_라인은_서비스로() {
        ShipDirectService service = mock(ShipDirectService.class);
        var controller = new com.shopwizard.ship.web.ShipDirectController(service, guard);
        when(mapper.countLines(5, 1, "MINE", null)).thenReturn(1);

        assertThrows(ResponseStatusException.class,
                () -> controller.updateInvoice(Map.of("orderNo", 6, "orderProdNo", 1, "invNo", "X"), shopAdmin("MINE")));
        verify(service, never()).updateInvoice(any());

        controller.updateInvoice(Map.of("orderNo", 5, "orderProdNo", 1, "invNo", "X"), shopAdmin("MINE"));
        verify(service).updateInvoice(any());
    }

    @Test
    void 단건_조회는_결과_행의_상점이_다르면_403() {
        ShipDirectService service = mock(ShipDirectService.class);
        var controller = new com.shopwizard.ship.web.ShipDirectController(service, guard);
        ShipDirect foreign = new ShipDirect();
        foreign.setShopCode("OTHER");
        when(service.select(any())).thenReturn(foreign);

        assertThrows(ResponseStatusException.class, () -> controller.select(Map.of("pOrderNo", "1"), shopAdmin("MINE")));
        foreign.setShopCode("MINE");
        assertEquals(foreign, controller.select(Map.of("pOrderNo", "1"), shopAdmin("MINE")));
    }

    @Test
    void 반품요청_배치도_소유권을_확인한다() {
        ReturnDirectService service = mock(ReturnDirectService.class);
        var controller = new com.shopwizard.ship.web.ReturnDirectController(service, guard);
        ReturnRequestBatchRequest req = new ReturnRequestBatchRequest();
        ReturnRequestBatchRequest.Line l = new ReturnRequestBatchRequest.Line();
        l.setOrderNo(9);
        l.setOrderProdNo(1);
        req.setLines(List.of(l));

        assertThrows(ResponseStatusException.class, () -> controller.requestBatch(req, chnlAdmin("C1")));
        verifyNoInteractions(service);
    }

    @Test
    void 상품_등록은_바디의_상점코드를_세션_상점으로_고정한다() {
        ProdService service = mock(ProdService.class);
        var controller = new com.shopwizard.catalog.web.ProdController(service, mock(ProdImgUploadService.class), guard);
        Prod prod = new Prod();
        prod.setShopCode("OTHER");

        controller.insert(prod, shopAdmin("MINE"));
        assertEquals("MINE", prod.getShopCode());
        verify(service).insert(prod);
    }

    @Test
    void 상품_수정_삭제_승인은_남의_상품이면_403() {
        ProdService service = mock(ProdService.class);
        var controller = new com.shopwizard.catalog.web.ProdController(service, mock(ProdImgUploadService.class), guard);
        Prod prod = new Prod();
        prod.setProdCode("P-OTHER");

        assertThrows(ResponseStatusException.class, () -> controller.update(prod, shopAdmin("MINE")));
        assertThrows(ResponseStatusException.class, () -> controller.delete("P-OTHER", shopAdmin("MINE")));
        assertThrows(ResponseStatusException.class,
                () -> controller.approv(Map.of("prodCodes", List.of("P-OTHER")), shopAdmin("MINE")));
        verifyNoInteractions(service);
    }

    @Test
    void 상점_목록은_상점_제한_계정에게_자기_상점만_채널_제한_계정에게는_코드_이름만() {
        ShopService service = mock(ShopService.class);
        Shop a = new Shop(); a.setShopCode("A"); a.setShopName("가게A"); a.setEmail("a@x"); a.setPhoneNo("010-1");
        Shop b = new Shop(); b.setShopCode("B"); b.setShopName("가게B"); b.setEmail("b@x"); b.setPhoneNo("010-2");
        when(service.getList(any())).thenReturn(List.of(a, b));
        var controller = new com.shopwizard.company.web.ShopController(service, guard);

        List<Shop> mine = controller.getList(Map.of(), shopAdmin("A"));
        assertEquals(1, mine.size());
        assertEquals("a@x", mine.get(0).getEmail());
        assertEquals(1, controller.getCount(Map.of(), shopAdmin("A")));
        assertThrows(ResponseStatusException.class, () -> controller.get("B", shopAdmin("A")));

        List<Shop> forChnl = controller.getList(Map.of(), chnlAdmin("C"));
        assertEquals(2, forChnl.size());
        assertEquals(null, forChnl.get(0).getEmail());      // 세부 정보는 비운다
        assertEquals("가게A", forChnl.get(0).getShopName());
    }

    @Test
    void 채널_목록은_채널_제한_계정에게_자기_채널만_상점_제한_계정에게는_코드_이름만() {
        ChnlService service = mock(ChnlService.class);
        Chnl c1 = new Chnl(); c1.setChnlCode("C1"); c1.setChnlName("채널1"); c1.setMngrEmail("m1@x"); c1.setPasswd("secret");
        Chnl c2 = new Chnl(); c2.setChnlCode("C2"); c2.setChnlName("채널2"); c2.setMngrEmail("m2@x");
        when(service.getList(any())).thenReturn(List.of(c1, c2));
        var controller = new com.shopwizard.company.web.ChnlController(service, guard);

        List<Chnl> mine = controller.getList(Map.of(), chnlAdmin("C1"));
        assertEquals(1, mine.size());
        assertEquals("m1@x", mine.get(0).getMngrEmail());
        assertEquals(null, mine.get(0).getPasswd());         // 비밀번호는 누구에게도 내려주지 않는다
        assertThrows(ResponseStatusException.class, () -> controller.get("C2", chnlAdmin("C1")));

        List<Chnl> forShop = controller.getList(Map.of(), shopAdmin("S"));
        assertEquals(2, forShop.size());
        assertEquals(null, forShop.get(0).getMngrEmail());
    }
}
