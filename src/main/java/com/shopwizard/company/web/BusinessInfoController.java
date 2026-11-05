package com.shopwizard.company.web;

import com.shopwizard.company.model.BusinessInfoView;
import com.shopwizard.company.service.BusinessInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 비회원도 보는 스토어프론트 하단 사업자 정보 (인증 불필요 — {@code MngrPaths.PUBLIC_READ}, GET 한정). */
@RestController
@RequestMapping("/company/shop/business-info")
@RequiredArgsConstructor
public class BusinessInfoController {
    private final BusinessInfoService businessInfoService;

    @GetMapping
    public BusinessInfoView get() {
        return businessInfoService.get();
    }
}
