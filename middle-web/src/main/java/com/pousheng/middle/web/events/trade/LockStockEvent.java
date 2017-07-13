package com.pousheng.middle.web.events.trade;

import io.terminus.parana.order.model.Shipment;
import lombok.Data;

/**
 * 锁定库存事件,适用于销售发货单以及售后发货单
 * Created by tony on 2017/7/13
 * pousheng-middle
 */
@Data
public class LockStockEvent implements java.io.Serializable{

    private static final long serialVersionUID = -4723865584369752826L;
    //发货单
    private Shipment shipment;
}
