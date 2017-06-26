package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.dto.SelectedWarehouse;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuStockDao;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@Component
@Slf4j
public class WarehouseSkuStockManager {

    private final WarehouseSkuStockDao warehouseSkuStockDao;

    @Autowired
    public WarehouseSkuStockManager(WarehouseSkuStockDao warehouseSkuStockDao) {
        this.warehouseSkuStockDao = warehouseSkuStockDao;
    }

    @Transactional
    public void decreaseStock(List<SelectedWarehouse> warehouses){
        for (SelectedWarehouse selectedWarehouse : warehouses) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = selectedWarehouse.getSkuCodeAndQuantities();
            Long warehouseId = selectedWarehouse.getWarehouseId();

            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                boolean success = warehouseSkuStockDao.decreaseStock(warehouseId,
                        skuCode,
                        quantity);
                if(!success){
                    WarehouseSkuStock wss = this.warehouseSkuStockDao.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
                    if(wss == null){
                        log.error("no sku(skuCode={}) stock found in warehouse(id={})",skuCode, warehouseId );
                    }else {
                        log.error("insufficient sku stock(skuCode={}, required stock={}, actual stock={}) for warehouse(id={})",
                                skuCode, quantity,wss.getAvailStock(), warehouseId );

                    }
                    throw new ServiceException("insufficient sku stock");
                }
            }
        }
    }
}
