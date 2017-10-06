package com.pousheng.middle.order.dto;

import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ShopOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by songrenfei on 2017/7/6
 */
@Data
public class ShopOrderPagingInfo implements Serializable{

    ShopOrder shopOrder;

    /**
     * 操作
     */
    private Set<OrderOperation> shopOrderOperations;
}
