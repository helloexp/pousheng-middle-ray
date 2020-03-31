package com.pousheng.middle.web.order.component;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.hksyc.component.SyncYunJuJitShipmentApi;
import com.pousheng.middle.hksyc.dto.LogisticsInfo;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.hksyc.dto.YJSyncShipmentRequest;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 云聚jit订单缺货同步云聚
 *
 * @author zhurg
 * @date 2019/6/13 - 上午9:39
 */
@Slf4j
@Component
public class JitOutOfStockSyncYjHandler {

    @Autowired
    private SyncYunJuJitShipmentApi syncYunJuJitShipmentApi;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    /**
     * 同步缺货jit订单到云聚
     *
     * @param jitOutIfStockEvent
     */
    @Subscribe
    @AllowConcurrentEvents
    public void syncOutOfStockOrder2Yj(JitOutIfStockEvent jitOutIfStockEvent) {
        log.info("jit order {} JitOutIfStockEvent accept", jitOutIfStockEvent.getShopOrder().getOrderCode());
        ShopOrder shopOrder = jitOutIfStockEvent.getShopOrder();
        if (MiddleChannel.YUNJUJIT.getValue().equals(shopOrder.getOutFrom())) {
            List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrder.getId());
            List<LogisticsInfo> logisticsInfos = skuOrders.stream().map(skuOrder -> {
                LogisticsInfo logisticsInfo = new LogisticsInfo();
                //ERP的商品订单Id
                //logisticsInfo.setOrder_product_id(skuOrder.getOutId());
                //sku条码
                logisticsInfo.setBar_code(skuOrder.getSkuCode());
                logisticsInfo.setLogistics_company_code("PINJUN");
                logisticsInfo.setLogistics_order("0");
                logisticsInfo.setDelivery_name("缺货");
                logisticsInfo.setDelivery_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                logisticsInfo.setDelivery_address("缺货");
                logisticsInfo.setArrival_time(shopOrder.getExtra().get(ExtraKeyConstant.EXPECT_DATE));
                logisticsInfo.setDelivery_method(shopOrder.getExtra().get(ExtraKeyConstant.TRANSPORT_METHOD_CODE));
                logisticsInfo.setBox_no("0");
                logisticsInfo.setAmount(0);
                return logisticsInfo;
            }).collect(Collectors.toList());

            YJSyncShipmentRequest syncShipmentRequest = new YJSyncShipmentRequest();
            //订单号 外部订单号
            syncShipmentRequest.setOrder_sn(shopOrder.getOutId());
            syncShipmentRequest.setLogistics_info(logisticsInfos);
            Response<Boolean> response = syncOutOfStockOrder2Yj(syncShipmentRequest, shopOrder.getShopId());
            if (!response.isSuccess()) {
                log.error("jit订单 {} 缺货同步云聚失败!", shopOrder.getOrderCode(), response.getError());
                Map<String, Object> params = Maps.newHashMap();
                params.put("shopId", shopOrder.getShopId());
                if (null != syncShipmentRequest) {
                    params.put("syncShipmentRequest", JSON.toJSONString(syncShipmentRequest));
                }
                autoCompensateLogic.createAutoCompensationTask(params, TradeConstants.FAIL_SYNC_OUTOFSTOCK_JIT_ORDER_TO_YJ, response.getError());
            }
        }
    }

    /**
     * 云聚JIT单子校验WMS回传发货信息
     */
    public void checkWmsCallbackResult(YyEdiShipInfo yyEdiShipInfo) {
        log.info("JIT order WMS callback check start");
        try {
            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(yyEdiShipInfo.getShipmentId());
            if (null != shipment) {
                OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
                ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(orderShipment.getOrderCode());
                long count = yyEdiShipInfo.getItemInfos().stream().filter(itemInfo -> itemInfo.getQuantity() == 0)
                        .count();
                //整单sku数量都是0，整单发货失败
                if (count == yyEdiShipInfo.getItemInfos().size()) {
                    this.syncOutOfStockOrder2Yj(JitOutIfStockEvent.builder().shopOrder(shopOrder).build());
                }
            } else {
                log.error("JIT order WMS callback shipmentId {} not exists", yyEdiShipInfo.getShipmentId());
            }
        } catch (Exception e) {
            log.error("check jit order wms callback failed,cause: ", e);
        }
        log.info("JIT order WMS callback check end");
    }

    public Response<Boolean> syncOutOfStockOrder2Yj(YJSyncShipmentRequest syncShipmentRequest, Long shopId) {
        YJRespone yjRespone = syncYunJuJitShipmentApi.doSyncShipmentOrder(syncShipmentRequest, shopId);
        if (yjRespone.getError() == 0) {
            return Response.ok(Boolean.TRUE);
        }
        return Response.fail(yjRespone.getError_info());
    }

    /**
     * jit单子缺货时间
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class JitOutIfStockEvent implements Serializable {

        private static final long serialVersionUID = 5832257522058639573L;

        private ShopOrder shopOrder;
    }
}