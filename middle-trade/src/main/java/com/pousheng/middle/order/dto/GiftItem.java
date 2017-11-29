package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 */
@Data
public class GiftItem extends BasicItemInfo implements Serializable {
    private Long skuId;
    private String outSKuId;
    /**
     * 赠品数量
     */
    private Integer quantity;
    private Long itemId;
    private String itemName;
    /**
     * 赠品标记的价格
     */
    private Integer price;

}
