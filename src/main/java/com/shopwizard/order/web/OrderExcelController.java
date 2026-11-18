package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderExcelResult;
import com.shopwizard.order.model.OrderExcelUpload;
import com.shopwizard.order.model.OrderExcelUploadDetail;
import com.shopwizard.order.service.OrderExcelService;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 주문 엑셀 업로드("주문엑셀등록"). 검사(/preview)는 DB 를 바꾸지 않고, 등록(/import)은 서버가 다시 검사해서 정상 행만 저장한다 —
 * 검사 결과를 믿지 않는다. 멀티파트라 감사값(등록자)은 서비스가 세션에서 직접 읽는다.
 */
@RestController
@RequestMapping("/order/excel")
@RequiredArgsConstructor
public class OrderExcelController {
    private final OrderExcelService orderExcelService;

    /** 빈 양식 다운로드(예전 양식과 같은 28열 .xls). */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("orderExcel.xls", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(orderExcelService.buildTemplate());
    }

    @PostMapping("/preview")
    public OrderExcelResult preview(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return orderExcelService.preview(file.getInputStream(), request);
    }

    @PostMapping("/import")
    public OrderExcelResult importFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return orderExcelService.importFile(file.getInputStream(), file.getOriginalFilename(), request);
    }

    /** 업로드 이력 목록 — pStartDate/pEndDate(yyyy-MM-dd), pPageOffset/pPageSize. */
    @GetMapping("/upload/list")
    public List<OrderExcelUpload> uploads(@RequestParam Map<String, String> params, HttpServletRequest request) {
        return orderExcelService.uploads(params, request);
    }

    @GetMapping("/upload/count")
    public int uploadCount(@RequestParam Map<String, String> params, HttpServletRequest request) {
        return orderExcelService.uploadCount(params, request);
    }

    @GetMapping("/upload/detail")
    public List<OrderExcelUploadDetail> uploadDetails(@RequestParam("uploadId") int uploadId, HttpServletRequest request) {
        return orderExcelService.uploadDetails(uploadId, request);
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
