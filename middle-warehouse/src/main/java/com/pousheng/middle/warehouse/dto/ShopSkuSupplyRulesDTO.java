package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/8/14
 */
@Data
public class ShopSkuSupplyRulesDTO implements Serializable {
    private static final long serialVersionUID = -1622919696284701902L;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 货品条码
     */
    private String skuCode;

    /**
     * 货号
     */
    private String materialCode;

    /**
     * 限制类型
     */
    private String type;

    /**
     * 规则状态
     */
    private String status;

    /**
     * 限制范围仓库ids
     */
    private List<Long> warehouseIds;
}
