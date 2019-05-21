package com.pousheng.middle.open;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author bernie
 * @date 2019/5/14
 */
@Component
@Slf4j
public class PsAfterSaleReceiverHelper {

    @Autowired
    private OpenShopCacher openShopCacher;

    public  Boolean filterInitStatusWhenPullAfterSaleOrder(OpenClientAfterSale openClientAfterSale){

        OpenShop openShop = openShopCacher.findById(openClientAfterSale.getOpenShopId());
        String channel=openShop.getChannel();

        if(!Objects.equals(channel, MiddleChannel.SUNING.getValue()) && !Objects.equals(channel,MiddleChannel.TAOBAO.getValue())){
            return false;
        }
        if (Objects.nonNull(openShop.getExtra()) && Objects.nonNull(
            openShop.getExtra().get(TradeConstants.PULL_REFUND_EXCHANGE_FLAG_KEY))) {
            if (Objects.equals(openShop.getExtra().get(TradeConstants.PULL_REFUND_EXCHANGE_FLAG_KEY), TradeConstants.PULL_REFUND_EXCHANGE_FLAG_VALUE)) {
               return true;
            }
        }
        return false;
    }

    public Boolean isExpectedStatus(OpenClientAfterSale openClientAfterSale){
        switch (openClientAfterSale.getStatus()) {
            case WAIT_SELLER_CONFIRM_GOODS:
            case SUCCESS:
            case RETURN_CLOSED:
            case EXCHANGE_CLOSED:
            case EXCHANGE_SUCCESS:
                return true;
            default:
                return false;
        }
    }
}
