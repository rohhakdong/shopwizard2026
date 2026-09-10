package com.shopwizard.order.service;

import com.shopwizard.external.shoplinker.Order;
import com.shopwizard.external.shoplinker.OrderHandler;
import com.shopwizard.order.model.ShoplinkerCollectRequest;
import com.shopwizard.order.model.ShoplinkerCollectResult;
import com.shopwizard.order.model.SlnkTrans;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 샵링커 협력사 API에서 주문을 긁어와 샵피온 주문으로 생성한다.
 * 레거시 {@code OrderServiceImpl#runInsertOrderFromShoplinkerInSchedule} 이식.
 *
 * <p>실행 흐름: 몰(target)별로 {@code tOrdSlnkTrans} 이력행 생성(진행) →
 * XML 호출/파싱 → 건별 주문 생성({@link ShoplinkerOrderWriter}, 건별 커밋) →
 * 이력행을 성공/실패로 마감. 클래스 레벨 트랜잭션 없음(건별 격리).</p>
 */
@Service
@RequiredArgsConstructor
public class ShoplinkerCollectService {

    private static final Logger log = LoggerFactory.getLogger(ShoplinkerCollectService.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${shoplinker.order-url}")
    private String orderUrl;
    @Value("${shoplinker.order2-url}")
    private String order2Url;
    @Value("${shoplinker.timeout-sec:60}")
    private int timeoutSec;

    private final SlnkTransService slnkTransService;
    private final ShoplinkerOrderWriter orderWriter;

    public ShoplinkerCollectResult collect(ShoplinkerCollectRequest req) {
        ShoplinkerCollectResult result = new ShoplinkerCollectResult();

        if (req.getMalls() == null || req.getMalls().isEmpty()) {
            result.setOk(false);
            result.setMessage("수집할 몰을 선택하세요.");
            return result;
        }
        if (isBlank(req.getStartDate()) || isBlank(req.getEndDate())) {
            result.setOk(false);
            result.setMessage("시작일과 종료일을 입력하세요.");
            return result;
        }
        if (slnkTransService.selectCountByResultType("진행") > 0) {
            result.setOk(false);
            result.setMessage("이미 진행 중인 수집이 있습니다. 완료 후 다시 시도하세요.");
            return result;
        }

        String registId = nz(req.getRegistId());
        String registName = nz(req.getRegistName());

        for (ShoplinkerCollectRequest.MallTarget target : req.getMalls()) {
            String mallCode = nz(target.getMallCode());
            String loginId = nz(target.getLoginId());
            String mallName = mallDisplayName(mallCode, loginId);

            SlnkTrans t = new SlnkTrans();
            t.setStartDate(req.getStartDate());
            t.setEndDate(req.getEndDate());
            t.setMallCode(mallCode);
            t.setLoginId(loginId);
            t.setResultType("진행");
            t.setTransStartDate(LocalDateTime.now().format(DT));
            t.setRegistId(registId);
            t.setRegistName(registName);
            String url = buildUrl(mallCode, loginId, req.getStartDate(), req.getEndDate());
            t.setShoplinkerUrl(url);
            slnkTransService.insert(t);   // transId 채번됨

            ShoplinkerCollectResult.MallSummary sm = new ShoplinkerCollectResult.MallSummary();
            sm.setTransId(t.getTransId());
            sm.setMallName(mallName);

            try {
                List<Order> orders = fetch(url);
                sm.setTotal(orders.size());

                int idx = 0;
                for (Order o : orders) {
                    o.setRegistId(registId);
                    o.setRegistName(registName);
                    ShoplinkerOrderWriter.Outcome outcome = processOne(t.getTransId(), idx++, o, registId, registName);
                    switch (outcome) {
                        case SUCCESS   -> sm.setSuccess(sm.getSuccess() + 1);
                        case DUPLICATE -> sm.setDuplicate(sm.getDuplicate() + 1);
                        case SKIP      -> sm.setSkip(sm.getSkip() + 1);
                        case ERROR     -> sm.setError(sm.getError() + 1);
                    }
                }

                t.setResultType("성공");
                t.setResultMessag(String.format("총 %d건 (성공 %d · 중복 %d · 무시 %d · 오류 %d)",
                        sm.getTotal(), sm.getSuccess(), sm.getDuplicate(), sm.getSkip(), sm.getError()));
                sm.setResultType("성공");
                sm.setResultMessag(t.getResultMessag());
            } catch (Exception e) {
                log.error("샵링커 수집 실패 mall={} url={}", mallName, url, e);
                result.setOk(false);
                t.setResultType("실패");
                t.setResultMessag(cut(safeMsg(e), 512));
                sm.setResultType("실패");
                sm.setResultMessag(t.getResultMessag());
            } finally {
                t.setTransEndDate(LocalDateTime.now().format(DT));
                slnkTransService.update(t);
                result.getMalls().add(sm);
            }
        }
        return result;
    }

    /** 건별 처리 — 중복/교환/무시 판정 후 주문 생성. 이력행은 항상 남긴다. */
    private ShoplinkerOrderWriter.Outcome processOne(int transId, int no, Order o,
                                                     String registId, String registName) {
        String code = nz(o.getCode());
        try {
            if (orderWriter.countByCode(code) > 0) {
                orderWriter.writeDetail(transId, no, code, "중복", "이미 처리된 건과 중복입니다.", registId, registName);
                return ShoplinkerOrderWriter.Outcome.DUPLICATE;
            }
            // GS홈쇼핑(eshop)은 교환주문도 새로 생성. 그 외 교환주문(change_yn=Y)은 누락.
            if ("Y".equals(nz(o.getChangeyn())) && !"GS홈쇼핑(eshop)".equals(nz(o.getMallcode()))) {
                orderWriter.writeDetail(transId, no, code, "중복", "교환주문접수 건이라서 누락시켰습니다.", registId, registName);
                return ShoplinkerOrderWriter.Outcome.DUPLICATE;
            }
            String skip = orderWriter.skipReason(o);
            if (skip != null) {
                orderWriter.writeDetail(transId, no, code, "무시", skip, registId, registName);
                return ShoplinkerOrderWriter.Outcome.SKIP;
            }
            String bizErr = orderWriter.createOrder(o, registId, registName);
            if (bizErr != null) {
                orderWriter.writeDetail(transId, no, code, "오류", bizErr, registId, registName);
                return ShoplinkerOrderWriter.Outcome.ERROR;
            }
            orderWriter.writeDetail(transId, no, code, "성공", "처리에 성공하였습니다.", registId, registName);
            return ShoplinkerOrderWriter.Outcome.SUCCESS;
        } catch (Exception e) {
            log.error("샵링커 주문 생성 실패 code={}", code, e);
            orderWriter.writeDetail(transId, no, code, "오류", safeMsg(e), registId, registName);
            return ShoplinkerOrderWriter.Outcome.ERROR;
        }
    }

    // ── 수집 ──────────────────────────────────────────────────────────

    private List<Order> fetch(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSec, 30)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .GET()
                .build();
        HttpResponse<byte[]> resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("샵링커 응답 오류 HTTP " + resp.statusCode());
        }
        byte[] body = resp.body();
        if (body == null || body.length == 0) {
            return new ArrayList<>();
        }
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(false);
        XMLReader xr = spf.newSAXParser().getXMLReader();
        OrderHandler handler = new OrderHandler();
        xr.setContentHandler(handler);
        try {
            xr.parse(new InputSource(new ByteArrayInputStream(body)));
        } catch (Exception e) {
            String preview = new String(body, StandardCharsets.UTF_8);
            preview = preview.length() > 200 ? preview.substring(0, 200) : preview;
            throw new IllegalStateException("샵링커 응답 XML 파싱 실패: " + e.getMessage() + " / 응답앞부분=" + preview, e);
        }
        List<Order> list = handler.getOrderList();
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 일반몰: order.php?sdt=&edt=&code=
     * 오픈마켓: order2.php?sdt=&edt=&code=&mall_order_code={A|G|R|11}&mall_user_id={loginId}
     */
    private String buildUrl(String mallCode, String loginId, String startDate, String endDate) {
        String sdt = startDate.replace("-", "");
        String edt = endDate.replace("-", "");
        if (mallCode.isEmpty()) {
            return orderUrl + "?sdt=" + sdt + "&edt=" + edt + "&code=";
        }
        return order2Url + "?sdt=" + sdt + "&edt=" + edt + "&code="
                + "&mall_order_code=" + mallCode + "&mall_user_id=" + loginId;
    }

    private static String mallDisplayName(String mallCode, String loginId) {
        String base = switch (mallCode) {
            case ""   -> "오픈마켓제외 쇼핑몰";
            case "A"  -> "옥션";
            case "G"  -> "지마켓(법인)";
            case "R"  -> "지마켓(개인)";
            case "11" -> "11번가";
            default   -> mallCode;
        };
        return loginId.isEmpty() ? base : base + " " + loginId;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
    private static String safeMsg(Throwable e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
    }
}
