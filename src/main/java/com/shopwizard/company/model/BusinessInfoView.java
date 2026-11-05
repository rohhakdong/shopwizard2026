package com.shopwizard.company.model;

import lombok.Data;

/**
 * 비회원도 보는 shop.html 하단 "사업자 정보"용 공개 뷰. 전자상거래법상 표시 항목(상호·대표자·사업자등록번호·
 * 통신판매업 신고번호·주소·고객센터)만 담는다 — 계좌번호·팩스·담당자 같은 내부 정보는 일부러 싣지 않는다.
 */
@Data
public class BusinessInfoView {
    private String compName;        // 상호
    private String prsdntName;      // 대표자
    private String bizNo;           // 사업자등록번호 (123-45-67890)
    private String corpNo;          // 법인등록번호 (123456-1234567)
    private String mordrSaleNo;     // 통신판매업 신고번호
    private String zipcode;
    private String address;         // 주소1 + 주소2
    private String phoneNo;         // 고객센터 전화
    private String email;
    private String homepg;
}
