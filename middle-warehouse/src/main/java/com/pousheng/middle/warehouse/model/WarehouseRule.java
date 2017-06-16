package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述Model类
 * Date: 2017-06-07
 */
@Data
public class WarehouseRule implements Serializable {

    private static final long serialVersionUID = 2814989059726443700L;

    private Long id;
    
    /**
     * 规则描述, 按照优先级将各仓名称拼接起来
     */
    private String name;
    
    private Date createdAt;
    
    private Date updatedAt;
}