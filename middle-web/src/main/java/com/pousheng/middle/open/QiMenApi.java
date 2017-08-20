package com.pousheng.middle.open;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 奇门api实现
 * Created by cp on 8/19/17.
 */
@RestController
@RequestMapping("/api/qm")
@Slf4j
public class QiMenApi {

    @PostMapping(value = "/delivery-order")
    public void createDeliveryOrder(@RequestBody DeliveryOrderRequest request) {
        log.info("receive create delivery order request:{}", request);
        //TODO 校验签名
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
