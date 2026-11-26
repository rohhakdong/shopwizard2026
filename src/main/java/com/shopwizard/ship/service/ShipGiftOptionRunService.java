package com.shopwizard.ship.service;

import com.shopwizard.framework.util.StringUtil;
import com.shopwizard.ship.mapper.GiftOptionUpdateDetailMapper;
import com.shopwizard.ship.mapper.GiftOptionUpdateMapper;
import com.shopwizard.ship.mapper.ShipPrintMapper;
import com.shopwizard.ship.model.GiftOptionUpdate;
import com.shopwizard.ship.model.GiftOptionUpdateDetail;
import com.shopwizard.ship.model.ShipDirect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 발주내역2생성 — 발주서(tShpShipDirectPrint) 행에 ① 샵링커 채널상품번호(ProdMemo01) ② 사은품 문자열(ItemName)·옵션금액(NvPrice)을
 * 다시 채우는 작업. 레거시 {@code orderGiftOptionUpdate.jsp} 의 이식이다.
 *
 * <p>레거시와 달라진 점(의도적): 대상 상점을 고른다(레거시는 한 상점 코드를 박아 둠) · 오래된 '진행' 실행은 실패로 돌려 새 실행을 막지 않게 한다
 * · 주문마다 별도 트랜잭션이고 예외가 난 주문도 상세에 '오류' 로 남긴다 · 같은 주문을 한 번만 처리한다 · 등록자를 기록한다 ·
 * 쓸 게 없거나 속성명(ItemName)에 이미 값이 있는 주문은 '무시' 로 남긴다(레거시는 쓸 게 없어도 '성공' 이었고, 속성명을 사은품 문자열로 덮어썼다).
 */
@Slf4j
@Service
public class ShipGiftOptionRunService {

    /** 진행 중인 채 이 시간이 지나면 중단된 실행으로 본다. */
    static final int STALE_MINUTES = 30;
    /** 한 번에 처리할 기간 상한(일). */
    static final int MAX_DAYS = 31;
    /** 응답에 싣는 상세(오류·무시) 상한 — 전체 기록은 상세 테이블에 있다. */
    static final int MAX_DETAIL_LINES = 200;
    /** 사은품 문자열을 쓴 행의 Remark — 다음 실행에서 "우리가 쓴 값" 임을 알아보는 표시. */
    static final String MARK = "사은품 문자열 생성";
    private static final int MSG_MAX = 200;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GiftOptionUpdateMapper runMapper;
    private final GiftOptionUpdateDetailMapper detailMapper;
    private final ShipPrintMapper printMapper;
    private final TransactionTemplate requiresNew;
    private final ReentrantLock lock = new ReentrantLock();

    public ShipGiftOptionRunService(GiftOptionUpdateMapper runMapper, GiftOptionUpdateDetailMapper detailMapper,
                                    ShipPrintMapper printMapper, PlatformTransactionManager txManager) {
        this.runMapper = runMapper;
        this.detailMapper = detailMapper;
        this.printMapper = printMapper;
        this.requiresNew = new TransactionTemplate(txManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 상세 한 줄. */
    public record Line(int no, Integer orderNo, String resultType, String message) {}

    /** 실행 결과. details 는 오류·무시만(최대 {@value #MAX_DETAIL_LINES}건). */
    public record Result(Integer updateId, String shopCode, String resultType, String resultMessag,
                         int total, int ignore, int error, int success, List<Line> details) {}

    /** 주문 한 건에 쓸 값을 계산한 결과(순수 함수의 반환). */
    record Plan(String gift, int optionAmt, boolean hasLine, boolean writable) {
        boolean nothingToWrite() { return gift.isEmpty() && optionAmt == 0; }
    }

    public Result run(String shopCode, String startDate, String endDate, String actorId, String actorName) {
        String shop = ShipPrintService.requireShop(shopCode);
        String[] range = range(startDate, endDate);
        if (printMapper.selectShopName(shop) == null) throw new ResponseStatusException(BAD_REQUEST, "상점을 찾을 수 없습니다.");

        if (!lock.tryLock()) throw running();
        try {
            tx(() -> runMapper.failStale(STALE_MINUTES));
            Map<String, Object> q = new HashMap<>();
            q.put("pResultType", "진행");
            if (runMapper.selectCount(q) > 0) throw running();

            GiftOptionUpdate run = new GiftOptionUpdate();
            run.setStartDate(range[0]);
            run.setEndDate(range[1]);
            run.setResultType("진행");
            run.setResultMessag("");
            run.setState(0);
            run.setRemark(shop);
            run.setRegistId(actorId);
            run.setRegistName(actorName);
            tx(() -> runMapper.insert(run));
            return execute(run, shop, range, actorId, actorName);
        } finally {
            lock.unlock();
        }
    }

    /** 이 상점의 최근 실행 이력(최신순). 이 도구가 남긴 실행만 — 실행 기록의 Remark 에 상점 코드를 적어 둔다. */
    public List<GiftOptionUpdate> history(String shopCode, int limit) {
        Map<String, Object> q = new HashMap<>();
        q.put("pRemark", ShipPrintService.requireShop(shopCode));
        q.put("pPageOffset", 0);
        q.put("pPageSize", Math.max(1, Math.min(limit, 100)));
        return runMapper.selectList(q);
    }

    /** 한 실행의 주문별 상세. 다른 상점의 실행이면 404. */
    public List<GiftOptionUpdateDetail> details(String shopCode, Integer updateId, int limit) {
        String shop = ShipPrintService.requireShop(shopCode);
        Map<String, Object> key = new HashMap<>();
        key.put("updateId", updateId);
        GiftOptionUpdate run = updateId == null ? null : runMapper.select(key);
        if (run == null || !shop.equals(run.getRemark())) throw new ResponseStatusException(NOT_FOUND, "실행 기록을 찾을 수 없습니다.");
        Map<String, Object> q = new HashMap<>();
        q.put("pUpdateId", updateId);
        q.put("pPageOffset", 0);
        q.put("pPageSize", Math.max(1, Math.min(limit, 1000)));
        return detailMapper.selectList(q);
    }

    private Result execute(GiftOptionUpdate run, String shop, String[] range, String actorId, String actorName) {
        int updateId = run.getUpdateId();
        String startedAt = LocalDateTime.now().format(STAMP);
        int ignore = 0;
        int error = 0;
        int success = 0;
        int total = 0;
        List<Line> details = new ArrayList<>();
        try {
            tx(() -> detailMapper.updatePrintAuctionnoByShop(shop, range[0], range[1]));
            List<Integer> orderNos = detailMapper.selectRunOrderNos(shop, range[0], range[1]);
            total = orderNos.size();
            int no = 0;
            for (Integer orderNo : orderNos) {
                Line line = processOrder(updateId, no++, orderNo, shop, actorId, actorName);
                switch (line.resultType()) {
                    case "성공" -> success++;
                    case "무시" -> ignore++;
                    default -> error++;
                }
                if (!"성공".equals(line.resultType()) && details.size() < MAX_DETAIL_LINES) details.add(line);
            }
            String message = "처리를 완료하였습니다. 무시 - " + ignore + "건, 중복 - 0건, 오류 - " + error + "건, 성공 - " + success + "건";
            finish(run, "성공", message, startedAt, actorId, actorName);
            return new Result(updateId, shop, "성공", message, total, ignore, error, success, details);
        } catch (RuntimeException e) {
            log.error("발주내역2생성 실패(updateId={}, shop={})", updateId, shop, e);
            String message = cut("처리 중 오류로 중단되었습니다: " + e.getMessage());
            finish(run, "실패", message, startedAt, actorId, actorName);
            return new Result(updateId, shop, "실패", message, total, ignore, error, success, details);
        }
    }

    /** 주문 한 건 처리 — 쓰기와 상세 기록은 한 트랜잭션, 예외가 나면 롤백하고 오류 상세만 따로 남긴다. */
    private Line processOrder(int updateId, int no, Integer orderNo, String shop, String actorId, String actorName) {
        try {
            return requiresNew.execute(status -> {
                Line line = apply(orderNo, no, shop, actorId, actorName);
                insertDetail(updateId, no, orderNo, line.resultType(), line.message(), actorId, actorName);
                return line;
            });
        } catch (RuntimeException e) {
            log.warn("발주내역2생성 주문 처리 오류(orderNo={}): {}", orderNo, e.toString());
            String msg = cut("시스템오류 : " + e.getMessage());
            try {
                tx(() -> insertDetail(updateId, no, orderNo, "오류", msg, actorId, actorName));
            } catch (RuntimeException inner) {
                log.error("발주내역2생성 오류 상세 기록 실패(orderNo={})", orderNo, inner);
            }
            return new Line(no, orderNo, "오류", msg);
        }
    }

    private Line apply(Integer orderNo, int no, String shop, String actorId, String actorName) {
        Map<String, Object> q = new HashMap<>();
        q.put("orderNo", orderNo);
        q.put("shopCode", shop);
        List<ShipDirect> rows = detailMapper.selectShipDirectPrintList(q);
        Plan plan = plan(rows);
        if (!plan.hasLine()) return new Line(no, orderNo, "오류", "발주내역이 없습니다.");
        if (plan.nothingToWrite()) return new Line(no, orderNo, "무시", "사은품도 옵션금액도 없어 쓰지 않았습니다.");
        if (!plan.writable()) return new Line(no, orderNo, "무시", "속성명(ItemName)에 이미 값이 있어 사은품 문자열을 쓰지 않았습니다.");
        Map<String, Object> w = new HashMap<>();
        w.put("orderNo", orderNo);
        w.put("shopCode", shop);
        w.put("gift", plan.gift());
        w.put("optionAmt", plan.optionAmt());
        w.put("changeId", actorId);
        w.put("changeName", actorName);
        detailMapper.updateShipDirectPrint(w);
        return new Line(no, orderNo, "성공", "처리에 성공하였습니다.");
    }

    /**
     * 주문 한 건의 발주서 행으로 쓸 값을 계산한다(순수 함수).
     * 사은품 문자열 = 사은품 행("힣" 으로 시작하는 레거시 행 또는 OrderType 사은품)의 "상품명-수량" 을 쉼표로 이은 것.
     * 옵션금액 = 첫 상품 행(OrderProdNo 1, 사은품 아님)의 주문 원본 옵션 문구에서 "(+1,000원)" 형태를 읽은 값
     * (11번가 채널은 "1,000원 2개" 형태의 합계를 더한다). 쓰기는 상품 행(OrderProdNo 1)의 속성명이 비어 있거나
     * 이 작업이 쓴 값일 때만 — 주문 옵션명이 들어 있는 속성명을 덮어쓰지 않는다.
     */
    static Plan plan(List<ShipDirect> rows) {
        if (rows == null || rows.isEmpty()) return new Plan("", 0, false, true);
        StringBuilder gift = new StringBuilder();
        int optionAmt = 0;
        boolean optionRead = false;
        boolean writable = true;
        for (ShipDirect r : rows) {
            String name = r.getProdName() == null ? "" : r.getProdName();
            boolean isGift = name.startsWith("힣") || "사은품".equals(r.getOrderType());
            if (isGift) {
                if (gift.length() > 0) gift.append(',');
                gift.append(name).append('-').append(r.getProdQty() == null ? 0 : r.getProdQty());
            }
            if (!isGift && r.getOrderProdNo() != null && r.getOrderProdNo() == 1) {   // 쓰기 대상은 첫 상품 행(사은품 행은 제외)
                String item = r.getItemName() == null ? "" : r.getItemName();
                if (!item.isEmpty() && !MARK.equals(r.getRemark())) writable = false;
                if (!optionRead) {
                    optionRead = true;
                    String orig = r.getOrderItemName() == null ? "" : r.getOrderItemName();
                    optionAmt = StringUtil.stripPrice(orig);
                    if (r.getChnlName() != null && r.getChnlName().contains("11번가")) optionAmt += StringUtil.stripPrices(orig);
                }
            }
        }
        return new Plan(gift.toString(), optionAmt, true, writable);
    }

    private void finish(GiftOptionUpdate run, String type, String message, String startedAt, String actorId, String actorName) {
        GiftOptionUpdate done = new GiftOptionUpdate();
        done.setUpdateId(run.getUpdateId());
        done.setResultType(type);
        done.setResultMessag(message);
        done.setUpdateStartDate(startedAt);
        done.setUpdateEndDate(LocalDateTime.now().format(STAMP));
        done.setChangeId(actorId);
        done.setChangeName(actorName);
        tx(() -> runMapper.update(done));
    }

    private void insertDetail(int updateId, int no, Integer orderNo, String type, String message, String actorId, String actorName) {
        GiftOptionUpdateDetail d = new GiftOptionUpdateDetail();
        d.setUpdateId(updateId);
        d.setUpdateDetailNo(no);
        d.setOrderNo(orderNo);
        d.setResultType(type);
        d.setResultMessag(cut(message));
        d.setState(0);
        d.setRegistId(actorId);
        d.setRegistName(actorName);
        detailMapper.insert(d);
    }

    /** 별도 트랜잭션(REQUIRES_NEW)으로 한 단계를 실행한다 — 단계마다 바로 커밋돼 다른 요청이 '진행' 상태를 볼 수 있다. */
    private void tx(Runnable work) {
        requiresNew.executeWithoutResult(status -> work.run());
    }

    static String[] range(String start, String end) {
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "시작일과 종료일을 입력하세요.");
        }
        try {
            LocalDate s = LocalDate.parse(start.trim());
            LocalDate e = LocalDate.parse(end.trim());
            if (s.isAfter(e)) throw new ResponseStatusException(BAD_REQUEST, "시작일이 종료일보다 늦습니다.");
            if (ChronoUnit.DAYS.between(s, e) >= MAX_DAYS) {
                throw new ResponseStatusException(BAD_REQUEST, "한 번에 " + MAX_DAYS + "일까지 처리할 수 있습니다.");
            }
            return new String[]{s.toString(), e.toString()};
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "날짜는 yyyy-MM-dd 형식으로 입력하세요.");
        }
    }

    private static ResponseStatusException running() {
        return new ResponseStatusException(CONFLICT, "현재 진행되는 건이 있어서 종료합니다. 잠시 뒤에 다시 실행하세요.");
    }

    private static String cut(String s) {
        if (s == null) return "";
        return s.length() <= MSG_MAX ? s : s.substring(0, MSG_MAX);
    }
}
