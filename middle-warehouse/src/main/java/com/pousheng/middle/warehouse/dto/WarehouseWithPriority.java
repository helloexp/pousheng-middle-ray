package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@Data
public class WarehouseWithPriority implements Serializable {

    private static final long serialVersionUID = -4963664790313802806L;

    private Long warehouseId;

    private Long shopId;

    private Integer priority;
}
