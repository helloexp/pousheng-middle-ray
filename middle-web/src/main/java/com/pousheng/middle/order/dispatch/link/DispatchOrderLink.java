package com.pousheng.middle.order.dispatch.link;

import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 派单链
 * Created by songrenfei on 2017/12/22
 */
public interface DispatchOrderLink {


    /**
     * 派单规则定义
     * @param dispatchOrderItemInfo 派单信息，经派单链执行后会封装出分派的商品信息，供生成对应的发货单
     * @param shopOrder 主订单信息
     * @param receiverInfo 收货地址
     * @param skuCodeAndQuantities sku及数量
     * @param context 参数上下文
     * @return 如果需要继续交给后面chain中的interceptor处理, 则返回true, 否则认为已经处理完毕, 返回false
     * @throws Exception 如果出现错误, 抛出异常
     */
    boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context)
            throws Exception;

}
