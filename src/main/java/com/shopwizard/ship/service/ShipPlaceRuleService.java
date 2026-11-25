package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipPlaceRuleMapper;
import com.shopwizard.ship.model.ShipPlaceRule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 배송제한 규칙 — 자동발주 "배송 데이터 생성"에서 발주서 행의 출고지(엑셀 시트명)·수취인 표시를 바꾸는 상점별 규칙(레거시 피죤 전용 SQL 의 일반화).
 * 저장만 한다. 상점의 "배송제한 규칙 사용" 옵션이 켜져 있어야 적용된다({@code ShipDataBuildService}).
 */
@Service
@RequiredArgsConstructor
public class ShipPlaceRuleService {

    static final int MAX_ADDR_LINES = 200;
    static final int MAX_EXCEPT_LINES = 50;
    static final int MAX_PATTERN_LEN = 100;
    private static final Pattern SECT = Pattern.compile("^[A-Za-z0-9가-힣_\\-]{1,50}$");
    private static final Pattern SHEET_FORBIDDEN = Pattern.compile("[\\[\\]:*?/\\\\]");

    private final ShipPlaceRuleMapper mapper;

    @Transactional(readOnly = true)
    public List<ShipPlaceRule> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public ShipPlaceRule find(Integer ruleNo) {
        if (ruleNo == null) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("ruleNo", ruleNo);
        return mapper.select(key);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShipPlaceRule create(ShipPlaceRule in) {
        ShipPlaceRule row = normalize(in);
        checkMasters(row);
        row.setState(state(in.getState(), 1));
        row.setRegistId(in.getRegistId());
        row.setRegistName(in.getRegistName());
        mapper.insert(row);
        return find(row.getRuleNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public ShipPlaceRule modify(ShipPlaceRule in) {
        ShipPlaceRule existing = find(in.getRuleNo());
        if (existing == null) throw bad("규칙을 찾을 수 없습니다.");
        ShipPlaceRule row = normalize(in);
        checkMasters(row);
        row.setRuleNo(existing.getRuleNo());
        row.setState(state(in.getState(), existing.getState() == null ? 0 : existing.getState()));
        row.setChangeId(in.getChangeId());
        row.setChangeName(in.getChangeName());
        mapper.update(row);
        return find(existing.getRuleNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Integer ruleNo) {
        ShipPlaceRule existing = find(ruleNo);
        if (existing == null) throw bad("규칙을 찾을 수 없습니다.");
        mapper.delete(existing);
    }

    private ShipPlaceRule normalize(ShipPlaceRule in) {
        ShipPlaceRule r = new ShipPlaceRule();
        r.setShopCode(required(in.getShopCode(), "상점", 12));
        r.setRuleName(optional(in.getRuleName(), "규칙명", 50));
        String sect = optional(in.getZipSect(), "우편번호 구분", 50);
        if (!sect.isEmpty() && !SECT.matcher(sect).matches()) throw bad("우편번호 구분은 영문·숫자·한글·_·- 50자 이내로 입력하세요.");
        r.setZipSect(sect);
        r.setAddrPatterns(lines(in.getAddrPatterns(), "주소 패턴", MAX_ADDR_LINES));
        r.setExceptAddrPatterns(lines(in.getExceptAddrPatterns(), "제외 주소 패턴", MAX_EXCEPT_LINES));
        r.setProdName(optional(in.getProdName(), "상품명(포함)", 50));
        r.setExceptChnlCodes(chnlCodes(in.getExceptChnlCodes()));

        String place = required(in.getShipPlace(), "시트명", 31);   // 엑셀 시트 이름 한계
        if (SHEET_FORBIDDEN.matcher(place).find()) throw bad("시트명에는 [ ] : * ? / \\ 를 쓸 수 없습니다.");
        r.setShipPlace(place);
        r.setNamePrefix(optional(in.getNamePrefix(), "수취인 표시", 10));
        r.setRemark(optional(in.getRemark(), "비고", 50));
        return r;
    }

    private void checkMasters(ShipPlaceRule r) {
        if (mapper.countShop(r.getShopCode()) == 0) throw bad("없는 상점입니다: " + r.getShopCode());
        for (String c : r.getExceptChnlCodes().isEmpty() ? new String[0] : r.getExceptChnlCodes().split(",")) {
            if (mapper.countChnl(c) == 0) throw bad("없는 채널입니다: " + c);
        }
    }

    /** 줄 단위 패턴 — 빈 줄 제거, 중복 제거, 줄당 길이·줄 수 제한. 저장은 줄바꿈(\n)으로 잇는다. */
    static String lines(String text, String label, int maxLines) {
        if (text == null || text.isBlank()) return "";
        Set<String> out = new LinkedHashSet<>();
        for (String line : text.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.length() > MAX_PATTERN_LEN) throw bad(label + "은(는) 한 줄에 " + MAX_PATTERN_LEN + "자 이내여야 합니다: " + t.substring(0, 20) + "…");
            out.add(t);
        }
        if (out.size() > maxLines) throw bad(label + "은(는) 최대 " + maxLines + "줄까지 입력할 수 있습니다.");
        return String.join("\n", out);
    }

    /** 쉼표 구분 채널 코드 — 공백 제거·중복 제거, 12자 이내 코드만. */
    static String chnlCodes(String text) {
        if (text == null || text.isBlank()) return "";
        List<String> out = new ArrayList<>();
        for (String c : text.split("[,\\s]+")) {
            String t = c.trim();
            if (t.isEmpty() || out.contains(t)) continue;
            if (t.length() > 12) throw bad("채널 코드가 올바르지 않습니다: " + t);
            out.add(t);
        }
        return String.join(",", out);
    }

    private static int state(Integer v, int fallback) {
        if (v == null) return fallback;
        if (v != 0 && v != 1) throw bad("사용 여부는 사용(1) 또는 사용 안 함(0) 이어야 합니다.");
        return v;
    }

    private static String required(String v, String label, int max) {
        String t = v == null ? null : v.trim();
        if (t == null || t.isEmpty()) throw bad(label + "을(를) 입력하세요.");
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    private static String optional(String v, String label, int max) {
        String t = v == null ? "" : v.trim();
        if (t.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return t;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
