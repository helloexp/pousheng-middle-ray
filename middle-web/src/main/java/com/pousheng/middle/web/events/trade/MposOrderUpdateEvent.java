package com.pousheng.middle.web.events.trade;

import io.terminus.parana.order.model.Shipment;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * mpos订单状态更新事件
 * Created by penghui on 2017/12/22
 */
@Data
public class MposOrderUpdateEvent implements Serializable{

    private static final long serialVersionUID = -3222888769931865929L;

    /**
     * 订单号
     */
    private Long orderId;

    /**
     * 更新类型
     */
    private Integer type;

    /**
     * 可用发货单集合
     */
    private List<Shipment> shipments;

    public MposOrderUpdateEvent(Long orderId,Integer type,List<Shipment> shipments){
        this.orderId = orderId;
        this.type = type;
        this.shipments = shipments;
    }

}
