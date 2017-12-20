package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/19
 * pousheng-middle
 */
@Data
public class RefundFeeData implements java.io.Serializable{
    private static final long serialVersionUID = -6602632316897233160L;
    private String skuCode;
    private Integer applyQuantity;
}
