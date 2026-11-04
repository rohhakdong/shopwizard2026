package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustDeliAddrMapper;
import com.shopwizard.profile.model.CustDeliAddr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 고객 본인 배송지: 검증, 개수 제한, 기본 배송지 규칙, 소유권(모든 쿼리가 custId 를 함께 건다). */
@ExtendWith(MockitoExtension.class)
class CustDeliAddrServiceTest {

    private static final int CUST = 2;

    @Mock private CustDeliAddrMapper mapper;
    private CustDeliAddrService service;

    @BeforeEach
    void setUp() {
        service = new CustDeliAddrService(mapper);
    }

    private static CustDeliAddr addr() {
        CustDeliAddr a = new CustDeliAddr();
        a.setRecverName(" 홍길동 ");
        a.setMobilePhoneNo(" 010-1234-5678 ");
        a.setZipcode("06236");
        a.setAddr1("서울시 강남구 테헤란로 1");
        a.setAddr2(" 101호 ");
        return a;
    }

    private static CustDeliAddr row(int no, int def) {
        CustDeliAddr r = new CustDeliAddr();
        r.setCustId(CUST);
        r.setDeliAddrNo(no);
        r.setDefaltYn(def);
        return r;
    }

    // ── 추가 ────────────────────────────────────────────────────

    @Test
    void 첫_배송지는_자동으로_기본이_되고_입력은_다듬어_저장한다() {
        when(mapper.countMine(CUST)).thenReturn(0);

        CustDeliAddr saved = service.insertMine(CUST, addr(), "cust01", "홍고객");

        ArgumentCaptor<CustDeliAddr> cap = ArgumentCaptor.forClass(CustDeliAddr.class);
        verify(mapper).insertMine(cap.capture());
        CustDeliAddr a = cap.getValue();
        assertThat(a.getCustId()).isEqualTo(CUST);
        assertThat(a.getDefaltYn()).isEqualTo(1);
        assertThat(a.getRecverName()).isEqualTo("홍길동");
        assertThat(a.getMobilePhoneNo()).isEqualTo("010-1234-5678");
        assertThat(a.getAddr2()).isEqualTo("101호");
        assertThat(a.getRemark()).isNull();   // 배송지 이름은 선택
        assertThat(a.getRegistId()).isEqualTo("cust01");
        assertThat(a.getRegistName()).isEqualTo("홍고객");
        assertThat(saved).isSameAs(a);
        verify(mapper).lockCust(CUST);        // 같은 회원의 동시 변경을 직렬화
        verify(mapper).clearDefault(CUST);
    }

    @Test
    void 두번째부터는_기본으로_지정하지_않으면_기본이_바뀌지_않는다() {
        when(mapper.countMine(CUST)).thenReturn(1);

        service.insertMine(CUST, addr(), "cust01", "홍고객");

        ArgumentCaptor<CustDeliAddr> cap = ArgumentCaptor.forClass(CustDeliAddr.class);
        verify(mapper).insertMine(cap.capture());
        assertThat(cap.getValue().getDefaltYn()).isZero();
        verify(mapper, never()).clearDefault(anyInt());
    }

    @Test
    void 기본으로_지정해서_추가하면_기존_기본은_먼저_해제한다() {
        when(mapper.countMine(CUST)).thenReturn(3);
        CustDeliAddr in = addr();
        in.setDefaltYn(1);

        service.insertMine(CUST, in, "cust01", "홍고객");

        InOrder order = inOrder(mapper);
        order.verify(mapper).clearDefault(CUST);
        order.verify(mapper).insertMine(any());
    }

    @Test
    void 요청_바디의_custId_는_무시하고_세션_custId_로_저장한다() {
        when(mapper.countMine(CUST)).thenReturn(0);
        CustDeliAddr in = addr();
        in.setCustId(999);   // 다른 회원 id 를 끼워 넣으려는 시도

        service.insertMine(CUST, in, "cust01", "홍고객");

        ArgumentCaptor<CustDeliAddr> cap = ArgumentCaptor.forClass(CustDeliAddr.class);
        verify(mapper).insertMine(cap.capture());
        assertThat(cap.getValue().getCustId()).isEqualTo(CUST);
    }

    @Test
    void 열_개가_차_있으면_추가를_거절한다() {
        when(mapper.countMine(CUST)).thenReturn(CustDeliAddrService.MAX_PER_CUST);

        assertThatThrownBy(() -> service.insertMine(CUST, addr(), "cust01", "홍고객"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(400);
                    assertThat(e.getReason()).contains("10");
                });
        verify(mapper, never()).insertMine(any());
    }

    @Test
    void 필수_값이나_형식이_틀리면_400이고_저장하지_않는다() {
        java.util.List<java.util.function.Consumer<CustDeliAddr>> breakers = java.util.List.of(
                a -> a.setRecverName(" "),
                a -> a.setRecverName("가".repeat(51)),
                a -> a.setMobilePhoneNo(""),
                a -> a.setMobilePhoneNo("010 1234 5678"),
                a -> a.setMobilePhoneNo("12345"),
                a -> a.setZipcode(""),
                a -> a.setZipcode("서울"),
                a -> a.setAddr1(" "),
                a -> a.setAddr1("가".repeat(256)),
                a -> a.setAddr2("가".repeat(256)),
                a -> a.setRemark("가".repeat(51)));
        for (java.util.function.Consumer<CustDeliAddr> breaker : breakers) {
            CustDeliAddr in = addr();
            breaker.accept(in);
            assertThatThrownBy(() -> service.insertMine(CUST, in, "cust01", "홍고객"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        }
        verify(mapper, never()).insertMine(any());
    }

    // ── 주문서에서 저장 ──────────────────────────────────────────

    @Test
    void 주문서_저장은_새_주소를_저장하되_기본_배송지는_바꾸지_않는다() {
        when(mapper.countMine(CUST)).thenReturn(2);
        CustDeliAddr in = addr();
        in.setDefaltYn(1);   // 자동 저장은 기본을 가로채지 않는다

        assertThat(service.addIfAbsent(CUST, in, "cust01", "홍고객")).isTrue();

        ArgumentCaptor<CustDeliAddr> cap = ArgumentCaptor.forClass(CustDeliAddr.class);
        verify(mapper).insertMine(cap.capture());
        assertThat(cap.getValue().getDefaltYn()).isZero();
        verify(mapper, never()).clearDefault(anyInt());
    }

    @Test
    void 주문서_저장은_첫_배송지면_기본으로_만든다() {
        when(mapper.countMine(CUST)).thenReturn(0);

        assertThat(service.addIfAbsent(CUST, addr(), "cust01", "홍길동")).isTrue();

        ArgumentCaptor<CustDeliAddr> cap = ArgumentCaptor.forClass(CustDeliAddr.class);
        verify(mapper).insertMine(cap.capture());
        assertThat(cap.getValue().getDefaltYn()).isEqualTo(1);
    }

    @Test
    void 주문서_저장은_같은_배송지가_있거나_가득_찼거나_값이_이상하면_조용히_건너뛴다() {
        // 같은 배송지가 이미 있다
        when(mapper.countMine(CUST)).thenReturn(1);
        when(mapper.findSame(any(), any())).thenReturn(row(5, 0));
        assertThat(service.addIfAbsent(CUST, addr(), "cust01", "홍길동")).isFalse();

        // 가득 찼다
        when(mapper.countMine(CUST)).thenReturn(CustDeliAddrService.MAX_PER_CUST);
        assertThat(service.addIfAbsent(CUST, addr(), "cust01", "홍길동")).isFalse();

        // 저장 규칙에 안 맞는다(예전 한 칸 주소라 우편번호가 없는 경우 등) — 예외 없이 false
        CustDeliAddr noZip = addr();
        noZip.setZipcode(null);
        assertThat(service.addIfAbsent(CUST, noZip, "cust01", "홍길동")).isFalse();

        verify(mapper, never()).insertMine(any());
    }

    // ── 수정 · 기본 · 삭제 (소유권) ───────────────────────────────

    @Test
    void 다른_회원의_배송지를_수정하면_404다_쿼리가_custId_로_걸러낸다() {
        when(mapper.updateMine(any())).thenReturn(0);   // WHERE CustId=세션 AND DeliAddrNo=? 에 걸리는 행이 없다

        assertThatThrownBy(() -> service.updateMine(CUST, 77, addr(), "cust01", "홍길동"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));

        ArgumentCaptor<CustDeliAddr> cap = ArgumentCaptor.forClass(CustDeliAddr.class);
        verify(mapper).updateMine(cap.capture());
        assertThat(cap.getValue().getCustId()).isEqualTo(CUST);
        assertThat(cap.getValue().getDeliAddrNo()).isEqualTo(77);
        verify(mapper, never()).setDefault(anyInt(), anyInt());
    }

    @Test
    void 수정하면서_기본으로_지정하면_기존_기본을_해제하고_지정한다() {
        when(mapper.updateMine(any())).thenReturn(1);
        when(mapper.selectOne(CUST, 7)).thenReturn(row(7, 1));
        CustDeliAddr in = addr();
        in.setDefaltYn(1);

        service.updateMine(CUST, 7, in, "cust01", "홍길동");

        InOrder order = inOrder(mapper);
        order.verify(mapper).clearDefault(CUST);
        order.verify(mapper).setDefault(CUST, 7);
    }

    @Test
    void 남의_배송지를_기본으로_지정하려_하면_404이고_내_기본은_그대로다() {
        when(mapper.selectOne(CUST, 77)).thenReturn(null);

        assertThatThrownBy(() -> service.setDefaultMine(CUST, 77))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));

        verify(mapper, never()).clearDefault(anyInt());   // 내 기본 배송지가 해제되면 안 된다
        verify(mapper, never()).setDefault(anyInt(), anyInt());
    }

    @Test
    void 기본_배송지_지정은_기존_기본을_해제한_뒤_지정한다() {
        when(mapper.selectOne(CUST, 7)).thenReturn(row(7, 0));

        service.setDefaultMine(CUST, 7);

        InOrder order = inOrder(mapper);
        order.verify(mapper).clearDefault(CUST);
        order.verify(mapper).setDefault(CUST, 7);
    }

    @Test
    void 기본_배송지를_삭제하면_가장_최근_배송지가_기본이_된다() {
        when(mapper.selectOne(CUST, 7)).thenReturn(row(7, 1));

        service.deleteMine(CUST, 7);

        verify(mapper).deleteMine(CUST, 7);
        verify(mapper).promoteLatest(CUST);
    }

    @Test
    void 기본이_아닌_배송지를_삭제해도_기본은_그대로다() {
        when(mapper.selectOne(CUST, 8)).thenReturn(row(8, 0));

        service.deleteMine(CUST, 8);

        verify(mapper).deleteMine(CUST, 8);
        verify(mapper, never()).promoteLatest(anyInt());
    }

    @Test
    void 남의_배송지_삭제는_404이고_지우지_않는다() {
        when(mapper.selectOne(CUST, 77)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteMine(CUST, 77))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));

        verify(mapper, never()).deleteMine(anyInt(), anyInt());
    }
}
