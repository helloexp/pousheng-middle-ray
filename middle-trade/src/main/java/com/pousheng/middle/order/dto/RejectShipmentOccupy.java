package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * 拒单占用库存domain
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.order.dto
 * 2018/8/14 09:44
 * pousheng-middle
 */
@Data
public class RejectShipmentOccupy {
    /**
     * 占用库存的发货单id
     */
    private Long shipmentId;

    /**
     * 发货单占用库存状态
     */
    private String status;


    public enum ShipmentOccupyStatus{
        /**
         * 占用
         */
        OCCUPY,
        /**
         * 释放
         */
        RELEASE;
    }
}
