package com.shopwizard.statistics.model;

import lombok.Data;

@Data
public class CustDrop {
    private Integer dropNo;
    private String dropReason;
    private String dropReasonDetail;
    private String chnlCode;
    private String registDate;
}
