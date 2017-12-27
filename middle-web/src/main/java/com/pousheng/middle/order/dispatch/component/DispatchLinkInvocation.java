/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.pousheng.middle.order.dispatch.component;

import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dispatch.link.DispatchOrderLink;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author:  songrenfei
 * Date: 2017-12-10
 */
@Component
@Scope("prototype")
public class DispatchLinkInvocation {

    @Autowired
    private  DispatchOrderChain dispatchOrderChain;

    private int interceptorIndex = -1;
    /**
     * 派单规则定义
     * @param dispatchOrderItemInfo 派单信息，经派单链执行后会封装出分派的商品信息，供生成对应的发货单
     * @param shopOrder 主订单信息
     * @param receiverInfo 收货地址
     * @param skuCodeAndQuantities sku及数量
     * @param context 参数上下文
     * @return 返回true, 则认为已经处理完毕, 如果需要继续交给后面chain中的interceptor处理(没有派完), 则返回false
     * @throws Exception 如果出现错误, 抛出异常
     */
    public boolean applyDispatchs(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        for (int i = 0; i<getDispatchOrderLinks().size(); i++) {
            this.interceptorIndex = i;
            DispatchOrderLink interceptor = getDispatchOrderLinks().get(i);
            if (!interceptor.dispatch(dispatchOrderItemInfo, shopOrder,receiverInfo,skuCodeAndQuantities, context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理null的情况
     * @return 拦截器列表
     */
    private List<DispatchOrderLink> getDispatchOrderLinks(){
        if(dispatchOrderChain!=null){
            return dispatchOrderChain.getDispatchOrderLinks();
        }else{
            return Collections.emptyList();
        }
    }
}
