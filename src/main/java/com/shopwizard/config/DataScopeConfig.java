package com.shopwizard.config;

import com.shopwizard.framework.auth.DataScopeFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 상점/채널 제한 관리자의 조회 요청에서 pShopCode/pChnlCode 를 세션 값으로 고정하는 필터 등록. */
@Configuration
public class DataScopeConfig {

    @Bean
    public FilterRegistrationBean<DataScopeFilter> dataScopeFilter(
            @Value("${app.authz.mode:enforce}") String mode) {
        FilterRegistrationBean<DataScopeFilter> reg =
                new FilterRegistrationBean<>(new DataScopeFilter(!"off".equalsIgnoreCase(mode.trim())));
        reg.addUrlPatterns("/*");
        reg.setName("dataScopeFilter");
        return reg;
    }
}
