package com.shopwizard.framework.auth;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 화면 코드(static/js/pages)가 부르는 API 가 전부 소유표(ScreenRegistry.OWNERSHIP)에 있어야 한다.
 * 새 화면/호출을 추가하고 소유표를 갱신하지 않으면 그 호출이 역할 권한 검사에서 막히거나(쓰기) 소유 없이 열려(읽기) 버리므로
 * 여기서 실패시킨다.
 */
class ScreenRegistryTest {

    private static final Path STATIC = Path.of("src/main/resources/static");
    private static final Pattern CALL = Pattern.compile(
            "Api\\.(get|post|put|delete|upload)\\(\\s*([`'\"])(/[^`'\"]*)\\2(\\s*\\+)?");
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static String normalize(String path) {
        return path.replaceAll("\\$\\{[^}]*}", "*").split("\\?")[0];
    }

    private static boolean owned(String method, String path) {
        for (String[] row : ScreenRegistry.OWNERSHIP) {
            if (row[0].equals(method) && (MATCHER.match(row[1], path) || row[1].equals(path))) return true;
        }
        return false;
    }

    @Test
    void 화면이_부르는_모든_API_가_소유표에_있다() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(STATIC.resolve("js/pages"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".js"))::iterator) {
                String src = Files.readString(f, StandardCharsets.UTF_8);
                Matcher m = CALL.matcher(src);
                while (m.find()) {
                    String verb = m.group(1).toUpperCase();
                    if (verb.equals("UPLOAD")) verb = "POST";
                    String base = normalize(m.group(3));
                    final String path = (m.group(4) != null && base.endsWith("/")) ? base + "*" : base;   // '/x/' + id
                    boolean open = verb.equals("GET")
                            && ScreenRegistry.OPEN_GETS.stream().anyMatch(p -> MATCHER.match(p, path));
                    if (!open && !owned(verb, path)) missing.add(f.getFileName() + ": " + verb + " " + path);
                }
            }
        }
        assertTrue(missing.isEmpty(), "소유표에 없는 호출: " + missing);
    }

    @Test
    void 소유표의_화면_key_는_모두_등록된_화면이다() {
        Set<String> keys = new HashSet<>(ScreenRegistry.BY_KEY.keySet());
        for (String[] row : ScreenRegistry.OWNERSHIP) {
            for (String s : row[2].split(",")) {
                assertTrue(keys.contains(s), "알 수 없는 화면 key: " + s);
            }
        }
    }

    @Test
    void 사이드바_모든_화면이_레지스트리에_있다() throws IOException {
        String html = Files.readString(STATIC.resolve("index.html"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\\{ key: '([a-z-]+)',\\s+label").matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(ScreenRegistry.BY_KEY.containsKey(m.group(1)), "레지스트리에 없는 사이드바 화면: " + m.group(1));
        }
        assertTrue(n >= 30, "사이드바 항목을 읽지 못함: " + n);
    }
}
