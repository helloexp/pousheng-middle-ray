package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.Data;

import java.io.Serializable;

/**
 * 发货单复杂信息封装
 * Created by songrenfei on 2017/6/20
 */
@Data
public class ShipmentDto implements Serializable{

    private static final long serialVersionUID = 7706149878099313346L;


    /**
     * 发货订单信息
     */
    private OrderShipment orderShipment;

    /**
     * 发货单信息
     */
    private Shipment shipment;
}
