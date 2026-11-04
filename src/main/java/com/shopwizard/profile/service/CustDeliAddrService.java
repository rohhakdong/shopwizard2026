package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustDeliAddrMapper;
import com.shopwizard.profile.model.CustDeliAddr;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CustDeliAddrService {

    /** 회원 한 명이 저장할 수 있는 배송지 수. */
    public static final int MAX_PER_CUST = 10;

    private static final Pattern PHONE = Pattern.compile("[0-9\\-]{9,15}");
    private static final Pattern ZIPCODE = Pattern.compile("[0-9\\-]{3,10}");

    private final CustDeliAddrMapper custDeliAddrMapper;

    // ── 관리자 화면용(읽기 전용, 기존) ─────────────────────────────────────────────────────────────
    public List<CustDeliAddr> selectList(Map<String, Object> params) { return custDeliAddrMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custDeliAddrMapper.selectCount(params); }
    public CustDeliAddr select(Map<String, Object> params) { return custDeliAddrMapper.select(params); }
    public CustDeliAddr selectDefault(Integer custId) { return custDeliAddrMapper.selectDefault(custId); }

    // ── 고객 본인 배송지 ─────────────────────────────────────────────────────────────────────────
    // custId 는 호출자(컨트롤러)가 서버 세션에서 꺼낸 값이다. 요청 바디의 custId/deliAddrNo 등은 신뢰하지 않고,
    // 모든 쿼리가 CustId 를 함께 조건으로 걸어 다른 회원의 배송지는 건드릴 수 없다.

    @Transactional(readOnly = true)
    public List<CustDeliAddr> selectMine(int custId) {
        return custDeliAddrMapper.selectMine(custId);
    }

    /** 배송지 추가. 첫 배송지는 자동으로 기본이 되고, 기본으로 지정하면 기존 기본은 해제된다. */
    public CustDeliAddr insertMine(int custId, CustDeliAddr in, String actorId, String actorName) {
        CustDeliAddr a = normalize(in);
        custDeliAddrMapper.lockCust(custId);   // 같은 회원의 동시 변경을 직렬화
        int count = custDeliAddrMapper.countMine(custId);
        if (count >= MAX_PER_CUST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송지는 최대 " + MAX_PER_CUST + "개까지 저장할 수 있습니다.");
        }
        boolean makeDefault = count == 0 || Integer.valueOf(1).equals(in.getDefaltYn());
        if (makeDefault) custDeliAddrMapper.clearDefault(custId);

        a.setCustId(custId);
        a.setDefaltYn(makeDefault ? 1 : 0);
        a.setRegistId(blankTo(actorId, "CUST"));
        a.setRegistName(blankTo(actorName, blankTo(actorId, "CUST")));
        custDeliAddrMapper.insertMine(a);
        return a;
    }

    /**
     * 주문서에서 "배송지 목록에 저장"을 켠 경우. 이미 같은 배송지가 있거나, 10개가 찼거나, 값이 저장 규칙에 안 맞으면
     * 조용히 건너뛴다(주문은 이미 끝났으므로 오류를 내지 않는다).
     *
     * @return 새로 저장했으면 true
     */
    public boolean addIfAbsent(int custId, CustDeliAddr in, String actorId, String actorName) {
        CustDeliAddr a;
        try {
            a = normalize(in);
        } catch (ResponseStatusException e) {
            return false;
        }
        custDeliAddrMapper.lockCust(custId);
        if (custDeliAddrMapper.countMine(custId) >= MAX_PER_CUST) return false;
        if (custDeliAddrMapper.findSame(custId, a) != null) return false;
        in.setDefaltYn(0);   // 자동 저장은 기본 배송지를 바꾸지 않는다(첫 배송지면 insertMine 이 기본으로 만든다)
        insertMine(custId, in, actorId, actorName);
        return true;
    }

    /** 배송지 수정(본인 것만). 기본 배송지 지정은 {@link #setDefaultMine} 로 한다. */
    public CustDeliAddr updateMine(int custId, int deliAddrNo, CustDeliAddr in, String actorId, String actorName) {
        CustDeliAddr a = normalize(in);
        custDeliAddrMapper.lockCust(custId);
        a.setCustId(custId);
        a.setDeliAddrNo(deliAddrNo);
        a.setChangeId(blankTo(actorId, "CUST"));
        a.setChangeName(blankTo(actorName, blankTo(actorId, "CUST")));
        if (custDeliAddrMapper.updateMine(a) == 0) {
            throw notFound();
        }
        if (Integer.valueOf(1).equals(in.getDefaltYn())) {
            custDeliAddrMapper.clearDefault(custId);
            custDeliAddrMapper.setDefault(custId, deliAddrNo);
        }
        return custDeliAddrMapper.selectOne(custId, deliAddrNo);
    }

    public void setDefaultMine(int custId, int deliAddrNo) {
        custDeliAddrMapper.lockCust(custId);
        if (custDeliAddrMapper.selectOne(custId, deliAddrNo) == null) throw notFound();
        custDeliAddrMapper.clearDefault(custId);
        custDeliAddrMapper.setDefault(custId, deliAddrNo);
    }

    /** 배송지 삭제(본인 것만). 기본 배송지를 지우면 가장 최근에 저장한 배송지가 기본이 된다. */
    public void deleteMine(int custId, int deliAddrNo) {
        custDeliAddrMapper.lockCust(custId);
        CustDeliAddr row = custDeliAddrMapper.selectOne(custId, deliAddrNo);
        if (row == null) throw notFound();
        custDeliAddrMapper.deleteMine(custId, deliAddrNo);
        if (Integer.valueOf(1).equals(row.getDefaltYn())) {
            custDeliAddrMapper.promoteLatest(custId);
        }
    }

    /** 입력 값을 다듬고 저장 규칙(필수·길이·형식)을 검사한다. 어긋나면 400. */
    static CustDeliAddr normalize(CustDeliAddr in) {
        if (in == null) throw bad("배송지 정보가 없습니다.");
        CustDeliAddr a = new CustDeliAddr();
        a.setRecverName(trim(in.getRecverName()));
        a.setMobilePhoneNo(trim(in.getMobilePhoneNo()));
        a.setZipcode(trim(in.getZipcode()));
        a.setAddr1(trim(in.getAddr1()));
        a.setAddr2(trim(in.getAddr2()));
        a.setRemark(trim(in.getRemark()));

        if (a.getRecverName().isEmpty()) throw bad("수취인 이름을 입력해 주세요.");
        if (a.getRecverName().length() > 50) throw bad("수취인 이름은 50자 이내로 입력해 주세요.");
        if (!PHONE.matcher(a.getMobilePhoneNo()).matches()) throw bad("연락처는 숫자와 하이픈(-)만 입력해 주세요.");
        if (!ZIPCODE.matcher(a.getZipcode()).matches()) throw bad("우편번호를 확인해 주세요. 주소 검색으로 입력합니다.");
        if (a.getAddr1().isEmpty()) throw bad("주소를 입력해 주세요. 주소 검색으로 입력합니다.");
        if (a.getAddr1().length() > 255) throw bad("주소가 너무 깁니다.");
        if (a.getAddr2().length() > 255) throw bad("상세 주소는 255자 이내로 입력해 주세요.");
        if (a.getRemark().length() > 50) throw bad("배송지 이름은 50자 이내로 입력해 주세요.");
        // 배송지 이름은 선택 — 비어 있으면 NULL 로 저장한다.
        if (a.getRemark().isEmpty()) a.setRemark(null);
        return a;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankTo(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "배송지를 찾을 수 없습니다.");
    }
}
