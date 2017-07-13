package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发货单详情
 * Created by songrenfei on 2017/6/22
 */
@Data
public class ShipmentDetail extends OrderBasicInfo implements Serializable{

    private static final long serialVersionUID = -200347009366007210L;

    //发货信息
    private Shipment shipment;

    private OrderShipment orderShipment;

    private List<ShipmentItem> shipmentItems;

    private ShipmentExtra shipmentExtra;







}
