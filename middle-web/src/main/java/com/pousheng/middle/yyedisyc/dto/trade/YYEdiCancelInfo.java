package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
public class YYEdiCancelInfo implements java.io.Serializable{
    private static final long serialVersionUID = 4658593769032049717L;

    @JsonProperty(value = "BillNo")
    private String BillNo;
    @JsonProperty(value = "ReMark")
    private String ReMark;

    @JsonIgnore
    public String getBillNo() {
        return BillNo;
    }

    @JsonIgnore
    public void setBillNo(String billNo) {
        BillNo = billNo;
    }
    @JsonIgnore
    public String getReMark() {
        return ReMark;
    }
    @JsonIgnore
    public void setReMark(String reMark) {
        ReMark = reMark;
    }
}
