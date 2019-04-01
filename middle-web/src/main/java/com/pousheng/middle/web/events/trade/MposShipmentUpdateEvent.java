package com.pousheng.middle.web.events.trade;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 *  发货单状态更新事件
 *  Created by penghui on 2017/12/22
 */
@Data
public class MposShipmentUpdateEvent implements Serializable{

    private static final long serialVersionUID = 2428761554690948062L;

    private Long shipmentId;

    private Map<String,String> extra;

    private MiddleOrderEvent middleOrderEvent;

    public MposShipmentUpdateEvent(Long shipmentId,MiddleOrderEvent middleOrderEvent,Map<String,String> extra){
        this.shipmentId = shipmentId;
        this.extra = extra;
        this.middleOrderEvent = middleOrderEvent;
    }

    public MposShipmentUpdateEvent(Long shipmentId,MiddleOrderEvent middleOrderEvent){
        this(shipmentId,middleOrderEvent,null);
    }
}
