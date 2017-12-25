package com.pousheng.middle.order.dispatch.link;

import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 全国门店发货规则
 * 优先级 6
 * 1、调用roger的接口查询全国门店及门店下对应商品的库存
 * 2、过滤掉省内门店和已经拒过单的门店，过滤后如果没有可用的范围则进入下个规则
 * 3、判断是否有整单发货的门店，如果没有则进入下个规则，如果有则判断个数，如果只有一个则该门店发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个门店
 * Created by songrenfei on 2017/12/23
 */
public class AllShopDispatchlink implements DispatchOrderLink{
    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        return false;
    }
}
