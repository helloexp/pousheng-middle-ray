package com.pousheng.middle.order.dispatch.component;

import com.google.common.collect.Lists;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.service.MposSkuStockWriteService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * mpos商品库存操作
 * =====先有电商仓再有mpos仓=====
 * ###占用库存
 * 1、非电商在售
 * mos仓对该商品的库存占用
 * 2、电商在售
 * 电商仓对该商品的库存进行占用
 * ###释放库存
 * 1、非电商在售
 * mos仓对该商品的库存占用释放
 * 2、电商在售
 * 电商仓对该商品的库存占用释放
 * ###扣减库存
 * 1、非电商在售
 * mos仓对该商品的库存占用释放
 * 2、电商在售
 * 电商仓对该商品的库存占用释放，并对可用库存进行扣减。（占用释放可用增加，可用再扣减）
 *
 * =====先有mpos仓再有电商仓=====
 * 1、商品A电商电商在售
 * 当恒康同步商品A的库存到中台时需要判断商品在对应的仓下是否有占用库存，有的话算可用库存时要把锁定库存算进去
 *
 * ======电商仓mpos仓都存在情况下，mpos仓取消了
 *
 *
 * ======电商仓mpos仓都存在情况下，电商仓取消了
 * Created by songrenfei on 2018/1/3
 */
@Component
@Slf4j
public class MposSkuStockLogic {

    @RpcConsumer
    private MposSkuStockWriteService mposSkuStockWriteService;
    @Autowired
    private StockPusher stockPusher;
    @Autowired
    private WarehouseSkuWriteService warehouseSkuWriteService;


    /**
     * 锁定库存
     * @param dispatchOrderItemInfo 商品信息
     */
    public Response<Boolean> lockStock(DispatchOrderItemInfo dispatchOrderItemInfo){

        //门店发货
        List<ShopShipment> shopShipments = dispatchOrderItemInfo.getShopShipments();
        //仓库发货
        List<WarehouseShipment> warehouseShipments = dispatchOrderItemInfo.getWarehouseShipments();

        //锁定mpos库存
        Response<Boolean> updateStockRes = mposSkuStockWriteService.lockStockShopAndWarehouse(shopShipments,warehouseShipments);
        if(!updateStockRes.isSuccess()){
            log.error("lock mpos sku stock for shopShipments:{} and warehouseShipments:{} fail,error:{} ",shopShipments,warehouseShipments,updateStockRes.getError());
            return Response.fail(updateStockRes.getError());
        }

        //触发库存推送
        List<String> skuCodes = Lists.newArrayList();
        List<Long> mposOnlineSaleWarehouseIds = dispatchOrderItemInfo.getMposOnlineSaleWarehouseIds();
        List<WarehouseShipment> onlineWarehouseShipments = Lists.newArrayListWithCapacity(mposOnlineSaleWarehouseIds.size());
        for (WarehouseShipment warehouseShipment : warehouseShipments) {
            //非电商仓的跳过
            if(!mposOnlineSaleWarehouseIds.contains(warehouseShipment.getWarehouseId())){
                continue;
            }
            for (SkuCodeAndQuantity skuCodeAndQuantity : warehouseShipment.getSkuCodeAndQuantities()) {
                skuCodes.add(skuCodeAndQuantity.getSkuCode());
            }
            onlineWarehouseShipments.add(warehouseShipment);
        }

        if(CollectionUtils.isEmpty(onlineWarehouseShipments)){
            //锁定电商库存
            Response<Boolean> response = warehouseSkuWriteService.lockStock(onlineWarehouseShipments);
            if(!response.isSuccess()){
                log.error("lock online warehouse sku stock:{} fail,error:{}",onlineWarehouseShipments,response.getError());
                return response;
            }

            //同步电商库存
            stockPusher.submit(skuCodes);
        }

        return Response.ok();
    }




    /**
     * 解锁库存
     * @param dispatchOrderItemInfo 商品信息
     */
    public void unLockStock(DispatchOrderItemInfo dispatchOrderItemInfo){

    }


    /**
     * 减少库存
     * @param dispatchOrderItemInfo 商品信息
     */
    public void decreaseStock(DispatchOrderItemInfo dispatchOrderItemInfo){

    }
}
