package com.pousheng.middle.open;

import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 计算某个店铺中某个sku的可用库存
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-19
 */
@Component
@Slf4j
public class AvailableStockCalc {

    @RpcConsumer
    private WarehouseShopStockRuleReadService shopStockRuleReadService;

    @RpcConsumer
    private WarehouseShopStockRuleWriteService shopStockRuleWriteService;

    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    public Integer availableStock(Long shopId, String skuCode){
        Response<List<Long>> rWarehouseIds = warehouseRuleReadService.findWarehouseIdsByShopId(shopId);
        if(!rWarehouseIds.isSuccess()){
            log.error("failed to find warehouseIds for shop(id={}), error code:{}",
                    shopId, rWarehouseIds.getError());
            throw new ServiceException(rWarehouseIds.getError());
        }
        for (Long warehouseId : rWarehouseIds.getResult()) {
            //todo: implement me
        }
        return null;
    }

}
