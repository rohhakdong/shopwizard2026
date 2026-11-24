package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderPriceResult;
import com.shopwizard.order.service.OrderPriceExcelService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 주문관리의 "엑셀 가격수정". 검사(/preview)는 DB 를 바꾸지 않고, 수정(/import)은 서버가 다시 검사해서 변경 행만 고친다 —
 * 검사 결과를 믿지 않는다. 멀티파트라 감사값(수정자)은 서비스가 세션에서 직접 읽고, 줄마다 데이터 범위를 검사한다.
 */
@RestController
@RequestMapping("/order/price-excel")
@RequiredArgsConstructor
public class OrderPriceExcelController {
    private final OrderPriceExcelService orderPriceExcelService;

    /** 빈 양식 다운로드(레거시 priceExcel.xls 와 같은 헤더). */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("priceExcel.xls", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(orderPriceExcelService.buildTemplate());
    }

    @PostMapping("/preview")
    public OrderPriceResult preview(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return orderPriceExcelService.preview(file.getInputStream(), request);
    }

    @PostMapping("/import")
    public OrderPriceResult importFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return orderPriceExcelService.importFile(file.getInputStream(), file.getOriginalFilename(), request);
    }

    private static void requireExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "엑셀 파일을 선택하세요.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "엑셀 파일(.xls, .xlsx)만 올릴 수 있습니다.");
        }
    }
}
