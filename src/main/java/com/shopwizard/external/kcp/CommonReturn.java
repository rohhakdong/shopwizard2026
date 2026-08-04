package com.shopwizard.external.kcp;

import lombok.Data;

@Data
public class CommonReturn {
    private String site_cd;
    private String tno;
    private String ordr_idxx;
    private String ipgm_amt;
    private String res_cd;
    private String res_msg;
    private String enc_data;
    private String enc_info;
    private String refund_mny;
    private String refund_type;
    private String cancel_yn;
    private String partcancel_yn;
}
