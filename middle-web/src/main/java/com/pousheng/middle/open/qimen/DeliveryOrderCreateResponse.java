package com.pousheng.middle.open.qimen;

import lombok.Data;

import java.util.List;

/**
 * Created by cp on 8/22/17.
 */
@Data
public class DeliveryOrderCreateResponse extends QimenResponse {

    private static final long serialVersionUID = 5224732627212295612L;

    private String createTime;
    private String deliveryOrderId;
    private List<DeliveryOrder> deliveryOrders;
    private String logisticsCode;
    private String warehouseCode;

    @Data
    public static class DeliveryOrder {
        private String createTime;
        private String deliveryOrderId;
        private String logisticsCode;
        private List<DeliveryOrderCreateResponse.OrderLine> orderLines;
        private String warehouseCode;
    }

    @Data
    public static class OrderLine {
        private String itemCode;
        private String itemId;
        private String orderLineNo;
        private String quantity;
    }
}