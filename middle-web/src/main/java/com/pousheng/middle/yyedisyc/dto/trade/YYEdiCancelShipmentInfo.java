package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
public class YYEdiCancelShipmentInfo implements java.io.Serializable{
    private static final long serialVersionUID = 4658593769032049717L;

    @JsonProperty(value = "BillNo")
    private String BillNo;
    @JsonProperty(value = "ReMark")
    private String ReMark;

    public String getBillNo() {
        return BillNo;
    }

    public void setBillNo(String billNo) {
        BillNo = billNo;
    }

    public String getReMark() {
        return ReMark;
    }

    public void setReMark(String reMark) {
        ReMark = reMark;
    }
}
