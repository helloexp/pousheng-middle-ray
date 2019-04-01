package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class SycHkShipmentOrderResponseBody implements Serializable {
    private static final long serialVersionUID = 2725171774930537903L;
    //中台主订单号
    private String orderNo;
    //恒康订单号
    private String erpOrderNo;
}
