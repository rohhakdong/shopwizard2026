package com.shopwizard.product.model;

import lombok.Data;

@Data
public class ProdBrand {
    private Integer brandId;
    private String compKorName;
    private String compEngName;
    private String brandKorName;
    private String brandEngName;
    private String brandAbbrName;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
