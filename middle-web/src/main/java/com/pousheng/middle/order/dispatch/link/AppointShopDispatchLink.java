package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.Warehouses4Address;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 指定门店发货规则
 * 优先级 1
 * 如果mpos订单指定了具体的门店发货，则直接整单派给该门店。
 * Created by songrenfei on 2017/12/22
 */
@Component
@Slf4j
public class AppointShopDispatchLink implements DispatchOrderLink{

    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;
    @RpcConsumer
    private WarehouseAddressRuleReadService warehouseAddressRuleReadService;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        log.info("DISPATCH-AppointShopDispatchLink-1  order(id:{}) start...",shopOrder.getId());
        Map<String,String> extraMap = shopOrder.getExtra();

        //全渠道订单不判断是否指定门店
        if (orderReadLogic.isAllChannelOpenShop(shopOrder.getShopId())){
            if(!extraMap.containsKey(TradeConstants.IS_ASSIGN_SHOP)){
                return true;
            }
        }
        if(!extraMap.containsKey(TradeConstants.IS_ASSIGN_SHOP)){
            log.error("shop order(id:{}) extra not key:{}",shopOrder.getId(),TradeConstants.IS_ASSIGN_SHOP);
            throw new ServiceException("query.assign.shop.fail");
        }
        String isAssignShop = extraMap.get(TradeConstants.IS_ASSIGN_SHOP);
        //1 代表指定门店发货
        if(extraMap.containsKey(TradeConstants.IS_ASSIGN_SHOP)&&Objects.equal(extraMap.get(TradeConstants.IS_ASSIGN_SHOP),"1")){
            Long shopId = Long.valueOf(extraMap.get(TradeConstants.ASSIGN_SHOP_ID));
            Shop shop = shopCacher.findShopById(shopId);
            ShopShipment shopShipment = new ShopShipment();
            shopShipment.setShopId(shopId);
            shopShipment.setShopName(shop.getName());
            shopShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            dispatchOrderItemInfo.getShopShipments().add(shopShipment);
            return Boolean.FALSE;
        }

        //查询当前店铺的派单仓范围

        List<Long> addressIds = Lists.newArrayListWithExpectedSize(3);
        Long currentAddressId =  Long.valueOf(receiverInfo.getCityId());
        addressIds.add(currentAddressId);
        while (currentAddressId > 1) {
            WarehouseAddress address = warehouseAddressCacher.findById(currentAddressId);
            addressIds.add(address.getPid());
            currentAddressId= address.getPid();
        }

        Response<List<Warehouses4Address>> r = warehouseAddressRuleReadService.findByReceiverAddressIds(shopOrder.getShopId(), addressIds);
        if (!r.isSuccess()) {
            log.error("failed to find warehouses for addressIds:{} of shop(id={}), error code:{}",
                    addressIds, shopOrder.getShopId(), r.getError());
            throw new ServiceException(r.getError());
        }

        context.put(DispatchContants.WAREHOUSE_FOR_ADDRESS, r.getResult().get(0));

        return true;
    }


}
