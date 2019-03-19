package com.pousheng.middle.order.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/19
 * pousheng-middle
 */
@Data
public class RefundFeeData implements java.io.Serializable{
    private static final long serialVersionUID = -6602632316897233160L;
    private String skuCode;
    private String outSkuCode;
    private Integer applyQuantity;
    private Integer cleanPrice;

    public String getComplexSkuCode() {
        if (StringUtils.isEmpty(outSkuCode)) {
            return skuCode;
        }
        return skuCode + outSkuCode;
    }
}
