package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联Model类
 * Date: 2017-06-07
 */
@Data
public class WarehouseAddressRule implements Serializable {

    private static final long serialVersionUID = 6678551853222246588L;
    /**
     * 主键
     */
    private Long id;
    
    /**
     * 地址id
     */
    private Long addressId;

    /**
     * 地址名称
     */
    private String addressName;

    /**
     * 规则id
     */
    private Long ruleId;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
