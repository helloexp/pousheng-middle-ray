package com.pousheng.middle.order.dto;

import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import lombok.Data;

import java.util.Set;

/**
 * 逆向订单分页信息
 * Created by songrenfei on 2017/6/26
 */
@Data
public class RefundPaging {

    private Refund refund;

    private OrderRefund orderRefund;

    private Set<OrderOperation> operations;
}
