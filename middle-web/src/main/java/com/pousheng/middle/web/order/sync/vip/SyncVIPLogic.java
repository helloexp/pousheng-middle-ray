package com.pousheng.middle.web.order.sync.vip;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.PsOrderReceiver;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.VipWarehouseMapping;
import com.pousheng.middle.warehouse.service.VipWarehouseMappingReadService;
import com.pousheng.middle.warehouse.service.VipWarehouseMappingWriteService;
import com.pousheng.middle.web.events.warehouse.PushEvent;
import com.pousheng.middle.web.item.cacher.VipWarehouseMappingProxy;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.warehouses.ShopSkuStockPushHandler;
import com.vip.vop.omni.logistics.LogisticsTrackResponse;
import com.vip.vop.omni.logistics.Order;
import com.vip.vop.omni.logistics.Package;
import com.vip.vop.omni.logistics.TraceInfo;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.vip.dto.StoreMapping;
import io.terminus.open.client.vip.enums.TransportCodeEnum;
import io.terminus.open.client.vip.extra.service.VipOrderReturnService;
import io.terminus.open.client.vip.extra.service.VipOrderStoreService;
import io.terminus.open.client.vip.extra.service.VipStoreService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.dto.ExpressDetails;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentExpressStatus;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShipmentWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import vipapis.delivery.RefuseGoods;
import vipapis.delivery.ReturnGoods;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通知vip接单 呼叫快递
 *
 * @author zhaoxw
 * @date 2018/9/26
 */

@Slf4j
@Component
public class SyncVIPLogic {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private VipOrderStoreService vipOrderStoreService;

    @RpcConsumer
    private VipStoreService vipStoreService;

    @Autowired
    private RefundReadLogic refundReadLogic;

    @Autowired
    private RefundWriteLogic refundWriteLogic;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @RpcConsumer
    private VipOrderReturnService vipOrderReturnService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    @Autowired
    private WarehouseRulesClient warehouseRulesClient;

    @RpcConsumer
    private VipWarehouseMappingReadService vipWarehouseMappingReadService;

    @RpcConsumer
    private VipWarehouseMappingWriteService vipWarehouseMappingWriteService;

    @RpcConsumer
    private ExpressCodeReadService expressCodeReadService;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private ShopSkuStockPushHandler shopSkuStockPushHandler;

    @Autowired
    private VipWarehouseMappingProxy vipWarehouseMappingProxy;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    private final static String UNDERCARRIAGE_CODE = "111111111111";

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    @Autowired
    private ShopCacher shopCacher;

    @Autowired
    private PsOrderReceiver orderReceiver;

    /**
     * 通知vip接单并呼叫快递
     *
     * @param shipment
     * @return
     */
    public Response<Boolean> syncOrderStoreToVIP(Shipment shipment, String mailNo) {
        log.debug("VIP-OXO-syncOrderStoreToVIP,params:shipment={}", shipment.toString());
        try {
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            Long warehouseId;
            //判断发货单是仓发还是店发
            if (Objects.equals(shipment.getShipWay(), 2)) {
                warehouseId = shipment.getShipId();
            } else {
                Shop shop = shopCacher.findShopById(shipmentExtra.getWarehouseId());
                warehouseId = warehouseCacher.findByOutCodeAndBizId(shop.getOuterId(), shop.getBusinessId().toString()).getId();
            }
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            Response<ShopOrder> orderResp = shopOrderReadService.findById(orderShipment.getOrderId());
            ShopOrder shopOrder = orderResp.getResult();
            Response<Boolean> response = vipOrderStoreService.responseOrderStore(shipment.getShopId(), shopOrder.getOutId(), vipWarehouseMappingProxy.findByWarehouseId(warehouseId));
            if (!response.isSuccess()) {
                log.error("fail to order store , shipmentId:{} fail,error:{}", shipment.getId(), response.getError());
                return Response.fail(response.getError());
            }
            Response<Boolean> deliveryResp = vipOrderStoreService.confirmStoreDelivery(shipment.getShopId(), shopOrder.getOutId(), vipWarehouseMappingProxy.findByWarehouseId(warehouseId), mailNo, null);
            if (!deliveryResp.isSuccess()) {
                log.error("fail to order store , shipmentId:{} fail,error:{}", shipment.getId(), deliveryResp.getError());
                return Response.fail(deliveryResp.getError());
            }
            //如果是仓发，呼叫成功更新为同步成功
            if (shipment.getShipWay() == 2) {
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            }
        } catch (Exception e) {
            log.error("fail to order store , shipmentId:{} fail,error:{}", shipment.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("sync.order.store.to.vip.fail");
        }
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 呼叫vip快递
     *
     * @param shipmentId
     * @param mailNo
     * @return
     */
    public Response<Boolean> confirmStoreDelivery(Long shipmentId, String mailNo) {
        log.debug("VIP-OXO-confirmStoreDelivery,params:shipmentId={},mailNo={}", shipmentId, mailNo);
        try {
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            Long warehouseId;
            //判断发货单是仓发还是店发
            if (Objects.equals(shipment.getShipWay(), 2)) {
                warehouseId = shipment.getShipId();
            } else {
                Shop shop = shopCacher.findShopById(shipmentExtra.getWarehouseId());
                warehouseId = warehouseCacher.findByOutCodeAndBizId(shop.getOuterId(), shop.getBusinessId().toString()).getId();
            }
            WarehouseDTO warehouseDTO = warehouseCacher.findById(warehouseId);
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            Response<ShopOrder> orderResp = shopOrderReadService.findById(orderShipment.getOrderId());
            ShopOrder shopOrder = orderResp.getResult();
            Response<Boolean> deliveryResp = vipOrderStoreService.confirmStoreDelivery(shipment.getShopId(), shopOrder.getOutId(), vipWarehouseMappingProxy.findByWarehouseId(warehouseDTO.getId()), mailNo, null);
            if (!deliveryResp.isSuccess()) {
                log.error("fail to order store , shipmentId:{} fail,error:{}", shipment.getId(), deliveryResp.getError());
                return Response.fail(deliveryResp.getError());
            }
        } catch (Exception e) {
            log.error("fail to order store , shipmentId:{} fail,error:{}", shipmentId, Throwables.getStackTraceAsString(e));
            return Response.fail("sync.order.store.to.vip.fail");
        }
        return Response.ok(Boolean.TRUE);


    }


    /**
     * 呼叫vip快递
     *
     * @param outId 外部订单号
     * @param
     * @return
     */
    public Response<Boolean> confirmUndercarriage(Long shopId, String outId) {
        log.debug("VIP-OXO-confirmUndercarriage,params:shopId={},outId={}", shopId, outId);
        try {
            Response<Boolean> deliveryResp = vipOrderStoreService.confirmStoreDelivery(shopId, outId, UNDERCARRIAGE_CODE, null, null);
            if (!deliveryResp.isSuccess()) {
                log.error("fail to notice under store , outId:{} fail,error:{}", outId, deliveryResp.getError());
                return Response.fail(deliveryResp.getError());
            }
        } catch (Exception e) {
            log.error("fail to notice under store , outId:{} fail,error:{}", outId, Throwables.getStackTraceAsString(e));
            return Response.fail("sync.undercarriage.to.vip.fail");
        }
        return Response.ok(Boolean.TRUE);
    }


    public Response<Boolean> confirmReturnResult(Refund refund) {
        log.debug("VIP-OXO-confirmReturnResult,params:{}", refund.toString());
        Response<Boolean> response = Response.ok(Boolean.TRUE);
        try {
            Response<ShopOrder> orderResp = shopOrderReadService.findByOrderCode(refund.getReleOrderCode());
            ShopOrder shopOrder = orderResp.getResult();
            if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIPOXO.getValue())) {
                log.error("this order is not from vip :{} ", refund.getId());
                return Response.fail("order.is.not.from.vip");
            }

            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
            ExpressCode expressCode=makeExpressNameByName(refundExtra.getShipmentCorpName());
            if (refund.getRefundType().equals(MiddleRefundType.AFTER_SALES_RETURN.value())) {
                List<ReturnGoods> goodsList = Lists.newArrayList();
                for (RefundItem refundItem : refundItems) {
                    ReturnGoods goods = new ReturnGoods();
                    goods.setAmount(refundItem.getFinalRefundQuantity());
                    goods.setProduct_name(refundItem.getSkuName());
                    goods.setBarcode(refundItem.getSkuCode());
                    goodsList.add(goods);
                }
                response= vipOrderReturnService.confirmReturnResult(refund.getShopId(), shopOrder.getOutId(), expressCode.getVipCode(), refundExtra.getShipmentCorpName(), refundExtra.getShipmentSerialNo(), refund.getBuyerNote(), goodsList);
            }
            if (refund.getRefundType().equals(MiddleRefundType.REJECT_GOODS.value())) {
                List<RefuseGoods> goodsList = Lists.newArrayList();
                for (RefundItem refundItem : refundItems) {
                    RefuseGoods goods = new RefuseGoods();
                    goods.setAmount(refundItem.getQuantity());
                    goods.setBarcode(refundItem.getSkuCode());
                    goodsList.add(goods);
                }
                response= vipOrderReturnService.confirmRefuseResult(refund.getShopId(), shopOrder.getOutId(), expressCode.getVipCode(), refundExtra.getShipmentCorpName(), refundExtra.getShipmentSerialNo(), refund.getBuyerNote(), goodsList);
            }
            Refund update = new Refund();
            update.setId(refund.getId());
            Map<String, String> extraMap = refund.getExtra();
            extraMap.put(TradeConstants.VIP_REFUND_SYNC_FLAG, response.isSuccess() ? "1" : "0");
            update.setExtra(extraMap);
            Response<Boolean> updateRefundRes = refundWriteLogic.update(update);
            if (!updateRefundRes.isSuccess()) {
                log.error("update refund(id:{}) fail,error:{}", update, updateRefundRes.getError());
                throw new JsonResponseException("update.refund.error");
            }
        } catch (Exception e) {
            log.error("fail to confirm refund , refundId:{} fail,error:{}", refund.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("sync.order.store.to.vip.fail");
        }

        return response;
    }

    public ExpressCode makeExpressNameByName(String name) {
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        criteria.setName(name);
        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (response.getResult().getData().size() == 0) {
            log.error("there is not any express info by name:{}", name);
            throw new JsonResponseException("express.info.is.not.exist");
        }
        return response.getResult().getData().get(0);
    }


    public Response<Boolean> getOrderLogisticsTrack(Long shopId, List<ShopOrder> shopOrders) {
        log.debug("VIP-OXO-getOrderLogisticsTrack,params:shopId={},shopOrders={}", shopId, shopOrders);
        List<String> orderIds = shopOrders.stream().map(e -> e.getOutId()).collect(Collectors.toList());
        try {
            Map<String, Long> orderMap = shopOrders.stream().collect(Collectors.toMap(ShopOrder::getOutId, ShopOrder::getId));
            Response<LogisticsTrackResponse> trackResp = vipOrderStoreService.getOrderLogisticsTrack(shopId, orderIds);
            if (!trackResp.isSuccess()) {
                log.error("fail to query order logistic track , shopId:{} ,orderIds:{} fail,error:{}", shopId, orderIds, trackResp.getError());
                return Response.fail(trackResp.getError());
            }
            log.debug("VIP-OXO-getOrderLogisticsTrack,resp:{}", trackResp.toString());
            List<Order> orders = trackResp.getResult().getOrders();
            for (Order order : orders) {
                if (CollectionUtils.isEmpty(order.getPackages())) {
                    continue;
                }
                List<Shipment> list = shipmentReadLogic.findByShopOrderId(orderMap.get(order.getOrder_id())).stream().filter(e->e.getStatus().equals(MiddleShipmentsStatus.SHIPPED.getValue())).collect(Collectors.toList());
                if(CollectionUtils.isEmpty(list)){
                    continue;
                }
                Package aPackage = order.getPackages().get(0);
                Shipment shipment = list.get(0);
                Response<ShipmentExpress> shipmentExpressRes = shipmentReadService.findShipmentExpress(shipment.getShipmentCode(), aPackage.getTransport_no());
                if (!shipmentExpressRes.isSuccess()) {
                    continue;
                }
                Map<String, String> extraMap = Maps.newHashMap();
                List<ExpressDetails> expressDetails = Lists.newArrayList();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    //若最新的物流信息更新时间小于当前时间 则忽略此次拉取结果
                    if (shipmentExpressRes.getResult() != null && sdf.parse(aPackage.getTraceInfos().get(0).getTransport_time()).before(shipmentExpressRes.getResult().getUpdatedAt())) {
                        continue;
                    }
                } catch (Exception e) {
                    log.info("fail to compare date  cause by  {}", e);
                }
                // 拿到最后新一条状态作为物流当前状态
                String status = aPackage.getTraceInfos().get(0).getTransport_code();
                //遍历唯品会的物流信息
                for (TraceInfo traceInfo : aPackage.getTraceInfos()) {
                    ExpressDetails expressDetail = new ExpressDetails();
                    expressDetail.setNodeAt(traceInfo.getTransport_time());
                    expressDetail.setNodeInfo(traceInfo.getTransport_detail());
                    expressDetails.add(expressDetail);
                }
                extraMap.put(TradeConstants.SHIPMENT_EXPRESS_NODE_DETAILS, JSON_MAPPER.toJson(expressDetails));
                //若物流信息不存在 则创建 同时更新物流单号
                if (shipmentExpressRes.getResult() != null) {
                    log.info("start to update express info for shipment code:{} by data:{}", shipment.getShipmentCode(), aPackage);
                    ShipmentExpress shipmentExpress = shipmentExpressRes.getResult();
                    shipmentExpress.setExpressStatus(transToShipmentExpressStatus(status));
                    shipmentExpress.setExtra(extraMap);
                    shipmentWriteService.updateExpressInfo(shipmentExpress);
                } else {
                    Shipment update = new Shipment();
                    update.setId(shipment.getId());
                    update.setShipmentSerialNo(aPackage.getTransport_no());
                    ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                    shipmentExtra.setShipmentSerialNo(aPackage.getTransport_no());
                    Map<String, String> extra = shipment.getExtra();
                    extra.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
                    update.setExtra(extra);
                    Response<Boolean> updateShipmentRes = shipmentWriteService.update(update);
                    if (!updateShipmentRes.isSuccess()) {
                        log.error("fail to update shipment express to :{},error:{}", aPackage.getTransport_no(), updateShipmentRes.getError());
                        continue;
                    }
                    log.info("start to create express info for shipment code:{} by data:{}", shipment.getShipmentCode(), aPackage);
                    ShipmentExpress shipmentExpress = new ShipmentExpress();
                    shipmentExpress.setShipmentCode(shipment.getShipmentCode());
                    shipmentExpress.setExpressNo(aPackage.getTransport_no());
                    shipmentExpress.setExpressStatus(transToShipmentExpressStatus(status));
                    shipmentExpress.setExpressCompanyCode(shipment.getShipmentCorpCode());
                    shipmentExpress.setExpressCompanyName(aPackage.getCarrier_name());
                    shipmentExpress.setExtra(extraMap);
                    shipmentWriteService.createExpressInfo(shipmentExpress);
                }
                //若状态为已收货 或 拒收 则要更新并通知
                if (status.equals(TransportCodeEnum.SIGN_FOR.value()) || status.equals(TransportCodeEnum.REFUSED.value())) {
                    confirmOrder(orderMap.get(order.getOrder_id()));
                }
            }

        } catch (Exception e) {
            log.error("fail to query order logistic track , shopId:{} ,orderIds:{} fail,error:{}", shopId, orderIds, Throwables.getStackTraceAsString(e));
            return Response.fail("sync.order.logistic.track.fail");
        }
        return Response.ok(Boolean.TRUE);
    }


    private void confirmOrder(Long orderId) {
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(orderId, MiddleOrderStatus.SHIPPED.getValue());
        if (skuOrders.size() == 0) {
            return;
        }
        for (SkuOrder skuOrder : skuOrders) {
            Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), MiddleOrderStatus.SHIPPED.getValue(), MiddleOrderStatus.CONFIRMED.getValue());
            if (!updateRlt.getResult()) {
                log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
            }
        }
        //判断订单的状态是否是已完成
        orderReceiver.noticeConfirm(orderId);
    }

    private Integer transToShipmentExpressStatus(String status) {
        TransportCodeEnum transportCodeEnum = TransportCodeEnum.from(status);
        switch (transportCodeEnum) {
            case TAKE_OUT:
                return ShipmentExpressStatus.COLLECTED.value();
            case EN_ROUTE:
                return ShipmentExpressStatus.DISTRIBUTING.value();
            case SIGN_FOR:
                return ShipmentExpressStatus.RETURNED.value();
            case REFUSED:
                return ShipmentExpressStatus.REJECTED.value();

        }
        return null;
    }


    public void syncWarehouseMapping(Long shopId) {
        Response<List<Long>> rWarehouseIds = warehouseRulesClient.findWarehouseIdsByShopId(shopId);
        if (!rWarehouseIds.isSuccess()) {
            log.error("find warehouse list by shopId fail: shopId: {}, caused: {}", shopId, rWarehouseIds.getError());
        }
        Response<List<VipWarehouseMapping>> response = vipWarehouseMappingReadService.findAll();
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        Map<Long, VipWarehouseMapping> warehouseMap = response.getResult().stream().collect(Collectors.toMap(VipWarehouseMapping::getWarehouseId, vipWarehouseMapping -> vipWarehouseMapping));
        List<Long> storeList = Lists.newArrayList();
        List<StoreMapping> queryList = vipStoreService.queryStroe(shopId);
        Boolean needRefresh = Boolean.FALSE;
        //新的映射则创建
        for (StoreMapping storeMapping : queryList) {
            WarehouseDTO warehouseDTO = warehouseCacher.findByOutCodeAndBizId(storeMapping.getOuterCode(), storeMapping.getCompanyId());
            storeList.add(warehouseDTO.getId());
            if (warehouseMap.get(warehouseDTO.getId()) == null) {
                VipWarehouseMapping vipWarehouseMapping = new VipWarehouseMapping().vipStoreSn(storeMapping.getStoreSn()).warehouseId(warehouseDTO.getId());
                Response<Long> createResp = vipWarehouseMappingWriteService.create(vipWarehouseMapping);
                if (!createResp.isSuccess()) {
                    log.error("fail to create vip warehouse mapping,cause by {}", createResp.getError());
                }
            } else if (!Objects.equals(warehouseMap.get(warehouseDTO.getId()).getVipStoreSn(), storeMapping.getStoreSn())) {
                VipWarehouseMapping update = warehouseMap.get(warehouseDTO.getId());
                update.setVipStoreSn(storeMapping.getStoreSn());
                Response<Boolean> updateResp = vipWarehouseMappingWriteService.update(update);
                if (!updateResp.isSuccess()) {
                    log.error("fail to update vip warehouse mapping ,cause by {}", updateResp.getError());
                }
                needRefresh = Boolean.TRUE;
            }
        }

        //删除vip中已经没有了的仓库信息
        for (Long warehouseId : warehouseMap.keySet()) {
            if (!storeList.contains(warehouseId)) {
                Response<Boolean> delResp = vipWarehouseMappingWriteService.deleteByWarehouseId(warehouseId);
                if (!delResp.isSuccess()) {
                    log.error("fail to delete vip warehouse mapping");
                }
                needRefresh = Boolean.TRUE;
            }
        }
        //是否需要重新推送库存，如果有仓库被移除了默认发货仓规则，则重新推送
        Boolean needPushStock = Boolean.FALSE;
        //检查默认发货仓规则里面是否存在无映射仓库 有的话 删除
        for (Long warehouseId : rWarehouseIds.getResult()) {
            if (!storeList.contains(warehouseId)) {
                warehouseRulesClient.deleteByShopIdAndWarehosueId(shopId, warehouseId);
                needPushStock = Boolean.TRUE;
            }
        }
        if (needPushStock) {
            log.info("start to re push stock to vip");
            shopSkuStockPushHandler.onPushEvent(new PushEvent(shopId, null));
        }
        if (needRefresh) {
            log.info("start to refresh vipMapping");
            vipWarehouseMappingProxy.refreshAll();
        }
    }
}
