package com.shopwizard.adjust.model;

import lombok.Data;

@Data
public class AdjustCheckRqst {
    private Integer rqstNo;
    private Integer mainId;
    private Integer treeId;
    private Integer childId;
    private Integer linkId;
    private String title;
    private String cntnts;
    private String email;
    private String phoneNo;
    private Integer treeCheck;
    private String rqstSect;
    private String fileName;
    private String supplyCode;
    private String supplyName;
    private String shopCode;
    private String shopName;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
