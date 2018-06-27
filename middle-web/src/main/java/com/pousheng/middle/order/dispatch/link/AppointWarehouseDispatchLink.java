package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Objects;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.exception.ServiceException;
import io.terminus.open.client.constants.ParanaTradeConstants;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 指定仓库发货规则
 * 优先级 1
 * 如果mpos订单指定了具体的仓库发货，则直接整单派给该仓库。
 * Created by songrenfei on 2017/12/22
 */
@Component
@Slf4j
public class AppointWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        log.info("DISPATCH-AppointWarehouseDispatchLink-0  order(id:{}) start...",shopOrder.getId());
        Map<String,String> extraMap = shopOrder.getExtra();
        if(!extraMap.containsKey(ParanaTradeConstants.ASSIGN_WAREHOUSE_ID)|| Strings.isNullOrEmpty(ParanaTradeConstants.ASSIGN_WAREHOUSE_ID)){
            return true;
        }

        //公司别-内码
        String assignWarehouseId = extraMap.get(ParanaTradeConstants.ASSIGN_WAREHOUSE_ID);
        Warehouse warehouse = warehouseCacher.findByCode(assignWarehouseId);
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        warehouseShipment.setWarehouseId(warehouse.getId());
        warehouseShipment.setWarehouseName(warehouse.getName());
        warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
        dispatchOrderItemInfo.getWarehouseShipments().add(warehouseShipment);
        return Boolean.FALSE;
    }


}
