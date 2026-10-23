package com.shopwizard.framework.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * 고객·상점·창고 계정 비밀번호 해시. 레거시는 솔트 없는 SHA-1(또는 PHP MD5)이라 DB 가 유출되면 바로 풀린다 —
 * 새로 저장하는 값은 BCrypt 로 하고, 기존 레거시 해시는 로그인에 성공하는 순간 BCrypt 로 다시 저장한다(자동 전환).
 * 관리자 계정({@code MngrService})과 같은 방식이다.
 */
public final class PasswordHasher {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final Pattern BCRYPT_FORMAT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    /** BCrypt 는 앞 72바이트만 쓴다 — 그보다 긴 입력을 조용히 잘라 저장하지 않도록 막는다. */
    public static final int MAX_BYTES = 72;

    private PasswordHasher() {}

    /** 새 비밀번호를 BCrypt 로 해시한다. 비었거나 72바이트를 넘으면 400. */
    public static String hash(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호를 입력하세요.");
        }
        if (raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 너무 깁니다.");
        }
        return ENCODER.encode(raw);
    }

    public static boolean isBcrypt(String stored) {
        return stored != null && BCRYPT_FORMAT.matcher(stored).matches();
    }

    /**
     * 입력값이 저장값과 일치하는가. 저장값이 BCrypt 면 BCrypt 로, 아니면 레거시 해시({@code legacyHashOfInput})와 상수시간 비교한다.
     * 빈 입력은 어떤 경우에도 통과시키지 않는다. 레거시 해시 계산은 BCrypt 경로에서는 호출하지 않도록 supplier 로 받는다.
     */
    public static boolean matches(String raw, String stored, java.util.function.Supplier<String> legacyHashOfInput) {
        if (raw == null || raw.isEmpty() || stored == null || stored.isEmpty()) return false;
        if (isBcrypt(stored)) return ENCODER.matches(raw, stored);
        String legacy = legacyHashOfInput.get();
        return legacy != null
                && MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), legacy.getBytes(StandardCharsets.UTF_8));
    }
}
