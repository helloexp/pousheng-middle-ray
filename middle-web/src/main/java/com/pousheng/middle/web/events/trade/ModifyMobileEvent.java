package com.pousheng.middle.web.events.trade;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/10
 * pousheng-middle
 */
@Data
public class ModifyMobileEvent implements java.io.Serializable {
    private static final long serialVersionUID = 2127239644732979748L;
    /**
     * 店铺订单主键
     */
    private Long shopOrderId;
}
