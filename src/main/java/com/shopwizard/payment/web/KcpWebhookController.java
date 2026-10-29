package com.shopwizard.payment.web;

import com.shopwizard.order.service.KcpWebhookService;
import com.shopwizard.payment.service.KcpProperties;
import com.shopwizard.payment.service.KcpWebhookVerifier;
import com.shopwizard.payment.service.KcpWebhookVerifier.HashResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * KCP 웹훅 수신(가상계좌 입금 통보). 파트너관리자에 https 주소로 등록해야 호출된다.
 *
 * <p>순서: 발신 IP → hash_data → 업무 구분(TX00 입금) → {@link KcpWebhookService}(KCP 조회로 입금 재확인 후 지불완료).
 * KCP 는 응답 본문에 {@code result=0000} 이 없으면 실패로 보고 최대 10회 재전송하므로, 다시 받아야 하는 경우에만 다른 값을 돌려준다.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment/webhook")
public class KcpWebhookController {

    private static final int MAX_BODY = 64 * 1024;
    // KCP 는 응답 본문에서 result=0000 을 찾는다. 문서는 "result=0000" 이라고만 적었지만, 평문 응답에는 KCP 가 성공으로 보지 않고
    // 같은 통보를 계속 재전송했다(실측). KCP 공통통보의 응답 규격(result 라는 hidden input)에 맞춰 HTML 로 응답하고,
    // 평문 형식을 기대하는 파서를 위해 같은 값을 주석에도 넣어 둔다.
    private static final String OK = response("0000");
    private static final String RETRY = response("9999");

    private static String response(String code) {
        return "<html><body><!-- result=" + code + " --><form><input type=\"hidden\" name=\"result\" value=\"" + code + "\"></form></body></html>";
    }

    private final KcpWebhookVerifier verifier;
    private final KcpWebhookService webhookService;
    private final KcpProperties props;

    @PostMapping(value = "/kcp", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> handle(HttpServletRequest request) throws IOException {
        String ip = verifier.clientIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
        if (!verifier.ipAllowed(ip)) {
            log.warn("KCP 웹훅: 허용되지 않은 발신 IP 입니다. ip={}", ip);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
        }

        // 본문 인코딩이 문서에 없다 — 모바일 복귀와 같은 방식(원시 바이트를 EUC-KR 로 해석)으로 읽고, 비어 있으면 컨테이너가 읽은 값을 쓴다.
        Map<String, String> p = KcpMobileController.parseReturnBody(request.getInputStream().readNBytes(MAX_BODY), request);

        HashResult hash = verifier.verifyHash(p);
        if (hash == HashResult.MISMATCH || (hash == HashResult.NO_KEY && props.prod())) {
            log.warn("KCP 웹훅: hash_data 검증에 실패했습니다. tno={} tx_cd={}", p.get("tno"), p.get("tx_cd"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
        }

        String txCd = p.get("tx_cd");
        String opCd = p.get("op_cd");
        // 실제 요청 모양을 알 수 있게 필드 이름과 hash_data 길이만 남긴다(값은 남기지 않는다 — 검증키·계좌 정보 보호).
        String sentHash = p.get("hash_data") != null ? p.get("hash_data") : p.get("noti_hash");
        log.info("KCP 웹훅 수신 tx_cd={} op_cd={} tno={} hash={} fields={} hash_len={}", txCd, opCd, p.get("tno"), hash,
                p.keySet(), sentHash == null ? 0 : sentHash.length());

        // 이 시스템이 처리하는 것은 가상계좌 입금(TX00)뿐이다. 에스크로 등 나머지는 받았다고만 응답한다.
        if (!"TX00".equals(txCd)) {
            return ok();
        }
        if ("13".equals(opCd)) {
            // 입금 취소 — 지불완료로 바뀐 주문을 자동으로 되돌리면 이미 나간 물건과 엇갈릴 수 있어 사람이 확인한다.
            log.error("KCP 가상계좌 입금 취소 통보 — 수동 확인 필요. tno={} order_no={} 금액={}", p.get("tno"), p.get("order_no"), p.get("ipgm_mnyx"));
            return ok();
        }

        try {
            KcpWebhookService.Outcome outcome = webhookService.handleDeposit(p.get("tno"), toLong(p.get("ipgm_mnyx")));
            return outcome == KcpWebhookService.Outcome.RETRY ? retry() : ok();
        } catch (Exception e) {
            // KCP 조회 실패·DB 오류 등 — 다시 받을 수 있게 실패로 응답한다.
            log.error("KCP 웹훅 처리 실패 tno={}: {}", p.get("tno"), e.getMessage());
            return retry();
        }
    }

    private static ResponseEntity<String> ok() {
        return ResponseEntity.ok(OK);
    }

    private static ResponseEntity<String> retry() {
        return ResponseEntity.ok(RETRY);
    }

    private static Long toLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
