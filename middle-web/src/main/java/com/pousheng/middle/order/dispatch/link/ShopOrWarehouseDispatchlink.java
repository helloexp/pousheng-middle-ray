package com.pousheng.middle.order.dispatch.link;

import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 门店或仓 发货规则（最后一条规则，最复杂场景）
 * 优先级 7
 * 1、最少拆单
 * 2、先仓后端
 * 3、相同拆单情况下距离最短优先
 * 4、组合拆单情况下距离和最短优先
 * Created by songrenfei on 2017/12/23
 */
public class ShopOrWarehouseDispatchlink implements DispatchOrderLink{
    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        return false;
    }
}
