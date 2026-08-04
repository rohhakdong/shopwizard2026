package com.shopwizard.company.model;

import lombok.Data;

@Data
public class SupplyCompBoard {
    private int boardNo;
    private int mainId;
    private int treeId;
    private int childId;
    private int linkId;
    private String title;
    private String cntnts;
    private String email;
    private String phoneNo;
    private String passwd;
    private int treeCheck;
    private int visiteCnt;
    private String boardType;
    private String fileName;
    private String docTypeCode;
    private String chnlCode;
    private String supplyCode;
    private String supplyName;
    private String shopCode;
    private String shopName;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
