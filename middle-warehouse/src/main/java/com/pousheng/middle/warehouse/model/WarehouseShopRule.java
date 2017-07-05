package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 店铺和发货规则的关联
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
@Data
public class WarehouseShopRule implements Serializable {

    private static final long serialVersionUID = 9019038647279671163L;

    /**
     * 主键id
     */
    private Long id;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 规则id
     */
    private Long ruleId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 修改时间
     */
    private Date updatedAt;
}
