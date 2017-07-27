package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class SycHkRefundResponseBody implements Serializable {
    private static final long serialVersionUID = 560155846439199229L;
    //中台退换货单号
    private String refundNo;
    //恒康订单号
    private String erpOrderNo;
}
