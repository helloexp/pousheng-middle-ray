package com.pousheng.middle.order.dto;

import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Refund;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Created by songrenfei on 2017/7/5
 */
@Data
public class RichMiddleRefund implements Serializable{


    private Refund refund;

    private OrderRefund orderRefund;

    private RefundExtra refundExtra;

    //发货信息
    private List<OrderShipment> orderShipments;

    private Set<OrderOperation> operations;

    //换货收货人信息
    private MiddleChangeReceiveInfo middleChangeReceiveInfo;
}
