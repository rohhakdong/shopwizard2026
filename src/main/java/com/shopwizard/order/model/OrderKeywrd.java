package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderKeywrd {
    private String keywrdSeq;
    private String keywrdName;
    private String keywrdSect;
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
