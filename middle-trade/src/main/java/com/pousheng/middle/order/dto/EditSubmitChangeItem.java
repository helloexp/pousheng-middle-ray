package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/3/12
 * pousheng-middle
 */
@Data
public class EditSubmitChangeItem implements java.io.Serializable {
    private static final long serialVersionUID = 5499681004201604796L;
    //商品编码和数量 (换货)
    private String changeSkuCode;
    //数量 (换货)
    private Integer changeQuantity;
    //换货商品价格
    private Integer changeSkuPrice;
    //换货单需要发货的仓库id
    private Long exchangeWarehouseId;
    //换货单需要发货的仓库名称
    private String exchangeWarehouseName;
}
