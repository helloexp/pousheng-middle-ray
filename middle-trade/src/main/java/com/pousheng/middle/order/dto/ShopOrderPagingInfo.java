package com.pousheng.middle.order.dto;

import com.pousheng.middle.order.model.MiddleShopOrder;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by songrenfei on 2017/7/6
 */
@Data
public class ShopOrderPagingInfo implements Serializable{

    MiddleShopOrder shopOrder;

    /**
     * 操作
     */
    private Set<OrderOperation> shopOrderOperations;
}
