package com.shopwizard.product.model;

import lombok.Data;

@Data
public class ProdReview {
    private Integer reviewNo;
    private String reviewTitle;
    private String reviewCntnts;
    private Integer viewCnt;
    private Integer custId;
    private String custName;
    private String brandName;
    private Integer orderNo;
    private Integer orderProdNo;
    private String prodCode;
    private String prodName;
    private String prodImg;
    private String reviewImg;
    private Integer prodGrade;
    private Integer deliGrade;
    private Integer qualtyGrade;
    private Integer priceGrade;
    private Integer svcGrade;
    private String chnlCode;
    private String chnlName;
    private Integer bestYn;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
