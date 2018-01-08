package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/8
 * pousheng-middle
 */
public class YYEdiShipmentResponseField implements java.io.Serializable{

    private static final long serialVersionUID = -667422813757126090L;

    @JsonProperty(value = "CompanyCode")
    private String CompanyCode;
    @JsonProperty(value = "BillNo")
    private String BillNo;

    //200表示成功
    @JsonProperty(value = "Status")
    private String Status;
    @JsonProperty(value = "ErrorMsg")
    private String ErrorMsg;
}
