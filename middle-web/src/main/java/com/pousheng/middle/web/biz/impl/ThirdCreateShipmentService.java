/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: YyediSyncShipmentService
 * Author:   xiehong
 * Date:     2018/5/29 下午8:34
 * Description: yyedi回传发货信息业务处理
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.pousheng.middle.constants.CacheConsts;
import com.pousheng.middle.open.manager.RedisLockClient;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.Exception.ConcurrentSkipBizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Description: 天猫订单脱敏信息事件触发生成发货单
 * User:        liangyj
 * Date:        2018/5/31
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.THIRD_ORDER_CREATE_SHIP)
@Service
@Slf4j
public class ThirdCreateShipmentService implements CompensateBizService {

    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private MiddleOrderWriteService middleOrderWriteService;

    @Autowired
    private RedisLockClient redisLockClient;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("ThirdCreateShipmentService.doProcess params is null");
            return;
        }

        log.info("DO-PROCESS-ORDER-CREATE-SHIPMENT-BIZ START biz id: {}", poushengCompensateBiz.getId());
        Stopwatch stopwatch = Stopwatch.createStarted();

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("TmallCreateShipmentService.doProcess context is null");
            throw new BizException("ThirdCreateShipmentService.doProcess context is null");
        }
        Long shopOrderId = JsonMapper.nonEmptyMapper().fromJson(context, Long.class);
        if (shopOrderId == null) {
            log.warn("TmallCreateShipmentService.doProcess OpenClientOrderSyncEvent is null");
            throw new BizException("ThirdCreateShipmentService.doProcess OpenClientOrderSyncEvent is null");
        }

        try {
            this.onShipment(shopOrderId);
        } catch (ConcurrentSkipBizException be) {
            throw be;
        } catch (Exception e) {
            log.error("auto create shipment fail for order id:{},caused by {}",shopOrderId, Throwables.getStackTraceAsString(e));
            String message = MessageFormat.format("auto create shipment fail,caused by {0}", Throwables.getStackTraceAsString(e));
            throw new BizException(message, e);
        }

        stopwatch.stop();
        log.info("DO-PROCESS-ORDER-CREATE-SHIPMENT-BIZ END biz id: {},order id:{}, cost {} ms", poushengCompensateBiz.getId(),shopOrderId,stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * @Description TODO
     * @Date        2018/6/1
     * @param       shopOrderId
     * @return      
     */
    private void onShipment(Long shopOrderId) {
        log.info("try to auto create shipment,shopOrder id is {}", shopOrderId);
        //使用乐观锁更新操作
        Response<Boolean> updateHandleStatusR = middleOrderWriteService
                .updateHandleStatus(shopOrderId, String.valueOf(OrderWaitHandleType.WAIT_AUTO_CREATE_SHIPMENT.value()),
                        String.valueOf(OrderWaitHandleType.ORIGIN_STATUS_SAVE.value()));
        if (!updateHandleStatusR.isSuccess()) {
            log.info("update handle status failed,shopOrderId is {},caused by {}", shopOrderId, updateHandleStatusR.getResult());
            return;
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        log.info("auto create shipment,step one");
        //天猫订单如果还没有拉取售后地址是不能生成发货单的
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())) {
            if (shopOrder.getBuyerName().contains("**")) {
                return;
            }
        }
        //京东订单如果还没标记是否自提是不能生成发货单的
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())) {
            if (!shopOrder.getExtra().containsKey("is_customer_pick_up") || Objects.equals(shopOrder.getExtra().get("is_customer_pick_up"), "true")) {
                return;
            }
        }
        String key = MessageFormat.format(CacheConsts.ShipmentCacheKeys.SHIPPING_LOCK_KEY_PATTERN,
            String.valueOf(shopOrder.getOutId()));
        String ticket = UUID.randomUUID().toString();
        try {
            boolean locked = redisLockClient.lock(key, CacheConsts.TEN_MINUTES_LOCK_TTL, ticket);
            if (!locked) {
                throw new ConcurrentSkipBizException("order is shipping.so skip");
            }
            shipmentWiteLogic.autoHandleOrderForCreateOrder(shopOrder);
        } finally {
            redisLockClient.unlock(key, ticket);
        }
    }

}