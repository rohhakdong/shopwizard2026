package com.shopwizard.operate.model;

import lombok.Data;

@Data
public class Faq {
    private Integer faqNo;
    private String custConsltType;
    private String faqTypeDesc;
    private String faqTitle;
    private String faqCntnts;
    private String chnlCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
