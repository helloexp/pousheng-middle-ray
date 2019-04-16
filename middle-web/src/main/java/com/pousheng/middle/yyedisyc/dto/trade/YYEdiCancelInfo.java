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

    @JsonProperty(value = "billno")
    private String billno;
    @JsonProperty(value = "remark")
    private String remark;

    /** 
     * 2019.04.16 RAY: 增加companycode參數，標註定單來源
     */
    @JsonProperty(value = "companycode")
    private String companycode;    
    
    @JsonIgnore
    public String getCompanycode() {
		return companycode;
	}
    @JsonIgnore
	public void setCompanycode(String companycode) {
		this.companycode = companycode;
	}
	
	@JsonIgnore
    public String getBillno() {
        return billno;
    }
    @JsonIgnore
    public void setBillno(String billno) {
        this.billno = billno;
    }
    @JsonIgnore
    public String getRemark() {
        return remark;
    }
    @JsonIgnore
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
