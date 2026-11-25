package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.DeliRstrctZipcodeMapper;
import com.shopwizard.ship.model.DeliRstrctZipcode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 배송제한 우편번호 — 레거시 "배송제한우편번호"(tShpDeliRstrctZipcodeAddr). 구분(예: DeliFeeUpStar, JerrycanLimit)별로 우편번호를 모아 두면
 * 배송제한 규칙이 "이 구분의 우편번호"로 조건을 건다. 우편번호는 하이픈을 뗀 숫자(5~6자리)로만 저장한다 — 규칙 적용이
 * {@code REPLACE(RecverZipcode,'-','')} 와 비교하기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class DeliRstrctZipcodeService {

    static final int MAX_BULK = 20_000;
    private static final Pattern ZIP = Pattern.compile("^[0-9]{5,6}$");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s,;]+");
    private static final Pattern SECT = Pattern.compile("^[A-Za-z0-9가-힣_\\-]{1,50}$");

    private final DeliRstrctZipcodeMapper mapper;

    /** 붙여넣은 텍스트를 읽은 결과 — 인식한 우편번호(중복 제거, 입력 순)와 못 읽은 토큰. */
    public record Parsed(List<String> zipcodes, List<String> invalid, int duplicatedInInput) {}

    /** 일괄 등록 결과. */
    public record BulkResult(String sect, int added, int alreadyExisted, int duplicatedInInput, List<String> invalid) {}

    @Transactional(readOnly = true)
    public List<Map<String, Object>> sects() { return mapper.selectSects(); }

    @Transactional(readOnly = true)
    public List<DeliRstrctZipcode> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public int countBySect(String sect) { return sect == null ? 0 : mapper.countBySect(sect.trim()); }

    /** 줄·쉼표·공백으로 나눠 하이픈을 떼고 5~6자리 숫자만 인정한다(순수 함수). */
    static Parsed parse(String text) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> invalid = new ArrayList<>();
        int dup = 0;
        if (text != null) {
            for (String raw : SEPARATORS.split(text.trim())) {
                if (raw.isEmpty()) continue;
                String z = raw.replace("-", "");
                if (!ZIP.matcher(z).matches()) {
                    if (!invalid.contains(raw)) invalid.add(raw);
                    continue;
                }
                if (!seen.add(z)) dup++;
            }
        }
        return new Parsed(new ArrayList<>(seen), invalid, dup);
    }

    /** 미리보기(저장하지 않음) — 인식 건수와 오류. */
    public Parsed preview(String text) {
        return parse(text);
    }

    @Transactional(rollbackFor = Exception.class)
    public BulkResult bulkAdd(String sectRaw, String text, String registId, String registName) {
        String sect = sect(sectRaw);
        Parsed p = parse(text);
        if (p.zipcodes().isEmpty()) throw bad("등록할 우편번호가 없습니다.");
        if (p.zipcodes().size() > MAX_BULK) throw bad("한 번에 " + String.format("%,d", MAX_BULK) + "건까지 등록할 수 있습니다.");

        Set<String> existing = new LinkedHashSet<>();
        List<String> all = p.zipcodes();
        for (int i = 0; i < all.size(); i += 1000) {   // IN 절이 너무 길어지지 않게 나눠서 조회
            existing.addAll(mapper.selectExisting(sect, all.subList(i, Math.min(all.size(), i + 1000))));
        }
        List<String> fresh = all.stream().filter(z -> !existing.contains(z)).toList();
        for (int i = 0; i < fresh.size(); i += 1000) {
            mapper.insertAll(sect, fresh.subList(i, Math.min(fresh.size(), i + 1000)), registId, registName);
        }
        return new BulkResult(sect, fresh.size(), existing.size(), p.duplicatedInInput(), p.invalid());
    }

    @Transactional(rollbackFor = Exception.class)
    public int remove(String sectRaw, String zipcode) {
        String z = zipcode == null ? "" : zipcode.replace("-", "").trim();
        if (!ZIP.matcher(z).matches()) throw bad("우편번호가 올바르지 않습니다.");
        return mapper.deleteOne(sect(sectRaw), z);
    }

    @Transactional(rollbackFor = Exception.class)
    public int removeSect(String sectRaw) {
        return mapper.deleteSect(sect(sectRaw));
    }

    private static String sect(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) throw bad("구분을 입력하세요.");
        if (!SECT.matcher(s).matches()) throw bad("구분은 영문·숫자·한글·_·- 50자 이내로 입력하세요.");
        return s;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
