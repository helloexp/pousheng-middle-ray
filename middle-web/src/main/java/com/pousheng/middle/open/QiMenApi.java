package com.pousheng.middle.open;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 奇门api实现
 * Created by cp on 8/19/17.
 */
@RestController
@RequestMapping("/api/qm")
@Slf4j
public class QiMenApi {

    @PostMapping
    public String gateway(HttpServletRequest request) {
        log.info("receive request:{}", request);
        //TODO 校验签名
        return "<response> <flag>success</flag> <code>0</code> <message>invalid appkey</message> <createTime>2016-07-06 12:00:00</createTime> <deliveryOrderId>W1234</deliveryOrderId> <warehouseCode>W1345</warehouseCode> <logisticsCode>P2345</logisticsCode> <deliveryOrders> <deliveryOrder> <deliveryOrderId>C1234</deliveryOrderId> <warehouseCode>W789</warehouseCode> <logisticsCode>SF</logisticsCode> <orderLines> <orderLine> <orderLineNo>11</orderLineNo> <itemCode>I234</itemCode> <itemId>W234</itemId> <quantity>1</quantity> </orderLine> </orderLines> <createTime>2016-06-18 12:00:00</createTime> </deliveryOrder> </deliveryOrders> </response>";
    }

    @Data
    private static class DeliveryOrderRequest {
        private DeliveryOrder deliveryOrder;
    }

    @Data
    private static class DeliveryOrder {

        /**
         * 出库单号
         */
        private String deliveryOrderCode;

        /**
         * 收件人信息
         */
        private String receiverInfo;
    }

    @Data
    private static class ReceiverInfo {
        private String name;
        private String zipCode;
        private String tel;
        private String mobile;
        private String province;
        private String city;
        private String area;
        private String town;
        private String detailAddress;
    }

}
