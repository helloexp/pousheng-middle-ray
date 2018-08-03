package com.pousheng.middle.warehouse.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Data
public class RulePriorityItemCriteria extends PagingCriteria  {

    private Long priorityId;

    private String warehouseName;

    private Integer warehouseType;

    private String outCode;
}
