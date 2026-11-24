package com.shopwizard.order.service;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.mapper.OrderConsltMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderConslt;
import com.shopwizard.order.model.OrderConsltRequest;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주문상세의 고객상담 — 레거시 주문상세(orderDetailGridView.jsp)의 "상담내역": 상담 등록과 처리내용(답변) 등록·수정.
 * 삭제는 레거시도 화면에서 막혀 있었고 코드도 오류가 났으므로 두지 않는다(상담 이력은 지워지지 않는다).
 *
 * <p>레거시와 달라진 점: ① 고객 정보·채널은 클라이언트가 아니라 주문 헤더에서 서버가 채운다(고객명·연락처만 입력으로 바꿀 수 있음),
 * ② 상태(State)를 1 로 저장한다(레거시는 항상 0), ③ 처리 수정에서도 처리 제목을 채운다, ④ 상담여부(CustConsltYn)를
 * 상품코드가 아니라 줄 단위로 표시한다.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderConsltService {

    static final int MAX_TEXT = 10_000;
    private static final int TITLE_LEN = 50;

    private final OrderConsltMapper mapper;
    private final OrderService orderService;
    private final OrderProdMapper orderProdMapper;

    /** 한 주문의 상담(최신 먼저). */
    @Transactional(readOnly = true)
    public List<OrderConslt> list(int orderNo) {
        return mapper.selectByOrder(String.valueOf(orderNo));
    }

    /** 상담 한 건(처리 수정 전 소유 주문 확인용). 없으면 null. */
    @Transactional(readOnly = true)
    public OrderConslt find(int custConsltNo) {
        return mapper.selectOne(custConsltNo);
    }

    /** 상담 등록. 등록한 상담을 돌려준다. */
    @Transactional(rollbackFor = Exception.class)
    public OrderConslt register(OrderConsltRequest req, AuthSession.Actor actor) {
        if (req.getOrderNo() == null) throw bad("주문번호가 없습니다.");
        Map<String, Object> key = new HashMap<>();
        key.put("orderNo", req.getOrderNo());
        Order order = orderService.select(key);
        if (order == null) throw bad("주문을 찾을 수 없습니다.");

        String type = required(req.getCustConsltType(), "상담구분을 선택하세요.");
        String reason = required(req.getCustConsltReasonType(), "상담사유를 선택하세요.");
        if (mapper.countActiveCode("cCustConsltType", type) == 0) throw bad("상담구분이 올바르지 않습니다.");
        if (mapper.countActiveCode("cCustConsltReasonType", reason) == 0) throw bad("상담사유가 올바르지 않습니다.");
        String media = trim(req.getConsltMediaType());
        if (media != null && mapper.countActiveCode("cConsltMediaType", media) == 0) throw bad("상담매체가 올바르지 않습니다.");
        String content = required(req.getConsltCntnts(), "상담내용을 입력하세요.");
        if (content.length() > MAX_TEXT) throw bad("상담내용은 " + MAX_TEXT + "자 이내여야 합니다.");
        String answer = trim(req.getAnswrCntnts());
        if (answer != null && answer.length() > MAX_TEXT) throw bad("처리내용은 " + MAX_TEXT + "자 이내여야 합니다.");

        OrderConslt c = new OrderConslt();
        if (req.getOrderProdNo() != null) {
            key.put("orderProdNo", req.getOrderProdNo());
            OrderProd line = orderProdMapper.selectLineRaw(key);
            if (line == null) throw bad("이 주문에 없는 상품 줄입니다.");
            c.setOrderProdNo(String.valueOf(req.getOrderProdNo()));
            c.setProdCode(line.getProdCode());
            c.setItemCode(line.getItemCode());
            c.setItemName(cut(line.getItemName(), 50));
        }
        String managerId = actor == null || actor.loginId() == null ? "SYSTEM" : actor.loginId();
        String managerName = actor == null || actor.name() == null ? managerId : actor.name();

        c.setOrderNo(String.valueOf(req.getOrderNo()));
        c.setCustConsltType(type);
        c.setCustConsltReasonType(reason);
        c.setConsltMediaType(media);
        c.setCustId(order.getCustId());
        c.setCustName(cut(first(req.getCustName(), order.getOrderName()), 50));
        c.setCustMobileNo(cut(first(req.getCustMobileNo(), order.getOrderMobileNo()), 50));
        c.setCustPhoneNo(cut(first(req.getCustPhoneNo(), order.getOrderPhoneNo()), 50));
        c.setCustEmail(cut(order.getOrderEmail(), 50));
        c.setChnlCode(order.getChnlCode());
        c.setConsltTitle(cut(content, TITLE_LEN));
        c.setConsltCntnts(content);
        if (answer != null) {
            c.setAnswrCntnts(answer);
            c.setAnswrTitle(cut(answer, TITLE_LEN));
        }
        c.setMngrId(managerId);
        c.setMngrName(managerName);
        c.setRegistId(managerId);
        c.setRegistName(managerName);
        mapper.insert(c);
        mapper.markConsulted(req.getOrderNo(), req.getOrderProdNo(), managerId, managerName);
        return mapper.selectOne(c.getCustConsltNo());
    }

    /** 처리내용(답변) 등록·수정. 상담구분·사유·내용은 바뀌지 않는다. */
    @Transactional(rollbackFor = Exception.class)
    public OrderConslt answer(OrderConsltRequest req, AuthSession.Actor actor) {
        if (req.getCustConsltNo() == null) throw bad("상담번호가 없습니다.");
        String answer = required(req.getAnswrCntnts(), "처리내용을 입력하세요.");
        if (answer.length() > MAX_TEXT) throw bad("처리내용은 " + MAX_TEXT + "자 이내여야 합니다.");
        if (mapper.selectOne(req.getCustConsltNo()) == null) throw bad("상담을 찾을 수 없습니다.");
        String managerId = actor == null || actor.loginId() == null ? "SYSTEM" : actor.loginId();
        String managerName = actor == null || actor.name() == null ? managerId : actor.name();
        mapper.updateAnswer(req.getCustConsltNo(), cut(answer, TITLE_LEN), answer, managerId, managerName, managerId, managerName);
        return mapper.selectOne(req.getCustConsltNo());
    }

    private static String required(String v, String message) {
        String t = trim(v);
        if (t == null) throw bad(message);
        return t;
    }

    private static String trim(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String first(String preferred, String fallback) {
        String t = trim(preferred);
        return t != null ? t : fallback;
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
