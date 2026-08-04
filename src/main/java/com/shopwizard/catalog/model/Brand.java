package com.shopwizard.catalog.model;

import lombok.Data;

@Data
public class Brand {
    private Integer brandId;
    private String svcCode;
    private String compKorName;
    private String compEngName;
    private String brandKorName;
    private String brandEngName;
    private String brandAbbrName;
    private String remark;
    private Integer state;
    private String registDate; private String registId; private String registName;
    private String changeDate; private String changeId; private String changeName;
}
