package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况Model类
 * Date: 2017-06-07
 */
@Data
public class WarehouseSku implements Serializable {

    private static final long serialVersionUID = 7582078350954212292L;

    private Long id;
    
    /**
     * 仓库id
     */
    private Long warehouseId;
    
    /**
     * sku标识
     */
    private String skuCode;
    
    /**
     * 同步基准库存
     */
    private Long baseStock;
    
    /**
     * 当前可用库存
     */
    private Long availStock;
    
    /**
     * 当前锁定库存
     */
    private Long lockedStock;
    
    /**
     * 上次同步校准时间
     */
    private Date syncAt;
    
    private Date createdAt;
    
    private Date updatedAt;
}
