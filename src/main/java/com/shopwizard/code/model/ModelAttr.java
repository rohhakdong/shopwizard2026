package com.shopwizard.code.model;

import lombok.Data;

@Data
public class ModelAttr {
    private Integer modelId;
    private Integer attrId;
    private String korWord1;
    private String korWord2;
    private String korWord3;
    private String korWord4;
    private String korWord5;
    private String engAbbr1;
    private String engAbbr2;
    private String engAbbr3;
    private String engAbbr4;
    private String engAbbr5;
    private String alterName;
    private Integer attrSeq;
    private String dataType;
    private Integer dataLength;
    private String constrCode;
    private Integer reqirdYn;
    private Integer searchYn;
    private Integer listYn;
    private String inputType;
    private String defaltVal;
    private Integer pkYn;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
