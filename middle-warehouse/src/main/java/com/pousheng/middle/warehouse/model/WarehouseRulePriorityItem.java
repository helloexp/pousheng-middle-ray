package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: zhaoxiaowei
 * Desc: Model类
 * Date: 2018-09-04
 */
@Data
public class WarehouseRulePriorityItem implements Serializable {

    private static final long serialVersionUID = -270630312516480178L;

    private Long id;

    /**
     * 所属优先级规则
     */
    private Long priorityId;

    /**
     * 仓库id
     */
    private Long warehouseId;

    /**
     * 优先级
     */
    private Integer priority;

    private Date createdAt;

    private Date updatedAt;


}
