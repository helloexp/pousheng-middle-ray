package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author caohao
 * Desc: 店铺中货品的库存分配规则Model类
 * Date: 2018-05-10
 */
@Data
public class WarehouseShopSkuStockRule implements Serializable {

    private static final long serialVersionUID = 3019054647264967216L;

    private Long id;

    /**
     * 店铺分配规则id
     */
    private Long shopRuleId;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * sku编号，也就是商品条码
     */
    private String skuCode;

    /**
     * 货号
     */
    private String materialId;

    /**
     * 货品名称，不存数据库
     */
    private String skuName;

    /**
     * 安全库存, 低于这个库存认为要推送的是0
     */
    private Long safeStock;

    /**
     * 库存分配比率, 以整数表示
     */
    private Integer ratio;

    /**
     * 虚拟库存，推送时加上，非必填
     */
    private Long jitStock;

    /**
     * 状态, 1 - 启用, -1 停用
     */
    private Integer status;

    /**
     * 上次推送数量
     */
    private Long lastPushStock;

    private Date createdAt;

    private Date updatedAt;
}
