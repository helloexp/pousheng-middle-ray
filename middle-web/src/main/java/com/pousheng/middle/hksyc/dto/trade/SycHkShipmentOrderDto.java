package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkShipmentOrderDto implements Serializable {

    private static final long serialVersionUID = 6664208455711735199L;

    private SycHkShipmentOrder tradeOrder;

    private SycHkUserAddress userAddress;


}
