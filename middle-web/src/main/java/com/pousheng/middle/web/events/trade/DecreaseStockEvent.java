package com.pousheng.middle.web.events.trade;

import io.terminus.parana.order.model.Shipment;
import lombok.Data;

/**
 * 扣减库存事件,适用于发货单取消时释放库存
 * Created by tony on 2017/7/13
 * pousheng-middle
 */
@Data
public class DecreaseStockEvent implements java.io.Serializable{

    private static final long serialVersionUID = -4723865584369752826L;
    //发货单
    private Shipment shipment;
}
