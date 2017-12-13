package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/13
 * pousheng-middle
 */
@Data
public class EditSubmitRefundItem implements java.io.Serializable {
    //商品编码和数量 (退货)
    private String refundSkuCode;
    //数量 (退货)
    private Integer refundQuantity;
    //商品编码和数量 (换货)
    private String changeSkuCode;
    //数量 (换货)
    private Integer changeQuantity;
    //退款金额
    private Long fee;
}
