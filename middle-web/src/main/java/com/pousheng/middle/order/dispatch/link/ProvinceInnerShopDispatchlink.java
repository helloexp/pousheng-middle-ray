package com.pousheng.middle.order.dispatch.link;

import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 省内门店发货规则
 * 优先级 5
 * 1、查找用户收货地址所在的省
 * 2、查找省内的打了mpos标签的门店（过滤掉已经拒过单的门店），如果没有找到则进入下个规则
 * 3、调用roger的接口查询各个门店及门店下对应商品的库存
 * 4、判断是否有整单发货的门店，如果没有则进入下个规则（记录下省内的门店），如果有则判断个数，如果只有一个则该门店发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个门店
 * Created by songrenfei on 2017/12/23
 */
public class ProvinceInnerShopDispatchlink implements DispatchOrderLink{
    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        return false;
    }
}
