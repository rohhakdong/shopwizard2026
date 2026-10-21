package com.shopwizard.framework.auth;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 요청 바디의 감사(audit) 필드 — 등록자(registId/registName)와 수정자(changeId/changeName) — 를
 * 로그인한 행위자({@link AuthSession.Actor})의 값으로 강제한다.
 *
 * <p>예전에는 화면이 보낸 값을 서버가 그대로 DB 와 상태 이력에 기록해서, 로그인한 사용자가 바디만 바꾸면
 * 다른 사람 이름으로 주문 취소·환불확정·상품 등록 등을 남길 수 있었다.</p>
 *
 * <p>규칙</p>
 * <ul>
 *   <li>최상위 본문: POST 는 등록자 계열, PUT/PATCH/DELETE 는 수정자 계열을 <b>항상</b> 세션 값으로 설정한다
 *       (화면이 빠뜨려도 채워진다). 반대 계열은 클라이언트가 보낸 경우에만 덮어쓴다 — 배치 게이트가 POST 로
 *       registId 를 행위자로 쓰는 것, 엔티티 PUT 이 changeId 를 쓰는 것 모두 그대로 동작하고 위조 값만 사라진다.</li>
 *   <li>Map / 빈 / 컬렉션·배열(재귀, 깊이 제한). 컬렉션 안이나 중첩 객체는 값이 이미 들어 있는 필드만 덮어쓴다
 *       (없던 필드를 만들어 모양을 바꾸지 않는다). 최상위가 컬렉션이면 각 원소를 최상위로 취급한다.</li>
 *   <li>빈은 {@code String} 타입의 writable 프로퍼티만 건드린다.</li>
 * </ul>
 */
public final class AuditFields {

    private static final List<String> REGIST = List.of("registId", "registName");
    private static final List<String> CHANGE = List.of("changeId", "changeName");
    private static final int MAX_DEPTH = 4;

    private AuditFields() {}

    public static void apply(Object body, AuthSession.Actor actor, String httpMethod) {
        if (body == null || actor == null) return;
        boolean change = "PUT".equalsIgnoreCase(httpMethod) || "PATCH".equalsIgnoreCase(httpMethod)
                || "DELETE".equalsIgnoreCase(httpMethod);
        String id = actor.loginId() == null ? "" : actor.loginId();
        String name = actor.name() == null ? "" : actor.name();
        walk(body, id, name, change ? CHANGE : REGIST, change ? REGIST : CHANGE, true, 0);
    }

    @SuppressWarnings("unchecked")
    private static void walk(Object node, String id, String name, List<String> primary, List<String> secondary,
                             boolean top, int depth) {
        if (node == null || depth > MAX_DEPTH) return;

        if (node instanceof Map<?, ?> map) {
            Map<Object, Object> m = (Map<Object, Object>) map;
            try {
                put(m, primary, id, name, top);
                put(m, secondary, id, name, false);
            } catch (UnsupportedOperationException ignored) {
                return;   // 불변 맵은 건드리지 않는다(요청 바디는 보통 LinkedHashMap)
            }
            for (Object v : m.values()) {
                if (isContainer(v) || isBean(v)) walk(v, id, name, primary, secondary, false, depth + 1);
            }
            return;
        }
        if (node instanceof Collection<?> c) {
            for (Object e : c) walk(e, id, name, primary, secondary, top, depth + 1);
            return;
        }
        if (node instanceof Object[] arr) {
            for (Object e : arr) walk(e, id, name, primary, secondary, top, depth + 1);
            return;
        }
        if (!isBean(node)) return;

        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(node);
        set(bw, primary, id, name, top);
        set(bw, secondary, id, name, false);
        for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
            if (pd.getReadMethod() == null || "class".equals(pd.getName())) continue;
            Object v;
            try {
                v = bw.getPropertyValue(pd.getName());
            } catch (RuntimeException e) {
                continue;
            }
            if (isContainer(v) || isBean(v)) walk(v, id, name, primary, secondary, false, depth + 1);
        }
    }

    /** always=true 면 키가 없어도 넣고, false 면 이미 키가 있을 때만 덮어쓴다. */
    private static void put(Map<Object, Object> m, List<String> keys, String id, String name, boolean always) {
        if (always || m.containsKey(keys.get(0))) m.put(keys.get(0), id);
        if (always || m.containsKey(keys.get(1))) m.put(keys.get(1), name);
    }

    private static void set(BeanWrapper bw, List<String> props, String id, String name, boolean always) {
        setOne(bw, props.get(0), id, always);
        setOne(bw, props.get(1), name, always);
    }

    private static void setOne(BeanWrapper bw, String prop, String value, boolean always) {
        if (!bw.isWritableProperty(prop) || bw.getPropertyType(prop) != String.class) return;
        if (!always) {
            // 값이 있는(클라이언트가 보낸) 필드만 덮어쓴다
            if (!bw.isReadableProperty(prop) || bw.getPropertyValue(prop) == null) return;
        }
        bw.setPropertyValue(prop, value);
    }

    private static boolean isContainer(Object v) {
        return v instanceof Map || v instanceof Collection || v instanceof Object[];
    }

    /** 이 프로젝트의 모델(요청 DTO/엔티티)만 빈으로 취급한다 — JDK 타입·열거형은 제외. */
    private static boolean isBean(Object v) {
        return v != null && v.getClass().getName().startsWith("com.shopwizard.");
    }
}
