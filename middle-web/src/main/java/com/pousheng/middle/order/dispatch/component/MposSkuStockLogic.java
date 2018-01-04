package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.MposSkuStockWriteService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/**
 * mpos商品库存操作
 * mpos仓发的商品的库存全部放在WarehouseSkuStock,和电商公用
 * 在发货单发货锁定库存时 先拉取当前发货仓及商品对应的库存
 * 1、当没有电商没有在售时mpos在售则先拉对应的库存到WarehouseSkuStock，然后再进行锁定
 * 2、电商在售时，则同步最新的库存到WarehouseSkuStock后，然后再进行锁定
 * 3、锁定后对应对应的商品进行尝试推电商最新库存
 *
 * 仓发释放库存则直接释放即可
 * 仓发扣减库存则直接扣减即可
 *
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
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;
    @Autowired
    private DispatchComponent dispatchComponent;



    /**
     * 锁定库存
     * @param dispatchOrderItemInfo 商品信息
     */
    public Response<Boolean> lockStock(DispatchOrderItemInfo dispatchOrderItemInfo){

        //门店发货
        List<ShopShipment> shopShipments = dispatchOrderItemInfo.getShopShipments();
        //锁定mpos门店库存
        Response<Boolean> updateStockRes = mposSkuStockWriteService.lockStockShop(shopShipments);
        if(!updateStockRes.isSuccess()){
            log.error("lock mpos sku stock for shopShipments:{} fail,error:{} ",shopShipments,updateStockRes.getError());
            return Response.fail(updateStockRes.getError());
        }

        //仓库发货
        List<WarehouseShipment> warehouseShipments = dispatchOrderItemInfo.getWarehouseShipments();
        List<String> skuCodes = dispatchComponent.getWarehouseSkuCodes(warehouseShipments);
        //1、先同步恒康最新库存到中台
        syncStock(warehouseShipments,skuCodes);
        //2、锁定库存
        if(!CollectionUtils.isEmpty(warehouseShipments)){
            //锁定电商库存
            Response<Boolean> response = warehouseSkuWriteService.lockStock(warehouseShipments);
            if(!response.isSuccess()){
                log.error("lock online warehouse sku stock:{} fail,error:{}",warehouseShipments,response.getError());
                return response;
            }

            //3、触发库存推送
            stockPusher.submit(skuCodes);
        }

        return Response.ok();
    }


    private void syncStock(List<WarehouseShipment> warehouseShipments,List<String> skuCodes){

        if(CollectionUtils.isEmpty(warehouseShipments)){
            return;
        }

        List<Long> warehouseIds = Lists.transform(warehouseShipments, new Function<WarehouseShipment, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable WarehouseShipment input) {
                return input.getWarehouseId();
            }
        });


        List<Warehouse> warehouses = warehouseAddressComponent.findWarehouseByIds(warehouseIds);

        //查询仓代码
        List<String> stockCodes = Lists.transform(warehouses, new Function<Warehouse, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Warehouse input) {
                return input.getCode();//todo 需要确认什么code
            }
        });

        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodes,skuCodes,2);
        if(CollectionUtils.isEmpty(skuStockInfos)){
            log.error("not find stock info by stockCodes:{} and skuCodes:{}",stockCodes,skuCodes);
            throw new ServiceException("query.hk.stock.fail");
        }

        List<StockDto> stockDtos = Lists.newArrayList();
        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos){

            List<HkSkuStockInfo.SkuAndQuantityInfo> skuAndQuantityInfos =hkSkuStockInfo.getMaterial_list();
            for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : skuAndQuantityInfos){
                StockDto stockDto = new StockDto();
                stockDto.setQuantity(skuAndQuantityInfo.getQuantity());
                stockDto.setSkuCode(skuAndQuantityInfo.getBarcode());
                stockDto.setUpdatedAt(new Date());
                stockDto.setWarehouseId(hkSkuStockInfo.getBusinessId());
                stockDtos.add(stockDto);
            }
        }

        Response<Boolean> r = warehouseSkuWriteService.syncStock(stockDtos);
        if(!r.isSuccess()){
            log.error("failed to sync stockDtos:{} stocks, error code:{}", stockDtos ,r.getError());
            throw new ServiceException(r.getError());
        }

    }



    /**
     * 解锁库存
     * @param dispatchOrderItemInfo 商品信息
     */
    public Response<Boolean> unLockStock(DispatchOrderItemInfo dispatchOrderItemInfo){

        //门店发货
        List<ShopShipment> shopShipments = dispatchOrderItemInfo.getShopShipments();
        //锁定mpos门店库存
        Response<Boolean> updateStockRes = mposSkuStockWriteService.unLockStockShop(shopShipments);
        if(!updateStockRes.isSuccess()){
            log.error("lock mpos sku stock for shopShipments:{} fail,error:{} ",shopShipments,updateStockRes.getError());
            return Response.fail(updateStockRes.getError());
        }

        //仓库发货
        List<WarehouseShipment> warehouseShipments = dispatchOrderItemInfo.getWarehouseShipments();

        return warehouseSkuWriteService.unlockStock(warehouseShipments);

    }


    /**
     * 减少库存
     * @param dispatchOrderItemInfo 商品信息
     */
    public Response<Boolean> decreaseStock(DispatchOrderItemInfo dispatchOrderItemInfo){

        //门店发货
        List<ShopShipment> shopShipments = dispatchOrderItemInfo.getShopShipments();
        //锁定mpos门店库存
        Response<Boolean> updateStockRes = mposSkuStockWriteService.unLockStockShop(shopShipments);
        if(!updateStockRes.isSuccess()){
            log.error("lock mpos sku stock for shopShipments:{} fail,error:{} ",shopShipments,updateStockRes.getError());
            return Response.fail(updateStockRes.getError());
        }

        //仓库发货
        List<WarehouseShipment> warehouseShipments = dispatchOrderItemInfo.getWarehouseShipments();

        return warehouseSkuWriteService.decreaseStock(warehouseShipments,warehouseShipments);
    }
}
