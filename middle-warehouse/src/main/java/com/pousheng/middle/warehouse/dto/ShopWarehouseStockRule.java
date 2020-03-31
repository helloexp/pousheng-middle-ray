package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: zhaoxiaowei
 * Desc: 店铺下的仓库的库存分配规则Model类
 * Date: 2018-09-11
 */
@Data
public class ShopWarehouseStockRule implements Serializable {

    private static final long serialVersionUID = 4708445769333608453L;

    private Long id;

    /**
     * 仓库id
     */
    private Long warehouseId;


    /**
     * 仓库名称
     */
    private String warehouseName;


    /**
     * 仓库编码
     */
    private String warehouseCode;

    /**
     * 店铺id
     */
    private Long shopId;


    /**
     * 店铺名称
     */
    private String shopName;


    /**
     * 店铺外码
     */
    private String outId;


    /**
     * 安全库存, 低于这个库存认为要推送的是0
     */
    private Long safeStock;

    /**
     * 虚拟库存
     */
    private Long jitStock;

    /**
     * 库存分配比率, 以整数表示
     */
    private Integer ratio;

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
