package com.shopwizard.company.model;

import lombok.Data;

@Data
public class DeliComp {
    private int deliCompCode;
    private String deliCompName;
    private String deliCompDesc;
    private String linkUrl;
    private String invNoVar;
    /** 샵링커에 송장을 보낼 때 쓰는 택배사 이름(비면 deliCompName). */
    private String slnkDeliName;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
