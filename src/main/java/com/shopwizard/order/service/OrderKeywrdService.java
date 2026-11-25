package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderKeywrdMapper;
import com.shopwizard.order.model.OrderKeywrd;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 발주 상품명 키워드 — 레거시 "발주상품명키워드" 화면(wOrdOrderKeywrdList). 자동발주의 "키워드로 대상 가져오기"가 이 키워드로
 * 상점 미지정 주문 라인을 상점에 배정한다: LIKE/LIKE OPTION 은 상품명/옵션에 포함되면 후보, NOT LIKE/NOT LIKE OPTION 은 포함되면 제외.
 * 사용 중(State=1)인 키워드만 쓴다.
 */
@Service
@RequiredArgsConstructor
public class OrderKeywrdService {

    public static final Set<String> SECTS = Set.of("LIKE", "LIKE OPTION", "NOT LIKE", "NOT LIKE OPTION");

    private final OrderKeywrdMapper mapper;

    @Transactional(readOnly = true)
    public List<OrderKeywrd> selectList(Map<String, Object> params) { return mapper.selectList(params); }

    @Transactional(readOnly = true)
    public int selectCount(Map<String, Object> params) { return mapper.selectCount(params); }

    @Transactional(readOnly = true)
    public OrderKeywrd select(Map<String, Object> params) { return mapper.select(params); }

    @Transactional(readOnly = true)
    public OrderKeywrd find(Integer keywrdSeq) {
        if (keywrdSeq == null) return null;
        Map<String, Object> key = new HashMap<>();
        key.put("keywrdSeq", keywrdSeq);
        return mapper.select(key);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderKeywrd create(OrderKeywrd in) {
        OrderKeywrd row = normalize(in);
        check(row, null);
        row.setState(state(in.getState(), 1));
        row.setRegistId(in.getRegistId());
        row.setRegistName(in.getRegistName());
        mapper.insert(row);
        return find(row.getKeywrdSeq());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderKeywrd modify(OrderKeywrd in) {
        OrderKeywrd existing = find(in.getKeywrdSeq());
        if (existing == null) throw bad("키워드를 찾을 수 없습니다.");
        OrderKeywrd row = normalize(in);
        check(row, existing.getKeywrdSeq());
        row.setKeywrdSeq(existing.getKeywrdSeq());
        row.setState(state(in.getState(), existing.getState() == null ? 0 : existing.getState()));
        row.setChangeId(in.getChangeId());
        row.setChangeName(in.getChangeName());
        mapper.update(row);
        return find(existing.getKeywrdSeq());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Integer keywrdSeq) {
        OrderKeywrd existing = find(keywrdSeq);
        if (existing == null) throw bad("키워드를 찾을 수 없습니다.");
        mapper.delete(existing);
    }

    private OrderKeywrd normalize(OrderKeywrd in) {
        OrderKeywrd r = new OrderKeywrd();
        r.setShopCode(required(in.getShopCode(), "상점", 12));
        r.setKeywrdName(required(in.getKeywrdName(), "키워드", 50));
        String sect = in.getKeywrdSect() == null ? "" : in.getKeywrdSect().trim().replaceAll("\\s+", " ").toUpperCase();
        if (!SECTS.contains(sect)) throw bad("구분은 LIKE · LIKE OPTION · NOT LIKE · NOT LIKE OPTION 중 하나여야 합니다.");
        r.setKeywrdSect(sect);
        String remark = in.getRemark() == null ? "" : in.getRemark().trim();
        if (remark.length() > 50) throw bad("비고는 50자 이내여야 합니다.");
        r.setRemark(remark);
        return r;
    }

    private void check(OrderKeywrd r, Integer exceptSeq) {
        if (mapper.countShop(r.getShopCode()) == 0) throw bad("없는 상점입니다: " + r.getShopCode());
        if (mapper.countDuplicate(r.getShopCode(), r.getKeywrdSect(), r.getKeywrdName(), exceptSeq) > 0) {
            throw bad("같은 상점·구분에 이미 있는 키워드입니다: " + r.getKeywrdName());
        }
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

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
