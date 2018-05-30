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

            //判断仓类型,如果是店仓
            Warehouse warehouse = warehouseCacher.findById(warehouseId);

            Long lockedStock = 0L;
            //店仓则计算门店占用库存
            if(Objects.equals(WarehouseType.from(warehouse.getType()),WarehouseType.SHOP_WAREHOUSE)){

                String outCode = "";
                Map<String, String> warehouseExtra = warehouse.getExtra();
                if (Objects.nonNull(warehouseExtra)) {
                    outCode = warehouseExtra.get("outCode") != null ? warehouseExtra.get("outCode") : "";
                }
                String companyCode =  warehouse.getCompanyCode();
                try{
                    Shop shop = middleShopCacher.findByOuterIdAndBusinessId(outCode,Long.valueOf(companyCode));
                    lockedStock = dispatchComponent.getMposSkuShopLockStock(shop.getId(),skuCode);
                }catch (Exception e){
                    log.error("find shop sku stock failed,warehouse id is {},caused by {}",warehouseId,e.getMessage());

                }
            }

            Response<WarehouseSkuStock> r =  warehouseSkuReadService.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
            if(!r.isSuccess()){
                log.error("failed to find available stock for sku(code={}) in warehouse(id={}), error code:{}",
                        skuCode, warehouseId, r.getError());
            }else{
                if (Objects.nonNull(r.getResult())){
                    quantity= quantity + (r.getResult().getAvailStock()==null?0L:r.getResult().getAvailStock()) - lockedStock;
                }
            }
        }
        return quantity;
    }

}
