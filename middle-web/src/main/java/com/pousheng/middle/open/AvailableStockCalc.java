package com.pousheng.middle.open;

import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleWriteService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
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
    private WarehouseSkuReadService  warehouseSkuReadService;

    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    /**
     * 计算某个sku在指定店铺中的可用库存
     *
     * @param shopId 店铺id
     * @param skuCode sku编码
     * @return  可用库存
     */
    public Long availableStock(Long shopId, String skuCode){
        //首先通过店铺发货规则确定发货仓库
        Response<List<Long>> rWarehouseIds = warehouseRuleReadService.findWarehouseIdsByShopId(shopId);
        if(!rWarehouseIds.isSuccess()){
            log.error("failed to find warehouseIds for shop(id={}), error code:{}",
                    shopId, rWarehouseIds.getError());
            throw new ServiceException(rWarehouseIds.getError());
        }
        //累加店铺发货仓库中的可用库存
        Long quantity = 0L;
        for (Long warehouseId : rWarehouseIds.getResult()) {
            Response<WarehouseSkuStock> r =  warehouseSkuReadService.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
            if(!r.isSuccess()){
                log.error("failed to find available stock for sku(code={}) in warehouse(id={}), error code:{}",
                        skuCode, warehouseId, r.getError());
            }else{
                //获取mpos仓占用库存
                //Long lockStock = dispatchComponent.getMposSkuWarehouseLockStock(warehouseId,skuCode);
                //减去mpos仓商品占用库存
                quantity= quantity+ r.getResult().getAvailStock();
            }
        }
        return quantity;
    }

}
