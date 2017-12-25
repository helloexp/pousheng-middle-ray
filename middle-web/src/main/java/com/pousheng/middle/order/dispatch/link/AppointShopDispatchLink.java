package com.pousheng.middle.order.dispatch.link;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dispatch.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 指定门店发货规则
 * 优先级 1
 * 如果mpos订单指定了具体的门店发货，则直接整单派给该门店。
 * Created by songrenfei on 2017/12/22
 */
@Slf4j
public class AppointShopDispatchLink implements DispatchOrderLink{
    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        //初始化分派商品信息
        initDispatchOrderItemInfo(dispatchOrderItemInfo,context);

        Boolean isAppointShopDispatch = Boolean.TRUE;
        Long shopId = 1L;
        String shopName = "指定门店发货-门店";
        if(isAppointShopDispatch){
            ShopShipment shopShipment = new ShopShipment();
            shopShipment.setShopId(shopId);
            shopShipment.setShopName(shopName);
            shopShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            dispatchOrderItemInfo.getShopShipments().add(shopShipment);
            return Boolean.FALSE;
        }

        return true;
    }

    private void initDispatchOrderItemInfo(DispatchOrderItemInfo dispatchOrderItemInfo,Map<String, Serializable> context){

        dispatchOrderItemInfo = new DispatchOrderItemInfo();
        List<ShopShipment> shopShipments = Lists.newArrayList();
        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        dispatchOrderItemInfo.setShopShipments(shopShipments);
        dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
        context = Maps.newHashMap();
    }
}
