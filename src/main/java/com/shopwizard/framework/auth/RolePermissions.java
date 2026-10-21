package com.shopwizard.framework.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 한 역할이 사용할 수 있는 화면. 레거시 권한 데이터(tAthMenu/tAthFunctn)를 {@link ScreenRegistry} 의 화면 key 로
 * 번역한 결과다.
 *
 * <ul>
 *   <li>view  : 활성 메뉴의 URI, 또는 활성 메뉴 아래 활성 조회 기능(w…) URI 의 basename 이 화면의 레거시 basename 과 같다.</li>
 *   <li>write : view 를 만들어 준 메뉴 아래에 활성 쓰기 기능(x…Insert|Update|Delete…)이 하나라도 있다.
 *               승인 화면({@link ScreenRegistry#APPROVE})은 메뉴 보유 자체가 승인 권한이다.</li>
 * </ul>
 *
 * <p>역할·메뉴·기능 관리 화면에 모두 쓰기 권한이 있는 역할은 자기 역할에 어떤 권한이든 스스로 부여할 수 있으므로
 * 제한할 실익이 없다 — 모든 화면의 view/write 를 가진 것으로 취급한다(레거시 시스템관리자 역할에는 이후에 새로 생긴
 * 화면의 레거시 메뉴가 없어 그대로 두면 운영 중인 시스템관리자가 일부 화면에서 잠긴다).</p>
 */
public record RolePermissions(Set<String> view, Set<String> write) {

    public static final RolePermissions NONE = new RolePermissions(Set.of(), Set.of());

    private static final Pattern WRITE_FUNCTN = Pattern.compile("^x[A-Za-z0-9]*(Insert|Update|Delete)[A-Za-z0-9]*$");

    /** 이 화면들의 쓰기 권한을 모두 가진 역할은 권한 체계 자체를 바꿀 수 있다. */
    private static final Set<String> PERMISSION_ADMIN_SCREENS = Set.of("auth-role", "auth-menu", "auth-functn");

    /** 사이드바의 모든 화면을 볼 수 있는 역할(시스템 관리자급). 화면이 소유하지 않은 엔드포인트도 통과한다. */
    public boolean full() {
        return ScreenRegistry.SCREENS.stream()
                .map(ScreenRegistry.Screen::key)
                .filter(k -> !k.equals(ScreenRegistry.APPROVE))
                .allMatch(view::contains);
    }

    /** menus: {MenuCode, MenuUri} / functns: {MenuCode, FunctnUri} — 둘 다 State=1 만 넘길 것. */
    public static RolePermissions compute(List<Map<String, Object>> menus, List<Map<String, Object>> functns) {
        Set<String> activeMenuCodes = new HashSet<>();
        Map<String, Set<String>> grantingMenus = new HashMap<>();   // 화면 key → view 를 준 MenuCode 들

        for (Map<String, Object> m : menus) {
            String code = str(m.get("MenuCode"));
            activeMenuCodes.add(code);
            grant(grantingMenus, basename(str(m.get("MenuUri"))), code);
        }
        for (Map<String, Object> f : functns) {
            String code = str(f.get("MenuCode"));
            if (!activeMenuCodes.contains(code)) continue;
            String base = basename(str(f.get("FunctnUri")));
            if (base.startsWith("w")) grant(grantingMenus, base, code);
        }

        Set<String> view = new HashSet<>(grantingMenus.keySet());
        Set<String> write = new HashSet<>();
        for (Map.Entry<String, Set<String>> e : grantingMenus.entrySet()) {
            if (e.getKey().equals(ScreenRegistry.APPROVE)) {
                write.add(e.getKey());
                continue;
            }
            for (Map<String, Object> f : functns) {
                if (e.getValue().contains(str(f.get("MenuCode")))
                        && WRITE_FUNCTN.matcher(basename(str(f.get("FunctnUri")))).matches()) {
                    write.add(e.getKey());
                    break;
                }
            }
        }
        if (write.containsAll(PERMISSION_ADMIN_SCREENS)) {
            Set<String> all = ScreenRegistry.SCREENS.stream().map(ScreenRegistry.Screen::key).collect(Collectors.toUnmodifiableSet());
            return new RolePermissions(all, all);
        }
        return new RolePermissions(Set.copyOf(view), Set.copyOf(write));
    }

    private static void grant(Map<String, Set<String>> grantingMenus, String base, String menuCode) {
        if (base.isEmpty()) return;
        for (ScreenRegistry.Screen s : ScreenRegistry.SCREENS) {
            if (s.bases().contains(base)) {
                grantingMenus.computeIfAbsent(s.key(), k -> new HashSet<>()).add(menuCode);
            }
        }
    }

    /** 메뉴/기능 URI → 확장자·쿼리·앞뒤 공백을 뗀 마지막 구간 (예: wOrdOrderProdList). 데이터에 앞뒤 공백/탭이 섞여 있다. */
    static String basename(String uri) {
        String u = uri == null ? "" : uri.trim();
        int q = u.indexOf('?');
        if (q >= 0) u = u.substring(0, q);
        int slash = u.lastIndexOf('/');
        if (slash >= 0) u = u.substring(slash + 1);
        int dot = u.indexOf('.');
        if (dot >= 0) u = u.substring(0, dot);
        return u.trim();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
