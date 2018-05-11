package com.pousheng.middle.warehouse.model;

import io.terminus.applog.annotation.LogMeId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项Model类
 * Date: 2017-06-07
 */
@Data
public class WarehouseRuleItem implements Serializable {

    private static final long serialVersionUID = -5829358460555504227L;

    @LogMeId
    private Long id;
    
    /**
     * 规则id
     */
    private Long ruleId;
    
    /**
     * 仓库id
     */
    private Long warehouseId;
    
    /**
     * 仓库名称
     */
    private String name;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    private Date createdAt;
    
    private Date updatedAt;
}
