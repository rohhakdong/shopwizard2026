package com.shopwizard.framework.auth;

import com.shopwizard.authority.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 역할별 {@link RolePermissions} 조회(짧은 TTL 캐시). 권한 관리 화면에서 메뉴/기능/역할을 바꾸면
 * {@link #invalidate()} 로 즉시 비운다.
 */
@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private static final long TTL_MILLIS = 60_000;

    private record Cached(RolePermissions perms, long expiresAt) {}

    private final RolePermissionMapper mapper;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public RolePermissions get(String roleUid) {
        if (roleUid == null || roleUid.isBlank()) return RolePermissions.NONE;
        long now = System.currentTimeMillis();
        Cached c = cache.get(roleUid);
        if (c != null && c.expiresAt() > now) return c.perms();
        RolePermissions p = RolePermissions.compute(mapper.selectActiveMenus(roleUid), mapper.selectActiveFunctns(roleUid));
        cache.put(roleUid, new Cached(p, now + TTL_MILLIS));
        return p;
    }

    public void invalidate() {
        cache.clear();
    }
}
