package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.service.MposSkuStockWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 派单引擎
 * Created by songrenfei on 2017/12/27
 */
@Component
@Slf4j
public class DispatchOrderEngine {

    @Autowired
    private ApplicationContext applicationContext;
    @RpcConsumer
    private MposSkuStockWriteService mposSkuStockWriteService;
    @Autowired
    private StockPusher stockPusher;


    public Response<DispatchOrderItemInfo> toDispatchOrder(ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities){

        //因为这个的scope是prototype, 所以需要每次从容器中获取新实例
        DispatchLinkInvocation dispatchLinkInvocation = applicationContext.getBean(DispatchLinkInvocation.class);

        //初始化
        DispatchOrderItemInfo dispatchOrderItemInfo = new DispatchOrderItemInfo();
        List<ShopShipment> shopShipments = Lists.newArrayList();
        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        dispatchOrderItemInfo.setShopShipments(shopShipments);
        dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
        Map<String, Serializable> context = Maps.newHashMap();
        try {
            boolean success = dispatchLinkInvocation.applyDispatchs(dispatchOrderItemInfo, shopOrder,receiverInfo,skuCodeAndQuantities, context);

            if(success){
                log.info("dispatchOrderItemInfo: " ,dispatchOrderItemInfo);
                //锁定库存及更新电商在售库存（当mpos仓和电商仓交集时）
                Response<Boolean> lockRes = lockStock(dispatchOrderItemInfo);
                if(!lockRes.isSuccess()){
                    return Response.fail(lockRes.getError());
                }
                return Response.ok(dispatchOrderItemInfo);
            }
            log.error("order:{} not matching any dispatch link",shopOrder.getId());
            return Response.fail("dispatch.order.fail");

        }catch (Exception e){
            log.error("dispatch order:{} fail,cause:{}",shopOrder.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("dispatch.order.fail");
        }
    }

    private Response<Boolean> lockStock(DispatchOrderItemInfo dispatchOrderItemInfo){


        /**
         * 门店发货
         */
        List<ShopShipment> shopShipments = dispatchOrderItemInfo.getShopShipments();
        /**
         * 仓库发货
         */
        List<WarehouseShipment> warehouseShipments = dispatchOrderItemInfo.getWarehouseShipments();

        Response<Boolean> updateStockRes = mposSkuStockWriteService.lockStockShopAndWarehouse(shopShipments,warehouseShipments);
        if(!updateStockRes.isSuccess()){
            log.error("lock mpos sku stock for shopShipments:{} and warehouseShipments:{} fail,error:{} ",shopShipments,warehouseShipments,updateStockRes.getError());
            return Response.fail(updateStockRes.getError());
        }

        //触发库存推送
        List<String> skuCodes = Lists.newArrayList();
        List<Long> mposOnlineSaleWarehouseIds = dispatchOrderItemInfo.getMposOnlineSaleWarehouseIds();
        for (WarehouseShipment warehouseShipment : warehouseShipments) {
            //非电商仓的跳过
            if(!mposOnlineSaleWarehouseIds.contains(warehouseShipment.getWarehouseId())){
                continue;
            }
            for (SkuCodeAndQuantity skuCodeAndQuantity : warehouseShipment.getSkuCodeAndQuantities()) {
                skuCodes.add(skuCodeAndQuantity.getSkuCode());
            }
        }
        if(CollectionUtils.isEmpty(skuCodes)){
            stockPusher.submit(skuCodes);
        }

        return Response.ok();
    }

}
