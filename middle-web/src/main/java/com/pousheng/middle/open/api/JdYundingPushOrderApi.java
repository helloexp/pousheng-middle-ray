package com.pousheng.middle.open.api;

import com.pousheng.middle.open.api.dto.ErpHandleShipmentResult;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.order.api.OrderReceiver;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 京东云鼎订单推送接口
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/15
 * pousheng-middle
 */
@OpenBean
@Slf4j
public class JdYundingPushOrderApi {
    @Autowired
    private OrderReceiver orderReceiver;
    @Autowired
    private OpenShopReadService openShopReadService;

    /**
     * 京东云鼎订单拉取服务
     */
    @OpenMethod(key = "jd.order.push.api", paramNames = {"shopId", "openClientFullOrders"}, httpMethods = RequestMethod.POST)
    public void pushJdOrder(Long shopId,
                            @NotEmpty(message = "openClientFullOrders.is.null") String openClientFullOrders) {
        log.info("JD-YUNDING-SYNC-ORDER-START shopId is {}, openClientFullOrders is:{} ", shopId, openClientFullOrders);
        //订单列表
        List<OpenClientFullOrder> openClientFullOrderList = JsonMapper.nonEmptyMapper()
                .fromJson(openClientFullOrders, JsonMapper.nonEmptyMapper()
                        .createCollectionType(List.class, OpenClientFullOrder.class));
        //查询店铺服务
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopId);
        if (!openShopResponse.isSuccess()) {
            log.error("find open shop failed,shopId is {},caused by {}", shopId, openShopResponse.getError());
            throw new OPServerException(200, openShopResponse.getError());
        }
        OpenClientShop openClientShop = OpenClientShop.from(openShopResponse.getResult());
        //订单处理
        orderReceiver.receiveOrder(openClientShop, openClientFullOrderList);

        log.info("JD-YUNDING-SYNC-ORDER-END");

    }
}

