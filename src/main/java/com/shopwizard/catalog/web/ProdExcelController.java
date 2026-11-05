package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.ProdExcelResult;
import com.shopwizard.catalog.service.ProdExcelService;
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
 * 상품 엑셀 일괄등록. 미리보기(/preview)는 검사만 하고, 등록(/import)은 서버가 다시 검사해서
 * 오류가 하나도 없을 때만 저장한다 — 미리보기 결과를 믿지 않는다.
 */
@RestController
@RequestMapping("/catalog/prod/excel")
@RequiredArgsConstructor
public class ProdExcelController {
    private final ProdExcelService prodExcelService;

    /** 빈 양식 다운로드(예전 양식과 같은 35열 .xls). */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("prodExcel.xls", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(prodExcelService.buildTemplate());
    }

    @PostMapping("/preview")
    public ProdExcelResult preview(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return prodExcelService.preview(file.getInputStream(), request);
    }

    @PostMapping("/import")
    public ProdExcelResult importAll(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return prodExcelService.importAll(file.getInputStream(), request);
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
