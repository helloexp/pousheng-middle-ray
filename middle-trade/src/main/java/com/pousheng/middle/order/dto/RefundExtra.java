package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.ReceiverInfo;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class RefundExtra implements Serializable{

    //发货单id 如果不能自动匹配到发货单号，则需要人工拆单
    private Long shipmentId;

    //发货仓ID
    private Long warehouseId;

    //发货仓名称
    private String warehouseName;

    //买家收货地址
    private ReceiverInfo receiverInfo;

    //物流公司代码
    private String shipmentCorpCode;
    //物流公司名称
    private String shipmentCorpName;
    //物流单号
    private String shipmentSerialNo;

}
