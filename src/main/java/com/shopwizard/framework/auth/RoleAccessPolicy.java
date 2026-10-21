package com.shopwizard.framework.auth;

import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * (HTTP 메서드, 경로) 가 역할에게 허용되는지 판정한다. 순수 로직이라 서버 없이 테스트한다.
 *
 * <ol>
 *   <li>열린 조회(공용 드롭다운)는 로그인만 되어 있으면 통과.</li>
 *   <li>화면이 소유한 경로: GET 은 소유 화면 중 하나라도 view, 쓰기는 하나라도 write 면 통과.</li>
 *   <li>어느 화면도 쓰지 않는 경로: full 역할은 통과. GET 은 민감 도메인이면 그 도메인 화면 중 하나라도 view 여야 하고
 *       그 외 도메인은 통과. 쓰기는 거부(화면이 호출하지 않는 쓰기 스캐폴드).</li>
 * </ol>
 */
public final class RoleAccessPolicy {

    public enum Result { ALLOW, DENY }

    public record Decision(Result result, boolean unowned, String reason) {
        public boolean allowed() { return result == Result.ALLOW; }
    }

    private record Entry(String method, String pattern, List<String> screens) {}

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        for (String[] row : ScreenRegistry.OWNERSHIP) {
            ENTRIES.add(new Entry(row[0], row[1], List.of(row[2].split(","))));
        }
    }

    private RoleAccessPolicy() {}

    public static Decision decide(String method, String path, RolePermissions perms) {
        boolean read = method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD");
        String m = read ? "GET" : method.toUpperCase();

        if (read && ScreenRegistry.OPEN_GETS.stream().anyMatch(p -> MATCHER.match(p, path))) {
            return new Decision(Result.ALLOW, false, "open");
        }

        Set<String> owners = new HashSet<>();
        for (Entry e : ENTRIES) {
            if (e.method().equals(m) && MATCHER.match(e.pattern(), path)) owners.addAll(e.screens());
        }
        if (!owners.isEmpty()) {
            Set<String> have = read ? perms.view() : perms.write();
            boolean ok = owners.stream().anyMatch(have::contains);
            return new Decision(ok ? Result.ALLOW : Result.DENY, false,
                    (read ? "view " : "write ") + String.join("|", owners));
        }

        if (perms.full()) return new Decision(Result.ALLOW, true, "unowned/full");
        if (!read) return new Decision(Result.DENY, true, "unowned write");

        String domain = domain(path);
        if (!ScreenRegistry.SENSITIVE_DOMAINS.contains(domain)) {
            return new Decision(Result.ALLOW, true, "unowned/open-domain");
        }
        Set<String> domainScreens = new HashSet<>();
        for (Entry e : ENTRIES) {
            if (domain(e.pattern()).equals(domain)) domainScreens.addAll(e.screens());
        }
        boolean ok = domainScreens.stream().anyMatch(perms.view()::contains);
        return new Decision(ok ? Result.ALLOW : Result.DENY, true, "unowned read in domain " + domain);
    }

    static String domain(String path) {
        String[] seg = path.split("/");
        return seg.length > 1 ? seg[1] : "";
    }
}
