package com.pousheng.middle.web.events.trade;

import io.terminus.parana.order.model.Shipment;
import lombok.Data;
import lombok.Getter;

/**
 * 判断订单或者售后订单下面的发货单是否已经全部发货完成
 * Created by tony on 2017/7/10.
 * pousheng-middle
 */
@Data
public class ShipOrderRefundEvent implements java.io.Serializable {

    private static final long serialVersionUID = -8769698989115811349L;

    private Shipment shipment;
}
