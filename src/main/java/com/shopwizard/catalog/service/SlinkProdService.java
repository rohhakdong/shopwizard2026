package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdExcelMapper;
import com.shopwizard.catalog.mapper.SlinkProdMapper;
import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.SlinkSendResult;
import com.shopwizard.external.shoplinker.ShoplinkerHttp;
import com.shopwizard.external.shoplinker.ShoplinkerProductRequest;
import com.shopwizard.external.shoplinker.ShoplinkerProductResponse;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.product.mapper.SlinkLogMapper;
import com.shopwizard.product.model.SlinkLog;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 승인(게시)된 샵피온 상품을 샵링커에 등록/수정한다(수동 전송). 레거시 {@code ProdServiceImpl#runInsertSlnkProd} 이식.
 *
 * <p>샵링커 호출은 <b>운영 샵링커에 실제로 상품이 등록·수정되는 외부 연동</b>이라 기본 모드는 {@code dry-run}
 * (요청만 만들어 이력에 남기고 보내지 않는다)이다. {@code live} 는 계정 ID 가 설정돼 있어야만 동작한다.
 * 한 번에 하나만 실행하고(락), 한 요청에 최대 {@value #MAX_PRODUCTS}건까지만 받는다.</p>
 */
@Service
public class SlinkProdService {

    private static final Logger log = LoggerFactory.getLogger(SlinkProdService.class);

    public static final int MAX_PRODUCTS = 50;
    /** 상품설명까지 GET 쿼리에 실리는 호출이라 길이를 제한한다. 레거시 성공 호출의 최대 길이는 7,002자였다(Apache 기본 한도 8,190). */
    public static final int MAX_URL_LENGTH = 8000;
    private static final int RESULT_MESSAGE_MAX = 255;   // tPrdSlinkLog.ResultMessag varchar(255)

    private final ProdService prodService;
    private final ProdExcelMapper excelMapper;
    private final SlinkProdMapper slinkProdMapper;
    private final SlinkLogMapper slinkLogMapper;
    private final DataScopeGuard scopeGuard;
    private final ShoplinkerHttp http;

    private final String productUrl;
    private final String mode;
    private final String customerId;
    private final String imageBaseUrl;
    private final String svcCode;
    private final int timeoutSec;

    /** 동시에 두 번 전송하지 않는다(단일 인스턴스 배포 전제 — 다중 인스턴스가 되면 DB 락으로 바꿀 것). */
    private final ReentrantLock sendLock = new ReentrantLock();

    public SlinkProdService(ProdService prodService, ProdExcelMapper excelMapper, SlinkProdMapper slinkProdMapper,
                            SlinkLogMapper slinkLogMapper, DataScopeGuard scopeGuard, ShoplinkerHttp http,
                            @Value("${shoplinker.product-url}") String productUrl,
                            @Value("${shoplinker.product-mode:dry-run}") String mode,
                            @Value("${shoplinker.customer-id:}") String customerId,
                            @Value("${shoplinker.image-base-url:http://www.shopion.co.kr}") String imageBaseUrl,
                            @Value("${shoplinker.product-svc-code:SHP001}") String svcCode,
                            @Value("${shoplinker.product-timeout-sec:30}") int timeoutSec) {
        this.prodService = prodService;
        this.excelMapper = excelMapper;
        this.slinkProdMapper = slinkProdMapper;
        this.slinkLogMapper = slinkLogMapper;
        this.scopeGuard = scopeGuard;
        this.http = http;
        this.productUrl = productUrl;
        this.mode = "live".equalsIgnoreCase(mode == null ? "" : mode.trim()) ? "live" : "dry-run";   // 모르는 값은 안전한 쪽
        this.customerId = customerId == null ? "" : customerId.trim();
        this.imageBaseUrl = imageBaseUrl;
        this.svcCode = svcCode;
        this.timeoutSec = timeoutSec;
    }

    public String mode() { return mode; }
    public boolean customerIdConfigured() { return !customerId.isEmpty(); }
    public String svcCode() { return svcCode; }

    public List<SlinkLog> logs(String prodCode, int limit) {
        return slinkProdMapper.selectProdLogs(prodCode, Math.max(1, Math.min(limit, 100)));
    }

    public SlinkSendResult send(List<String> prodCodes, HttpServletRequest request) {
        if (prodCodes == null || prodCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전송할 상품을 선택하세요.");
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String c : prodCodes) if (c != null && !c.isBlank()) codes.add(c.trim());
        if (codes.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전송할 상품을 선택하세요.");
        if (codes.size() > MAX_PRODUCTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 번에 최대 " + MAX_PRODUCTS + "건까지 전송할 수 있습니다.");
        }
        if (isLive() && customerId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "샵링커 계정 ID(SHOPLINKER_CUSTOMER_ID)가 설정되지 않아 실제 전송을 할 수 없습니다.");
        }
        if (!sendLock.tryLock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 진행 중인 샵링커 전송이 있습니다. 완료 후 다시 시도하세요.");
        }
        try {
            SlinkSendResult result = new SlinkSendResult();
            result.setMode(mode);
            AuthSession.Actor actor = AuthSession.actor(request);
            for (String code : codes) result.getResults().add(sendOne(code, actor, request));
            return result;
        } finally {
            sendLock.unlock();
        }
    }

    private boolean isLive() { return "live".equals(mode); }

    private SlinkSendResult.Row sendOne(String code, AuthSession.Actor actor, HttpServletRequest request) {
        SlinkSendResult.Row row = new SlinkSendResult.Row(code);
        try {
            scopeGuard.requireCatProd(request, code);
        } catch (RuntimeException e) {
            return skip(row, "권한이 없는 상품입니다.");
        }
        Prod p = prodService.select(code);
        if (p == null) return skip(row, "존재하지 않는 상품입니다.");
        row.setProdName(p.getProdName());

        if (!prodService.isPublished(code)) return skip(row, "승인(게시)되지 않은 상품입니다. 승인한 뒤 전송하세요.");
        String shopSvc = p.getShopCode() == null ? null : excelMapper.selectShopSvc(p.getShopCode());
        if (!svcCode.equals(shopSvc)) return skip(row, "샵링커 전송 대상 서비스(" + svcCode + ")의 상품이 아닙니다.");
        List<String> errors = ShoplinkerProductRequest.validate(p);
        if (!errors.isEmpty()) return skip(row, String.join(" ", errors));

        // 재고: 게시된 상품은 주문으로 차감되는 판매 상품의 재고를 보낸다(없음/-1000 이면 무제한 표시 10000 — 레거시 규칙).
        Integer stock = slinkProdMapper.selectLiveSupplyQty(code);
        int quantity = stock == null || stock == ShoplinkerProductRequest.UNLIMITED_MARK ? ShoplinkerProductRequest.UNLIMITED_QTY : stock;

        ShoplinkerProductRequest.Built built = ShoplinkerProductRequest.build(p, prodService.selectProdDesc(code), quantity,
                new ShoplinkerProductRequest.Config(productUrl, customerId, imageBaseUrl));
        if (built.url().length() > MAX_URL_LENGTH) {
            return skip(row, "요청이 너무 깁니다(" + built.url().length() + "자, 한도 " + MAX_URL_LENGTH + "자). 상품설명을 줄여 주세요.");
        }
        String warn = built.warnings().isEmpty() ? "" : " [주의] " + String.join(" ", built.warnings());

        if (!isLive()) {
            row.setStatus(SlinkSendResult.DRY);
            row.setMessage("시험 모드: 샵링커로 보내지 않았습니다(요청 " + built.url().length() + "자)." + warn);
            record(p, "dry", row.getMessage(), built.logUrl(), actor);
            return row;
        }

        String resultType;
        try {
            ShoplinkerProductResponse resp = ShoplinkerProductResponse.parse(http.get(built.url(), timeoutSec));
            row.setStatus(resp.ok() ? SlinkSendResult.SENT : SlinkSendResult.FAILED);
            row.setMessage(resp.message() + warn);
            resultType = resp.ok() ? "true" : "false";
        } catch (Exception e) {
            log.warn("샵링커 상품 전송 오류 prodCode={}: {}", code, e.toString());
            row.setStatus(SlinkSendResult.FAILED);
            row.setMessage("전송 오류: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " - " + e.getMessage()) + warn);
            resultType = "error";
        }
        record(p, resultType, row.getMessage(), built.logUrl(), actor);
        return row;
    }

    private static SlinkSendResult.Row skip(SlinkSendResult.Row row, String message) {
        row.setStatus(SlinkSendResult.SKIPPED);
        row.setMessage(message);
        return row;
    }

    /** 전송 이력 한 건 기록. 이력 기록이 실패해도 전송 결과 자체는 그대로 돌려준다(대신 서버 로그에 남긴다). */
    private void record(Prod p, String resultType, String message, String logUrl, AuthSession.Actor actor) {
        try {
            SlinkLog l = new SlinkLog();
            l.setLogType("상품");
            l.setProdCode(p.getProdCode());
            l.setCateCode(p.getCateCode());
            l.setProdName(p.getProdName());
            l.setSaleYn(p.getSaleYn());
            l.setResultType(resultType);
            l.setResultMessag(message == null ? null
                    : message.length() > RESULT_MESSAGE_MAX ? message.substring(0, RESULT_MESSAGE_MAX) : message);
            l.setCallUrl(logUrl);   // 계정 ID 는 이미 가려진 주소
            l.setRegistId(actor == null ? null : actor.loginId());
            l.setRegistName(actor == null ? null : actor.name());
            slinkLogMapper.insert(l);
        } catch (RuntimeException e) {
            log.error("샵링커 전송 이력 기록 실패 prodCode={} resultType={}: {}", p.getProdCode(), resultType, e.toString());
        }
    }
}
