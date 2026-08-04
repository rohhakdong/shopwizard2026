package com.shopwizard.store.model;

import lombok.Data;

@Data
public class Layout {
    private Integer designId;
    private String layoutCode;
    private String layoutName;
    private String layoutDesc;
    private String parentLayoutCode;
    private Integer layoutLevel;
    private Integer layoutSeq;
    private String imgName;
    private String imgLink;
    private Integer imgWidth;
    private Integer imgHeight;
    private String imgHtml;
    private Integer state;
    private String remark;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
    private Integer childCount;
}
