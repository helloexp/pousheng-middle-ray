package com.pousheng.middle.order.dto;

import com.pousheng.middle.order.model.PoushengGiftActivity;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import lombok.Data;

import java.util.Set;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@Data
public class PoushengGiftActivityPagingInfo implements java.io.Serializable{
    private static final long serialVersionUID = 5558790462801772617L;

    private PoushengGiftActivity poushengGiftActivity;
    /**
     * 操作类型
     */
    private Set<OrderOperation> giftOperations;
}
