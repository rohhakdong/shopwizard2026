package com.shopwizard.catalog.model;

import lombok.Data;

@Data
public class Cate {
    private String cateCode;
    private String svcCode;
    private String cateName;
    private String cateDesc;
    private String parentCateCode;
    private Integer cateLevel;
    private Integer cateSeq;
    private Integer childCount;
    private String cateCode1; private String cateName1;
    private String cateCode2; private String cateName2;
    private String cateCode3; private String cateName3;
    private String cateCode4; private String cateName4;
    private String remark;
    private Integer state;
    private String registDate; private String registId; private String registName;
    private String changeDate; private String changeId; private String changeName;
}
