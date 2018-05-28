package com.pousheng.middle.web.warehouses.dto;

import com.pousheng.middle.warehouse.model.WarehouseRule;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2018/4/2
 */
@Data
public class WarehouseRuleDto implements Serializable {

    private static final long serialVersionUID = -1287253870437865877L;

    WarehouseRule warehouseRule;

    List<WarehouseRuleItemDto> warehouseRuleItemDtos;
}
