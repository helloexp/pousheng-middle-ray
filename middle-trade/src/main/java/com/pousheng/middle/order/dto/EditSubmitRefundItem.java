package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/13
 * pousheng-middle
 */
@Data
public class EditSubmitRefundItem implements java.io.Serializable {
    private static final long serialVersionUID = 5366346151064699012L;
    //商品编码和数量 (退货)
    private String refundSkuCode;
    //数量 (退货)
    private Integer refundQuantity;
    //退货商品对应的换货商品集合

    //退款金额
    private Long fee;
}
