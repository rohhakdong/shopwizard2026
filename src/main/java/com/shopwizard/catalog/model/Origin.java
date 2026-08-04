package com.shopwizard.catalog.model;

import lombok.Data;

@Data
public class Origin {
    private Integer originId;
    private String originKorName;
    private String originEngName;
    private String originDesc;
    private String nationCode;
    private String nationName;
    private String remark;
    private Integer state;
    private String registDate; private String registId; private String registName;
    private String changeDate; private String changeId; private String changeName;
}
