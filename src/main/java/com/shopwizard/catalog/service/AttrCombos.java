package com.shopwizard.catalog.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 속성값1~4(쉼표로 구분한 값 목록)에서 옵션 조합을 만든다 — 레거시 {@code ProdServiceImpl#setProdInfo} 규칙 그대로:
 * 속성값1 의 값마다 속성값2 의 값마다 … 모든 조합(곱)이 옵션 한 행이 된다. 예) 사이즈 "S,M" × 색상 "빨강,파랑" → 4개.
 *
 * <p>레거시와 같이 <b>앞 단계가 비면 그 뒤 단계는 무시</b>한다(속성값2 가 비어 있으면 속성값3·4 는 안 쓴다).
 * 레거시에 없던 안전장치로 값은 앞뒤 공백을 지우고 빈 값·중복은 버리며, 조합이 {@link #MAX_COMBOS} 개를 넘으면 만들지 않는다.</p>
 */
public final class AttrCombos {

    public static final int MAX_COMBOS = 100;

    private AttrCombos() {}

    /** 한 단계의 값 목록 — 쉼표(, ，)로 나누고 공백·빈 값·중복 제거. */
    static List<String> tokens(String raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw != null) {
            for (String t : raw.split("[,，]")) {
                String v = t.trim();
                if (!v.isEmpty()) out.add(v);
            }
        }
        return new ArrayList<>(out);
    }

    /** 앞에서부터 값이 이어지는 단계들(비어 있는 단계에서 멈춘다). */
    static List<List<String>> levels(String... raw) {
        List<List<String>> levels = new ArrayList<>();
        for (String r : raw) {
            List<String> t = tokens(r);
            if (t.isEmpty()) break;
            levels.add(t);
        }
        return levels;
    }

    /** 만들어질 조합 수. 값이 하나도 없으면 0. */
    public static int count(String... raw) {
        List<List<String>> levels = levels(raw);
        if (levels.isEmpty()) return 0;
        long n = 1;
        for (List<String> l : levels) {
            n *= l.size();
            if (n > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) n;
    }

    /** 앞 단계가 비어 있어서 무시되는, 값이 들어 있는 뒤 단계 번호(1~4). */
    public static List<Integer> ignoredLevels(String... raw) {
        int used = levels(raw).size();
        List<Integer> ignored = new ArrayList<>();
        for (int i = used + 1; i < raw.length; i++) {   // used 번째까지 쓰이고, 그 다음 단계는 비어 있어서 멈췄다
            if (!tokens(raw[i]).isEmpty()) ignored.add(i + 1);
        }
        return ignored;
    }

    /**
     * 모든 조합. 각 원소는 {속성값1, 속성값2, 속성값3, 속성값4}(안 쓰는 단계는 빈 문자열).
     * 레거시의 중첩 반복문과 같은 순서(앞 단계가 바깥 반복)이며, {@link #MAX_COMBOS} 를 넘으면 빈 목록을 돌려준다.
     */
    public static List<String[]> expand(String... raw) {
        List<List<String>> levels = levels(raw);
        int n = count(raw);
        if (n == 0 || n > MAX_COMBOS) return List.of();
        List<String[]> out = new ArrayList<>(n);
        build(levels, 0, new String[4], out);
        return out;
    }

    private static void build(List<List<String>> levels, int depth, String[] cur, List<String[]> out) {
        if (depth == levels.size()) {
            String[] row = new String[4];
            for (int i = 0; i < 4; i++) row[i] = i < depth ? cur[i] : "";
            out.add(row);
            return;
        }
        for (String v : levels.get(depth)) {
            cur[depth] = v;
            build(levels, depth + 1, cur, out);
        }
    }
}
