package com.shopwizard.framework.auth;

import com.shopwizard.order.model.Basket;
import com.shopwizard.order.model.OrderCancelRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 요청 바디의 감사 필드(registId/registName/changeId/changeName)를 로그인 행위자 값으로 강제하는 규칙. */
class AuditFieldsTest {

    private static final AuthSession.Actor ME = new AuthSession.Actor("admin01", "홍길동");

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    // ── Map 바디 ─────────────────────────────────────────────────────────

    @Test
    void POST_는_등록자_계열을_세션_값으로_강제하고_위조값을_버린다() {
        Map<String, Object> body = map("registId", "FORGED", "registName", "위조", "prodCode", "P1");

        AuditFields.apply(body, ME, "POST");

        assertThat(body).containsEntry("registId", "admin01").containsEntry("registName", "홍길동")
                .containsEntry("prodCode", "P1");
    }

    @Test
    void 클라이언트가_registId_를_빼먹어도_POST_는_세션_값으로_채운다() {
        Map<String, Object> body = map("prodCode", "P1");

        AuditFields.apply(body, ME, "POST");

        assertThat(body).containsEntry("registId", "admin01").containsEntry("registName", "홍길동");
        assertThat(body).doesNotContainKeys("changeId", "changeName");   // 반대 계열은 보내지 않았으면 만들지 않는다
    }

    @Test
    void PUT_PATCH_DELETE_는_수정자_계열을_세션_값으로_강제한다() {
        for (String method : new String[]{"PUT", "PATCH", "DELETE"}) {
            Map<String, Object> body = map("changeId", "FORGED", "orderNo", 7);

            AuditFields.apply(body, ME, method);

            assertThat(body).as(method).containsEntry("changeId", "admin01").containsEntry("changeName", "홍길동")
                    .containsEntry("orderNo", 7);
            assertThat(body).as(method).doesNotContainKeys("registId", "registName");
        }
    }

    @Test
    void 반대_계열도_클라이언트가_보냈다면_위조값을_덮어쓴다() {
        // 예: 배치 게이트는 POST 인데 changeId 를 같이 보낸 경우, 엔티티 PUT 이 registId 를 같이 보낸 경우
        Map<String, Object> post = map("registId", "A", "changeId", "FORGED");
        Map<String, Object> put = map("changeId", "B", "registId", "FORGED");

        AuditFields.apply(post, ME, "POST");
        AuditFields.apply(put, ME, "PUT");

        assertThat(post).containsEntry("registId", "admin01").containsEntry("changeId", "admin01");
        assertThat(put).containsEntry("changeId", "admin01").containsEntry("registId", "admin01");
    }

    @Test
    void 행위자_이름이_없으면_빈_문자열로_채운다() {
        Map<String, Object> body = new HashMap<>();

        AuditFields.apply(body, new AuthSession.Actor("admin01", null), "POST");

        assertThat(body).containsEntry("registId", "admin01").containsEntry("registName", "");
    }

    // ── 빈(POJO) 바디 ────────────────────────────────────────────────────

    @Test
    void 빈_POST_는_등록자_계열만_강제한다() {
        Basket b = new Basket();
        b.setRegistId("FORGED");
        b.setChangeId("KEEP-NOT");   // 반대 계열: 보냈으니 덮어쓴다

        AuditFields.apply(b, ME, "POST");

        assertThat(b.getRegistId()).isEqualTo("admin01");
        assertThat(b.getRegistName()).isEqualTo("홍길동");        // 보내지 않았어도 채워진다
        assertThat(b.getChangeId()).isEqualTo("admin01");        // 보낸 값이라 위조값 제거
        assertThat(b.getChangeName()).isNull();                  // 보내지 않은 반대 계열은 그대로
    }

    @Test
    void 빈_PUT_은_수정자_계열을_강제하고_보내지_않은_등록자_계열은_그대로_둔다() {
        Basket b = new Basket();
        b.setRegistId("ORIGINAL");   // 수정 폼이 DB 값을 그대로 되돌려 보낸 등록자

        AuditFields.apply(b, ME, "PUT");

        assertThat(b.getChangeId()).isEqualTo("admin01");
        assertThat(b.getChangeName()).isEqualTo("홍길동");
        assertThat(b.getRegistId()).isEqualTo("admin01");        // 값을 보낸 필드는 세션 값으로 통일(위조 방지)
        assertThat(b.getRegistName()).isNull();
    }

    @Test
    void 감사_프로퍼티가_없는_빈은_건드리지_않는다() {
        OrderCancelRequest.Line line = new OrderCancelRequest.Line();
        line.setOrderNo(1);

        AuditFields.apply(line, ME, "POST");   // registId 프로퍼티가 없는 모델 — 예외 없이 통과

        assertThat(line.getOrderNo()).isEqualTo(1);
    }

    @Test
    void 배치_요청_DTO_의_registId_를_강제하고_라인은_그대로다() {
        OrderCancelRequest req = new OrderCancelRequest();
        req.setRegistId("FORGED");
        req.setRegistName("위조");
        OrderCancelRequest.Line l = new OrderCancelRequest.Line();
        l.setOrderNo(10);
        l.setOrderProdNo(2);
        req.setLines(List.of(l));

        AuditFields.apply(req, ME, "POST");

        assertThat(req.getRegistId()).isEqualTo("admin01");
        assertThat(req.getRegistName()).isEqualTo("홍길동");
        assertThat(req.getLines()).hasSize(1);
        assertThat(req.getLines().get(0).getOrderNo()).isEqualTo(10);
    }

    // ── 중첩 / 컬렉션 ────────────────────────────────────────────────────

    @Test
    void 중첩_Map_과_리스트_안의_위조값은_덮어쓰되_없던_필드는_만들지_않는다() {
        Map<String, Object> item1 = map("registId", "FORGED", "x", 1);
        Map<String, Object> item2 = map("x", 2);
        Map<String, Object> body = map("lines", new ArrayList<>(List.of(item1, item2)), "inner", map("changeId", "FORGED"));

        AuditFields.apply(body, ME, "POST");

        assertThat(item1).containsEntry("registId", "admin01").containsEntry("x", 1);
        assertThat(item2).doesNotContainKeys("registId", "registName");
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) body.get("inner");
        assertThat(inner).containsEntry("changeId", "admin01").doesNotContainKey("registId");
    }

    @Test
    void 최상위가_리스트면_각_원소를_최상위로_취급한다() {
        Map<String, Object> a = map("name", "a");
        Map<String, Object> b = map("name", "b", "registId", "FORGED");
        List<Map<String, Object>> body = List.of(a, b);

        AuditFields.apply(body, ME, "POST");

        assertThat(a).containsEntry("registId", "admin01");   // 보내지 않았어도 채워진다
        assertThat(b).containsEntry("registId", "admin01");
    }

    // ── 예외 상황 ────────────────────────────────────────────────────────

    @Test
    void 본문이나_행위자가_없으면_아무것도_하지_않는다() {
        Map<String, Object> body = map("registId", "KEEP");

        AuditFields.apply(null, ME, "POST");
        AuditFields.apply(body, null, "POST");

        assertThat(body).containsEntry("registId", "KEEP");
    }

    @Test
    void 불변_Map_이어도_예외없이_통과한다() {
        Map<String, Object> immutable = Map.of("registId", "X");

        AuditFields.apply(immutable, ME, "POST");   // UnsupportedOperationException 을 삼킨다

        assertThat(immutable).containsEntry("registId", "X");
    }

    @Test
    void 문자열_같은_단순_본문은_무시한다() {
        AuditFields.apply("plain text", ME, "POST");
        AuditFields.apply(42, ME, "POST");
    }
}
