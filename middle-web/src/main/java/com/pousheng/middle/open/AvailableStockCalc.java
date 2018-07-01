package com.pousheng.middle.open;

import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.*;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @RpcConsumer
    private MposSkuStockReadService mposSkuStockReadService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private DispatchComponent dispatchComponent;

    private static final String WAREHOUSE_STATUS_ENABLED = "1";

    /**
     * 计算某个sku在指定店铺中的可用库存
     *
     * @param shopId 店铺id
     * @param skuCode sku编码
     * @return  可用库存
     */
    public Long availableStock(Long shopId, String skuCode, Long safeStock){
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

            //如果仓库非可用状态则不累加此仓库可用库存
            Warehouse warehouse = warehouseCacher.findById(warehouseId);
            if (warehouse == null){
                log.warn("failed to find warehouse(id={}) from warehouse cacher", warehouseId);
                continue;
            }
            if (!Objects.equals(Integer.valueOf(WAREHOUSE_STATUS_ENABLED),warehouse.getStatus())){
                continue;
            }

            Response<WarehouseSkuStock> r =  warehouseSkuReadService.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
            if (!r.isSuccess()){
                log.error("failed to find available stock for sku(code={}) in warehouse(id={}), error code:{}",
                        skuCode, warehouseId, r.getError());
            } else{
                if (Objects.nonNull(r.getResult())){
                    //quantity= quantity + (r.getResult().getAvailStock()==null?0L:r.getResult().getAvailStock()) - lockedStock;
                    //逐个仓库减去线上店铺的安全库存
                    long availStock = r.getResult().getAvailStock() == null ? 0L : r.getResult().getAvailStock();
                    if( availStock > safeStock ){
                        quantity = quantity +  availStock - safeStock;
                    }
                }
            }
        }
        return quantity;
    }

}
