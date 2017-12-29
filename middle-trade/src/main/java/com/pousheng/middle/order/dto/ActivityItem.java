package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 */
@Data
public class ActivityItem extends BasicItemInfo implements Serializable {

    /**
     * spuId
     */
    private Long spuId;

    /**
     * 货号
     */
    private String materialCode;

    /**
     * 活动商品最低购买数量
     */
    private Integer quantity;

    /**
     * skuTemplateId
     */
    private Long skuTemplateId;
}
