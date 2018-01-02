package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/2
 * pousheng-middle
 */
@Data
public class ShipInfo {
    /**
     * 中台发货单号
     */
    private Long shipmentId;
    /**
     * yyEDI发货单号
     */
    private String yyEDIShipmentId;
    /**
     * 物流公司代码
     */
    private String shipmentCorpCode;
    /**
     * 物流单号，可以用逗号隔开，可以有多个物流单号
     */
    private String shipmentSerialNo;
    /**
     * 发货时间
     */
    private String shipmentDate;
}
