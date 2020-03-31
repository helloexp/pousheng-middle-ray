package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 发货单拒单原因
 * AUTHOR: zhangbin
 * ON: 2019/8/12
 */
@Data
public class ShipmentRejectReason implements Serializable {
    private static final long serialVersionUID = -948505083624500508L;

    //销售店铺
    private Long shopId;

    private String shopName;

    private Long shipmentId;

    private String skuCode;

    private String shipmentCode;

    private String rejectReason;

    private Date rejectAt;

    //接单店仓
    private Long warehouseId;

}
