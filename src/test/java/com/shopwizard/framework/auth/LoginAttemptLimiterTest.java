package com.shopwizard.framework.auth;

import com.shopwizard.framework.auth.AuthSession.Type;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    // 3회 실패 → 1분 잠금, 실패 집계 창 2분
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, 120_000, 60_000, now::get);

    @Test
    void 연속_실패가_한도에_도달하면_잠긴다() {
        String key = LoginAttemptLimiter.key(Type.MNGR, "admin", "1.1.1.1");

        limiter.recordFailure(key);
        limiter.recordFailure(key);
        assertFalse(limiter.isLocked(key));

        limiter.recordFailure(key);
        assertTrue(limiter.isLocked(key));
    }

    @Test
    void 잠금은_시간이_지나면_풀린다() {
        String key = LoginAttemptLimiter.key(Type.MNGR, "admin", "1.1.1.1");
        for (int i = 0; i < 3; i++) limiter.recordFailure(key);
        assertTrue(limiter.isLocked(key));

        now.addAndGet(60_001);
        assertFalse(limiter.isLocked(key));
    }

    @Test
    void 성공하면_실패_횟수가_초기화된다() {
        String key = LoginAttemptLimiter.key(Type.MNGR, "admin", "1.1.1.1");
        limiter.recordFailure(key);
        limiter.recordFailure(key);
        limiter.recordSuccess(key);

        limiter.recordFailure(key);
        limiter.recordFailure(key);
        assertFalse(limiter.isLocked(key));
    }

    @Test
    void 집계_창이_지나면_이전_실패는_잊는다() {
        String key = LoginAttemptLimiter.key(Type.MNGR, "admin", "1.1.1.1");
        limiter.recordFailure(key);
        limiter.recordFailure(key);

        now.addAndGet(120_001);
        limiter.recordFailure(key);
        assertFalse(limiter.isLocked(key));
    }

    @Test
    void 주체_아이디_IP가_다르면_서로_영향이_없다() {
        String a = LoginAttemptLimiter.key(Type.MNGR, "admin", "1.1.1.1");
        for (int i = 0; i < 3; i++) limiter.recordFailure(a);

        assertTrue(limiter.isLocked(a));
        assertFalse(limiter.isLocked(LoginAttemptLimiter.key(Type.CUST, "admin", "1.1.1.1")));
        assertFalse(limiter.isLocked(LoginAttemptLimiter.key(Type.MNGR, "other", "1.1.1.1")));
        assertFalse(limiter.isLocked(LoginAttemptLimiter.key(Type.MNGR, "admin", "2.2.2.2")));
    }

    @Test
    void 키는_아이디의_대소문자와_공백을_정규화한다() {
        assertEquals(
                LoginAttemptLimiter.key(Type.MNGR, "Admin ", "1.1.1.1"),
                LoginAttemptLimiter.key(Type.MNGR, "admin", "1.1.1.1"));
    }
}
