package com.shopwizard.company.model;

import lombok.Data;

/**
 * 비회원도 볼 수 있는 shop.html 스토어프론트의 "전체 쇼핑몰" 드롭다운용 최소 정보.
 * {@link Shop}에는 loginId/passwd(평문)/연락처 등 민감 정보가 섞여 있어 통째로
 * 공개 API에 노출하면 안 된다 (실제로 그렇게 노출되고 있던 버그를 이 클래스로 고침).
 */
@Data
public class ShopPublicView {
    private String shopCode;
    private String shopName;
}
