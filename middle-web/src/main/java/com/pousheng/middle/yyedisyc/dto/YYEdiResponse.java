package com.pousheng.middle.yyedisyc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 同步发货单或者售后单时恒康返回消息的消息头
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class YYEdiResponse implements Serializable{
    private static final long serialVersionUID = -4030774668654863201L;
    //200:整体成功,100:部分成功,-100:整体失败
    private String errorCode;

    private String description;
    private List<YYEdiResponseField> fields;

    /**
     * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
     * Date: 2018/1/8
     * pousheng-middle
     */
    public static class YYEdiResponseField implements Serializable{

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
}
