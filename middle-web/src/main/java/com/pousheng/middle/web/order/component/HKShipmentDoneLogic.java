package com.pousheng.middle.web.order.component;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.AfterSaleExchangeServiceRegistryCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientOrderShipment;
import io.terminus.open.client.order.service.OpenClientAfterSaleExchangeService;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 发货单发货完成后通知电商的方法
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/4
 * pousheng-middle
 */
@Slf4j
@Component
public class HKShipmentDoneLogic {
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    @Autowired
    private AfterSaleExchangeServiceRegistryCenter afterSaleExchangeServiceRegistryCenter;

    @Autowired
    private EcpOrderLogic ecpOrderLogic;

    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private OrderWriteService orderWriteService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteService refundWriteService;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @RpcConsumer
    private MiddleOrderWriteService middleOrderWriteService;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    public void doneShipment(Shipment shipment) {
        log.info("HK SHIPMENT DONE LISTENER start, shipmentId is {},shipmentType is {}", shipment.getId(), shipment.getType());
        //判断发货单是否发货完
        if (shipment.getType() == ShipmentType.SALES_SHIP.value()) {
            //判断发货单是否已经全部发货完成,如果全部发货完成之后需要更新order的状态为待收货
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            long orderShopId = orderShipment.getOrderId();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
            Flow flow = flowPicker.pickOrder();
            log.info("HK SHIPMENT id:{} DONE LISTENER shopOrderStatus is {}", shipment.getId(), shopOrder.getStatus());

            if (MiddleChannel.YUNJUJIT.getValue().equals(shopOrder.getOutFrom())) {
                //jit大单 整单发货，所以将所有子单合并处理即可
                Response<Boolean> response = middleOrderWriteService.updateOrderStatusForJit(shopOrder, MiddleOrderEvent.SHIP.toOrderOperation());
                if (!response.isSuccess()) {
                    log.error("update order ship error (id:{}),original status is {}", shopOrder.getId(), shopOrder.getStatus());
                    throw new JsonResponseException("update.order.status.error");
                }
            } else {
                //更新子订单中的信息
                List<Long> skuOrderIds = Lists.newArrayList();
                Map<Long, Integer> skuInfos = shipment.getSkuInfos();
                skuOrderIds.addAll(skuInfos.keySet());
                List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
                for (SkuOrder skuOrder : skuOrders) {
                    Response<SkuOrder> updateShippedRlt = orderWriteService.updateShippedNum(skuOrder.getId(), skuInfos.get(skuOrder.getId()));
                    if (!updateShippedRlt.isSuccess()) {
                        log.error("fail to update sku order shipped quantity by skuOrderId={},quantity={}, cause:{}",
                                skuOrder.getId(), skuInfos.get(skuOrder.getId()), updateShippedRlt.getError());
                        throw new JsonResponseException(updateShippedRlt.getError());
                    }

                    if (flow.operationAllowed(skuOrder.getStatus(), MiddleOrderEvent.SHIP.toOrderOperation())) {
                        skuOrder = updateShippedRlt.getResult();

                        // 是否部分取消，而且已经发货完成
                        boolean partialCancelAndDone = Objects.equals(skuOrder.getWithHold(), 0) && Objects.equals(skuOrder.getShipped(), skuOrder.getShipping());
                        // 如果未达到全部发货，则不更新子单状态
                        if (!partialCancelAndDone && skuOrder.getQuantity() > skuOrder.getShipped()) {
                            continue;
                        }
                        Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), MiddleOrderStatus.SHIPPED.getValue());
                        if (!updateRlt.getResult()) {
                            log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                            throw new JsonResponseException("update.sku.order.status.error");
                        }
                    }
                }

            }
            log.info("wait to notify ecp,shipmentId is {},shipmentType is {}", shipment.getId(), shipment.getType());
            //尝试同步发货信息到电商平台,如果有多个发货单，需要等到所有的发货单发货完成之后才会通知电商平台
            PoushengCompensateBiz biz = new PoushengCompensateBiz();
            biz.setBizType(PoushengCompensateBizType.SYNC_ECP.name());
            biz.setBizId(String.valueOf(shipment.getId()));
            biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
            compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
        }

        //丢件补发类型的发货单的类型是3，中台没有相应的枚举类
        if (shipment.getType() == ShipmentType.EXCHANGE_SHIP.value() || shipment.getType() == 3) {
            //如果发货单已经全部发货完成,需要更新refund表的状态为待确认收货,rufund表的状态为待收货完成,C
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            long afterSaleOrderId = orderShipment.getAfterSaleOrderId();
            List<OrderShipment> orderShipments = shipmentReadLogic.findByAfterOrderIdAndType(afterSaleOrderId);
            //获取该售后单下所有的发
            Refund refund = refundReadLogic.findRefundById(afterSaleOrderId);
            List<Integer> shipmentStatuses = orderShipments.stream().map(OrderShipment::getStatus).collect(Collectors.toList());
            if (shipmentStatuses.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                return;
            }
            if (refund.getStatus() == MiddleRefundStatus.WAIT_SHIP.getValue()) {
                Response<List<OrderShipment>> listResponse = orderShipmentReadService.findByAfterSaleOrderIdAndOrderLevel(afterSaleOrderId, OrderLevel.SHOP);
                List<Integer> orderShipMentStatusList = listResponse.getResult().stream().map(OrderShipment::getStatus).collect(Collectors.toList());
                log.info("HK SHIPMENT DONE LISTENER for change,orderShipMentStatusList is {}", orderShipMentStatusList);
                if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue()) &&
                        !orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_MPOS_RECEIVE.getValue())
                        && !orderShipMentStatusList.contains(MiddleShipmentsStatus.SYNC_HK_ING.getValue()) &&
                        !orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue())) {
                    Response<Boolean> resRlt = refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.SHIP.toOrderOperation());
                    if (!resRlt.isSuccess()) {
                        log.error("update refund status error (id:{}),original status is {}", refund.getId(), refund.getStatus());
                        throw new JsonResponseException("update.refund.status.error");
                    }
                    //将shipmentExtra的已发货时间塞入值,
                    Flow flow = flowPicker.pickAfterSales();
                    Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.SHIP.toOrderOperation());
                    RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                    refundExtra.setShipAt(new Date());
                    Map<String, String> extrMap = refund.getExtra();
                    extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    refund.setExtra(extrMap);
                    refund.setStatus(targetStatus);
                    Response<Boolean> updateRefundRes = refundWriteService.update(refund);
                    if (!updateRefundRes.isSuccess()) {
                        log.error("update refund(id:{}) fail,error:{}", refund, updateRefundRes.getError());
                        throw new JsonResponseException("update.refund.error");
                    }
                    //天猫换货单 需要反馈物流发货，调用 tmall.exchange.consigngoods
                    OpenShop openShop = openShopCacher.findById(orderShipment.getShopId());//根据店铺id查询店铺
                    Map<String, String> extraMap = openShop.getExtra();
                    if (Objects.equals(openShop.getChannel(), MiddleChannel.TAOBAO.getValue()) && extraMap.containsKey(TradeConstants.EXCHANGE_PULL) && Objects.equals(extraMap.get(TradeConstants.EXCHANGE_PULL), "Y")) {

                        OpenClientAfterSaleExchangeService afterSaleExchangeService = afterSaleExchangeServiceRegistryCenter.getAfterSaleExchangeService(openShop.getChannel());
                        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                        //运单号
                        String shipmentSerialNo = StringUtils.isEmpty(shipmentExtra.getShipmentSerialNo()) ? "" : Splitter.on(",").omitEmptyStrings().trimResults().splitToList(shipmentExtra.getShipmentSerialNo()).get(0);
                        //获取快递信息
                        ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
                        String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);
                        OpenClientOrderShipment openOrderShipment = new OpenClientOrderShipment();
                        openOrderShipment.setOuterOrderId(refund.getOutId().substring(refund.getOutId().indexOf("_") + 1));
                        openOrderShipment.setLogisticsType("200");//100表示平邮，200表示快递
                        openOrderShipment.setLogisticsCompany(expressCompanyCode);
                        openOrderShipment.setWaybill(shipmentSerialNo);
                        List<String> outerItemOrderIds = Lists.newArrayList();
                        List<String> outerSkuCodes = Lists.newArrayList();
                        for (ShipmentItem shipmentItem : shipmentItems) {
                            outerSkuCodes.add(shipmentItem.getOutSkuCode());
                            if (!StringUtils.isEmpty(shipmentItem.getSkuOutId())) {
                                outerItemOrderIds.add(shipmentItem.getSkuOutId());
                            }
                        }
                        openOrderShipment.setOuterSkuCodes(outerSkuCodes);
                        openOrderShipment.setOuterItemOrderIds(outerItemOrderIds);
                        log.info("notice taobao refund order ship (id:{}) shopId (shopId:{}) openOrderShipment (openOrderShipment:{})", refund.getId(), openShop.getId(), mapper.toJson(openOrderShipment));
                        Response<Boolean> result = afterSaleExchangeService.ship(openShop.getId(), openOrderShipment);
                        log.info("notice taobao refund order ship result (result:{})", mapper.toJson(result));
                        if (!result.isSuccess()) {
                            log.error("fail to notice taobao refund order ship (id:{})  ", refund.getId());
                            Map<String, Object> param2 = Maps.newHashMap();
                            param2.put("openShopId", openShop.getId());
                            param2.put("channel", openShop.getChannel());
                            param2.put("openOrderShipment", openOrderShipment);
                            autoCompensateLogic.createAutoCompensationTask(param2, TradeConstants.FAIL_REFUND_SHIP_TO_TMALL, result.getError());
                        }
                    }

                }
            }
            //丢件补发类型
            if (refund.getStatus() == MiddleRefundStatus.LOST_WAIT_SHIP.getValue()) {
                log.info("HK SHIPMENT DONE LISTENER for lost,shipmentId is {}", shipment.getId());
                Response<Boolean> resRlt = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.LOST_SHIPPED.toOrderOperation());
                if (!resRlt.isSuccess()) {
                    log.error("update refund status error (id:{}),original status is {}", refund.getId(), refund.getStatus());
                    throw new JsonResponseException("update.refund.status.error");
                }
                //将shipmentExtra的已发货时间塞入值,
                Flow flow = flowPicker.pickAfterSales();
                Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.LOST_SHIPPED.toOrderOperation());
                RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                refundExtra.setShipAt(new Date());
                Map<String, String> extrMap = refund.getExtra();
                extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                refund.setExtra(extrMap);
                refund.setStatus(targetStatus);
                Response<Boolean> updateRefundRes = refundWriteService.update(refund);
                if (!updateRefundRes.isSuccess()) {
                    log.error("update refund(id:{}) fail,error:{}", refund, updateRefundRes.getError());
                    throw new JsonResponseException("update.refund.error");
                }
            }
        }

        //真正扣减库存
        mposSkuStockLogic.decreaseStock(shipment);

        log.info("HK SHIPMENT DONE LISTENER end, shipmentId is {},shipmentType is {}", shipment.getId(), shipment.getType());

    }
}
