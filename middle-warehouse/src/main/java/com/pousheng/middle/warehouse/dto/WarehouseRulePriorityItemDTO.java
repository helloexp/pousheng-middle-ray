package com.pousheng.middle.warehouse.dto;

import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import lombok.Data;

/**
 * 仓库模型转换成DTO
 *
 * @auther feisheng.ch
 * @time 2018/5/17
 */
@Data
public class WarehouseRulePriorityItemDTO {

    WarehouseRuleItemDto warehouse;

    WarehouseRulePriorityItem item;

    public WarehouseRuleItemDto getWarehouse() {
        return warehouse;
    }

    public WarehouseRulePriorityItemDTO warehouse(WarehouseRuleItemDto warehouse) {
        this.warehouse = warehouse;
        return this;
    }

    public WarehouseRulePriorityItem getItem() {
        return item;
    }

    public WarehouseRulePriorityItemDTO item(WarehouseRulePriorityItem item) {
        this.item = item;
        return this;
    }
}
