package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/27
 */
@Data
public class ShopOrderWithReceiveInfo implements Serializable{

    private static final long serialVersionUID = 1703858758163191271L;
    private ShopOrder shopOrder;

    private ReceiverInfo receiverInfo;
}
