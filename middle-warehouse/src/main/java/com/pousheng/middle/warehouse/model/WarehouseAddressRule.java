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
    private Long id;
    
    /**
     * 地址id
     */
    private Long addressId;
    
    /**
     * 地址类型, 1: 省, 2: 市
     */
    private Integer addressType;
    
    /**
     * 规则id
     */
    private Long ruleId;
    
    private Date createdAt;
    
    private Date updatedAt;
}
