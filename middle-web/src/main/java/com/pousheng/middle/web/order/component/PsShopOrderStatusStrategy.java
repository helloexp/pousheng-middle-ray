package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.strategy.ShopOrderStatusStrategy;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 找不到相应的店铺状态,默认返回-100
 * Created by tony on 2017/7/11.
 * pousheng-middle
 */
@Component
public class PsShopOrderStatusStrategy implements ShopOrderStatusStrategy {
    @Override
    public Integer status(List<SkuOrder> list) {
        if (list.size() > 0) {
            Set<Integer> skuOrderStatus = Lists.transform(list,SkuOrder::getStatus).stream().collect(Collectors.toSet());
            if (skuOrderStatus.size()==1){
                return list.get(0).getStatus();
            }else{
                //如果此时订单的状态不一致,有状态为负值的,过滤负值
                List<SkuOrder> listFilter = list.stream().filter(Objects::nonNull)
                        .filter(skuOrder ->(skuOrder.getStatus()!= MiddleOrderStatus.CANCEL.getValue()))
                        .filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.REFUND_APPLY_WAIT_SYNC_HK.getValue()))
                        .filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.REFUND_SYNC_HK_SUCCESS.getValue()))
                        .filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.REFUND.getValue()))
                        .filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.CANCEL_FAILED.getValue()))
                        .filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.REVOKE_FAILED.getValue())).collect(Collectors.toList());
                listFilter.sort((SkuOrder s1, SkuOrder s2) -> s1.getStatus().compareTo(s2.getStatus()));
                // 若存在正值的情况 则取正值最小的
                if (CollectionUtils.isNotEmpty(listFilter)) {
                    return listFilter.get(0).getStatus();
                }

                //若不存在正值的情况 则取排除JIT的-7后的最小值
                List<SkuOrder> filterList = list.stream().filter(Objects::nonNull)
                    .filter(skuOrder ->(skuOrder.getStatus()!= MiddleOrderStatus.JIT_STOCK_RELEASED.getValue()))
                    .collect(Collectors.toList());
                filterList.sort((SkuOrder s1, SkuOrder s2) -> s1.getStatus().compareTo(s2.getStatus()));
                if(CollectionUtils.isNotEmpty(filterList)){
                    return filterList.get(0).getStatus();
                }
            }

        }
        return -100;
    }

}
