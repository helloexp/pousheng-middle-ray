package com.pousheng.middle.order.enums;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/3/7
 * pousheng-middle
 */
@Data
public class MiddleAfterSaleInfo {
    //可以申请售后的订单或者售后单id
    private Long id;
    //异议申请售后的订单或者售后单的code
    private String code;
    //id的类型(1.订单;2.售后单)
    private Integer type;
    //订单或者售后单说明
    private String desc;
}
