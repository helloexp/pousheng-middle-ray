package com.pousheng.middle.web.order.event;

import io.terminus.parana.order.model.Shipment;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2018/4/3
 */
@Data
public class ShipmentPosToHkEvent implements Serializable{

    private static final long serialVersionUID = 7339637498571920377L;

    private Shipment shipment;
}
