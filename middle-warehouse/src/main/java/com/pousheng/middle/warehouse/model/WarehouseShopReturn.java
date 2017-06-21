package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库Model类
 * Date: 2017-06-21
 */
@Data
public class WarehouseShopReturn implements Serializable {

    private static final long serialVersionUID = 3112083070991083157L;
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
     * 仓库id
     */
    private Long warehouseId;
    
    /**
     * 仓库名称
     */
    private String warehouseName;
    
    private Date createdAt;
    
    private Date updatedAt;
}
