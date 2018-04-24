package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 同步ERP发货单返回结果
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class SycErpShipmentOrderResponse implements Serializable {

    private static final long serialVersionUID = 3481612408765835667L;

    //@JsonProperty(value = "ErrorCode")
    private Integer errorCode;

    //@JsonProperty(value = "Description")
    private String description;

    //@JsonProperty(value = "Fields")
    private List<ResultItem> fields;


    @Data
    public static class ResultItem implements Serializable{
        private static final long serialVersionUID = 715850793297987800L;

        //@JsonProperty(value = "OrderNo")
        private String orderNo;
        //@JsonProperty(value = "Status")
        private Integer status;
        //@JsonProperty(value = "ErrorMsg")
        private String errorMsg;
    }
}
