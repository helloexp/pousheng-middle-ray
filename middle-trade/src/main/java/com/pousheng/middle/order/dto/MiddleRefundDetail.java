package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Refund;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class MiddleRefundDetail implements Serializable {

    private static final long serialVersionUID = -1985259516399709308L;

    private Refund refund;

    private OrderRefund orderRefund;

    private List<RefundItem> refundItems;

    private RefundExtra refundExtra;

    //发货信息,这里包成list只是为了方便前端复用其他页面的发货单信息渲染
    private List<OrderShipment> orderShipments;
    //发货信息
    private List<ShipmentItem> shipmentItems;
}
