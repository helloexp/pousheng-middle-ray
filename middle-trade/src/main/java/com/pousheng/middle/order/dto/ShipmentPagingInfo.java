package com.pousheng.middle.order.dto;

import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 发货单分页信息封装
 * Created by songrenfei on 2017/6/20
 */
@Data
public class ShipmentPagingInfo implements Serializable{

    private static final long serialVersionUID = 7706149878099313346L;


    /**
     * 发货订单信息
     */
    private OrderShipment orderShipment;

    /**
     * 发货单信息
     */
    private Shipment shipment;

    /**
     * 订单信息
     */
    private ShopOrder shopOrder;

    //extra信息
    private ShipmentExtra shipmentExtra;

    /**
     * 操作
     */
    private Set<OrderOperation> operations;
}
