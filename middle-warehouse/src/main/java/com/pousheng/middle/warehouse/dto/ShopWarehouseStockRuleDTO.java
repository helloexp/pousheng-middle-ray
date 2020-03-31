package com.pousheng.middle.warehouse.dto;

import lombok.Data;

/**
 * @author caohao
 * Desc: 店铺中货品的库存分配规则Model类
 * Date: 2018-05-10
 */
@Data
public class ShopWarehouseStockRuleDTO {

    private ShopWarehouseStockRule shopWarehouseStockRule;

    private WarehouseDTO warehouseDTO;

    public ShopWarehouseStockRule getShopWarehouseStockRule() {
        return shopWarehouseStockRule;
    }

    public ShopWarehouseStockRuleDTO shopWarehouseStockRule(ShopWarehouseStockRule shopWarehouseStockRule) {
        this.shopWarehouseStockRule = shopWarehouseStockRule;
        return this;
    }

    public WarehouseDTO getWarehouseDTO() {
        return warehouseDTO;
    }

    public ShopWarehouseStockRuleDTO warehouseDTO(WarehouseDTO warehouseDTO) {
        this.warehouseDTO = warehouseDTO;
        return this;
    }
}
