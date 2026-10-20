package com.shopwizard.framework.auth;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 로그인 무차별 대입 완화용 인메모리 제한기. 같은 (주체, 아이디, IP) 조합에서 {@code windowMillis} 안에
 * {@code maxFailures}번 틀리면 {@code lockMillis} 동안 잠근다. 비밀번호가 아이디와 같은 관리자 계정이
 * 있어 온라인 추측 공격이 현실적이라 도입했다. 서버를 재시작하면 초기화된다(단일 인스턴스 전제).
 */
@Component
public class LoginAttemptLimiter {

    private static final int PURGE_THRESHOLD = 10_000;

    private static final class Entry {
        int failures;
        long windowStart;
        long lockedUntil;
    }

    private final int maxFailures;
    private final long windowMillis;
    private final long lockMillis;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public LoginAttemptLimiter() {
        this(5, 10 * 60_000L, 5 * 60_000L, System::currentTimeMillis);
    }

    LoginAttemptLimiter(int maxFailures, long windowMillis, long lockMillis, LongSupplier clock) {
        this.maxFailures = maxFailures;
        this.windowMillis = windowMillis;
        this.lockMillis = lockMillis;
        this.clock = clock;
    }

    public static String key(AuthSession.Type type, String loginId, String remoteAddr) {
        return type + "|" + (loginId == null ? "" : loginId.trim().toLowerCase()) + "|" + remoteAddr;
    }

    public boolean isLocked(String key) {
        Entry e = entries.get(key);
        return e != null && e.lockedUntil > clock.getAsLong();
    }

    public void recordFailure(String key) {
        long now = clock.getAsLong();
        if (entries.size() > PURGE_THRESHOLD) purgeExpired(now);
        entries.compute(key, (k, e) -> {
            if (e == null || now - e.windowStart > windowMillis) {
                e = new Entry();
                e.windowStart = now;
            }
            e.failures++;
            if (e.failures >= maxFailures) {
                e.lockedUntil = now + lockMillis;
                e.failures = 0;
                e.windowStart = now;
            }
            return e;
        });
    }

    public void recordSuccess(String key) {
        entries.remove(key);
    }

    private void purgeExpired(long now) {
        entries.entrySet().removeIf(en ->
                en.getValue().lockedUntil <= now && now - en.getValue().windowStart > windowMillis);
    }
}
