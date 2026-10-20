package com.shopwizard.framework.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** DB 쿼리 오류 */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException e) {
        log.error("DB 오류: {}", e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류가 발생했습니다.");
    }

    /** 파라미터 타입 불일치 (e.g. /cust/abc → Integer 변환 실패) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 오류: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, "잘못된 요청 파라미터입니다: " + e.getName());
    }

    /** 필수 파라미터 누락 등 잘못된 요청 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 컨트롤러가 의도해서 던진 상태코드(400 잘못된 요청, 401 로그인 필요 등)는 그대로 돌려준다.
     * 아래 Exception 핸들러가 이것까지 삼켜서 모두 500으로 바꿔버리는 문제가 있었다.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return error(status, e.getReason() != null ? e.getReason() : status.getReasonPhrase());
    }

    /**
     * 없는 경로/허용하지 않는 메서드는 클라이언트 오류다. 아래 Exception 핸들러가 이것까지 500으로 바꾸고
     * 에러 로그를 남기던 것을, 405/404 로 정직하게 돌려준다(제거된 쓰기 엔드포인트 호출이 서버 오류처럼 보이지 않게).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        return error(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다.");
    }

    /** 요청 본문을 읽을 수 없는 경우(깨진 JSON, 잘못된 인코딩 등)도 클라이언트 오류다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("요청 본문 오류: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다.");
    }

    /** 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e) {
        log.error("처리되지 않은 예외: {}", e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
