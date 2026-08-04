package com.shopwizard.authority.model;

import lombok.Data;

@Data
public class Menu {
    private String menuCode;
    private String roleUid;
    private String roleName;
    private String menuName;
    private String menuUri;
    private String parentMenuCode;
    private String parentMenuName;
    private int menuLevel;
    private int menuSeq;
    private int childCount;
    private String menuCode1;
    private String menuName1;
    private String menuCode2;
    private String menuName2;
    private String menuCode3;
    private String menuName3;
    private String menuCode4;
    private String menuName4;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
