package com.pousheng.middle.open.qimen;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

/**
 * Created by cp on 8/22/17.
 */
@XStreamAlias("request")
@Data
public class DeliveryOrderCreateRequest {

    @XStreamAlias("deliveryOrder")
    private DeliveryOrder deliveryOrder;

    @XStreamAlias("extendProps")
    private ExtendProps extendProps;

    @Data
    public static class DeliveryOrder {

        /**
         * 出库单号
         */
        @XStreamAlias("deliveryOrderCode")
        private String deliveryOrderCode;

        /**
         * 收件人信息
         */
        @XStreamAlias("receiverInfo")
        private ReceiverInfo receiverInfo;
    }

    @Data
    public static class ReceiverInfo {

        @XStreamAlias("name")
        private String name;

        @XStreamAlias("zipCode")
        private String zipCode;

        @XStreamAlias("tel")
        private String tel;

        @XStreamAlias("mobile")
        private String mobile;

        @XStreamAlias("province")
        private String province;

        @XStreamAlias("city")
        private String city;

        @XStreamAlias("area")
        private String area;

        @XStreamAlias("town")
        private String town;

        @XStreamAlias("detailAddress")
        private String detailAddress;
    }

    @Data
    public static class ExtendProps{

        @XStreamAlias("wmsAppKey")
        private String wmsAppKey;

        @XStreamAlias("wmsSign")
        private String wmsSign;

    }

}
