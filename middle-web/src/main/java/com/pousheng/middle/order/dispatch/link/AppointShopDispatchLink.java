package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Objects;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.common.exception.ServiceException;
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

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        log.info("DISPATCH-AppointShopDispatchLink-1  order(id:{}) start...",shopOrder.getId());
        Map<String,String> extraMap = shopOrder.getExtra();
        if(!extraMap.containsKey(TradeConstants.IS_ASSIGN_SHOP)){
            log.error("shop order(id:{}) extra not key:{}",shopOrder.getId(),TradeConstants.IS_ASSIGN_SHOP);
            throw new ServiceException("query.assign.shop.fail");
        }
        String isAssignShop = extraMap.get(TradeConstants.IS_ASSIGN_SHOP);
        //1 代表指定门店发货
        if(Objects.equal(isAssignShop,"1")){
            Long shopId = Long.valueOf(extraMap.get(TradeConstants.ASSIGN_SHOP_ID));
            Shop shop = shopCacher.findShopById(shopId);
            ShopShipment shopShipment = new ShopShipment();
            shopShipment.setShopId(shopId);
            shopShipment.setShopName(shop.getName());
            shopShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            dispatchOrderItemInfo.getShopShipments().add(shopShipment);
            return Boolean.FALSE;
        }

        return true;
    }


}
