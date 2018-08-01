package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.open.api.dto.CancelOutOrderInfo;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/1下午7:56
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.OUTER_ORDER_CANCEL_RESULT)
@Service
@Slf4j
public class CancelOrderService implements CompensateBizService {

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("CancelOrderService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("CancelOrderService.doProcess context is null");
            return;
        }
        CancelOutOrderInfo cancelOutOrderInfo = JsonMapper.nonEmptyMapper().fromJson(context, CancelOutOrderInfo.class);

        if (cancelOutOrderInfo == null) {
            log.warn("CancelOrderService.doProcess cancelOutOrderInfo is null");
            return;
        }
        try {
            doBiz(cancelOutOrderInfo);
        } catch (Exception e) {
            log.error("CancelOrderService forEach cancelOutOrderInfo ({}) is error: {}", context, Throwables.getStackTraceAsString(e));
        }
    }


    private void doBiz(CancelOutOrderInfo cancelOutOrderInfo) {
        String outId = cancelOutOrderInfo.getOutOrderId();
        String outFrom = cancelOutOrderInfo.getChannel();
        Response<Optional<ShopOrder>> findShopOrder = shopOrderReadService.findByOutIdAndOutFrom(outId, outFrom);
        if (!findShopOrder.isSuccess()) {
            log.error("fail to find shop order by outId={},outFrom={} when sync receiver info,cause:{}",
                    outId, outFrom, findShopOrder.getError());
            return;
        }
        Optional<ShopOrder> shopOrderOptional = findShopOrder.getResult();
        if (!shopOrderOptional.isPresent()) {
            log.error("shop order not found where outId={},outFrom=:{} when sync receiver info", outId, outFrom);
            return;
        }
        orderWriteLogic.autoCancelShopOrder(shopOrderOptional.get().getId());
    }
}
