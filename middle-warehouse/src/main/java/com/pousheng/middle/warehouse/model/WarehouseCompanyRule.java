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
public class WarehouseCompanyRule implements Serializable {

    private static final long serialVersionUID = 3112083070991083157L;

    /**
     * 主键id
     */
    private Long id;

    /**
     * 公司编码
     */
    private String companyCode;

    /**
     * 公司名称
     */
    private String companyName;
    
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

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
