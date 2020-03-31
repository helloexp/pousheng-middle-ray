package com.pousheng.middle.web.order.component;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pousheng.middle.open.ReceiverInfoCompleter;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.DispatchOrderEngine;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.OrderNoteProcessingFlag;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.web.order.sync.erp.SyncErpShipmentLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.warehouses.algorithm.WarehouseChooser;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.order.enums.ShipmentOccupyType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zhurg
 * @date 2019/5/27 - 下午4:08
 */
@Slf4j
@Component
public class DispatchOrderHandler {

    @Autowired
    private SkuOrderReadService skuOrderReadService;

    @Autowired
    private ReceiverInfoReadService receiverInfoReadService;

    @Autowired
    private DispatchOrderEngine dispatchOrderEngine;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private ShipmentReadService shipmentReadService;

    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private OrderWriteService orderWriteService;

    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;

    @Autowired
    private ReceiverInfoCompleter receiverInfoCompleter;

    @Autowired
    private WarehouseChooser warehouseChooser;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private MiddleShopCacher middleShopCacher;

    @Autowired
    private SyncErpShipmentLogic syncErpShipmentLogic;

    @Autowired
    private JitOutOfStockSyncYjHandler jitOutOfStockSyncYjHandler;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${dispatch.retry.times:5}")
    private int dispatchRetryTimes;

    /**
     * 拆单派单(拒单后重新派单) for 全渠道
     *
     * @param skuCodeAndQuantities 被拒单的商品
     * @param shopOrder            订单
     */
    private List<SkuCodeAndQuantity> toDispatchOrder(ShopOrder shopOrder, List<SkuCodeAndQuantity> skuCodeAndQuantities) {

        log.info("TO-DISPATCH-ORDER-START for id:{} begin {}", shopOrder.getOrderCode(), DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();
        Response<List<SkuOrder>> skuOrdersRes = skuOrderReadService.findByShopOrderId(shopOrder.getId());
        if (!skuOrdersRes.isSuccess()) {
            throw new JsonResponseException(skuOrdersRes.getError());
        }
        Response<List<ReceiverInfo>> receiveInfosRes = receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
        if (!receiveInfosRes.isSuccess()) {
            throw new JsonResponseException(receiveInfosRes.getError());
        }
        //是否首次派单
        boolean isFirst = CollectionUtils.isEmpty(skuCodeAndQuantities);
        List<SkuOrder> skuOrders = skuOrdersRes.getResult().stream()
                .filter(Objects::nonNull)
                .filter(it -> !Objects.equals(it.getStatus(), MiddleOrderStatus.CANCEL.getValue()))
                .collect(Collectors.toList());
        if (isFirst) {
            //获取skuCode,只取待处理的
            skuCodeAndQuantities = convert2SkuCodeAndQuantity(skuOrders);
        }
        Response<DispatchOrderItemInfo> response = dispatchOrderEngine.toDispatchOrder(shopOrder, receiveInfosRes.getResult().get(0), skuCodeAndQuantities);
        stopwatch.stop();
        log.info("TO-DISPATCH-ORDER-END for id:{} done at {} cost {} ms", shopOrder.getOrderCode(), DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        if (!response.isSuccess()) {
            log.error("dispatch order id:{} fail,error:{}", shopOrder.getOrderCode(), response.getError());
            //记录未处理原因
            shipmentWiteLogic.updateShipmentNote(shopOrder, shipmentWiteLogic.error2Type(response.getError()));
            //如果自动处理处理失败直接设置为DONE
            Map<String, String> shopOrderExtra = shopOrder.getExtra();
            if (StringUtils.isNotEmpty(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG))
                    && Objects.equals(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG), OrderNoteProcessingFlag.WAIT_HANLE.name())) {
                shipmentWiteLogic.updateOrderNoteProcessingFlag(shopOrder, OrderNoteProcessingFlag.DONE.name());
            }
            if (!isFirst) {
                //如果不是第一次派单，将订单状态恢复至待处理(主要针对MPOS拒单的情况)
                shipmentWiteLogic.makeSkuOrderWaitHandle(skuCodeAndQuantities, skuOrders);
            }
            throw new JsonResponseException(response.getError());
        }
        DispatchOrderItemInfo dispatchOrderItemInfo = response.getResult();
        log.info("MPOS DISPATCH ORDER:{} SUCCESS result:{}", shopOrder.getOrderCode(), dispatchOrderItemInfo);
        //寻源就库存不足的
        List<SkuCodeAndQuantity> stockNotEnoughShipments = dispatchOrderItemInfo.getSkuCodeAndQuantities();
        //部分拆单
        boolean partSend = !CollectionUtils.isEmpty(dispatchOrderItemInfo.getSkuCodeAndQuantities());
        int orderNoteCount = 0;
        List<SkuCodeAndQuantity> warehouseFailedShipments = Lists.newArrayList();
        List<SkuCodeAndQuantity> shopFailedShipments = Lists.newArrayList();
        //仓发
        if (!CollectionUtils.isEmpty(dispatchOrderItemInfo.getWarehouseShipments())) {
            for (WarehouseShipment warehouseShipment : dispatchOrderItemInfo.getWarehouseShipments()) {
                try {
                    Long shipmentId = shipmentWiteLogic.createShipment(shopOrder, skuOrders, warehouseShipment);
                    if (shipmentId != null) {
                        orderNoteCount += doAfterCreateShipment(shopOrder, shipmentId, isFirst, 1);
                    }
                } catch (Exception e) {
                    log.error("warehouse dispatch order fail, sku info {}, order id {}", JSON.toJSONString(warehouseShipment.getSkuCodeAndQuantities()), shopOrder.getId(), e);
                    warehouseFailedShipments.addAll(warehouseShipment.getSkuCodeAndQuantities());
                }
            }
        }
        //店发
        if (!CollectionUtils.isEmpty(dispatchOrderItemInfo.getShopShipments())) {
            for (ShopShipment shopShipment : dispatchOrderItemInfo.getShopShipments()) {
                try {
                    Long shipmentId = shipmentWiteLogic.createShopShipment(shopOrder, skuOrders, shopShipment);
                    if (shipmentId != null) {
                        orderNoteCount += doAfterCreateShipment(shopOrder, shipmentId, isFirst, 2);
                    }
                } catch (Exception e) {
                    log.error("shop dispatch order fail, sku info {}, order id {}", JSON.toJSONString(shopShipment.getSkuCodeAndQuantities()), shopOrder.getId(), e);
                    shopFailedShipments.addAll(shopShipment.getSkuCodeAndQuantities());
                }
            }
        }

        if (!CollectionUtils.isEmpty(warehouseFailedShipments) || !CollectionUtils.isEmpty(stockNotEnoughShipments)) {
            partSend = true;
            //TODO 不太确定为啥之前的版本对与店发失败的不处理
            List<SkuCodeAndQuantity> faileds = Lists.newArrayList();
            if (!CollectionUtils.isEmpty(warehouseFailedShipments)) {
                faileds.addAll(warehouseFailedShipments);
            }
            if (!CollectionUtils.isEmpty(stockNotEnoughShipments)) {
                faileds.addAll(stockNotEnoughShipments);
            }
            //如果是恒康pos订单或者全渠道订单，暂不处理
            if (shopOrder.getExtra().containsKey(TradeConstants.IS_HK_POS_ORDER) || orderReadLogic.isAllChannelOpenShop(shopOrder.getShopId())) {
                log.info("hk pos or all channel order(id:{}) can not be dispatched", shopOrder.getId());
                if (!isFirst) {
                    //如果不是第一次派单，将订单状态恢复至待处理，主要针对MPOS拒单的情况
                    shipmentWiteLogic.makeSkuOrderWaitHandle(faileds, skuOrders);
                }
            } else {
                log.info("mpos order(id:{}) can not be dispatched", shopOrder.getId());
                //取消子单
                for (SkuCodeAndQuantity skuCodeAndQuantity : faileds) {
                    SkuOrder skuOrder = shipmentWiteLogic.getSkuOrder(skuOrders, skuCodeAndQuantity.getSkuOrderId(), skuCodeAndQuantity.getSkuCode());
                    orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue());
                    //添加取消原因
                    Map<String, String> skuOrderExtra = skuOrder.getExtra();
                    skuOrderExtra.put(TradeConstants.SKU_ORDER_CANCEL_REASON, TradeConstants.SKU_CANNOT_BE_DISPATCHED);
                    orderWriteService.updateOrderExtra(skuOrder.getId(), OrderLevel.SKU, skuOrderExtra);
                }
                syncMposOrderLogic.syncNotDispatcherSkuToMpos(shopOrder, faileds);
            }
        }

        List<SkuCodeAndQuantity> failedShipments = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(warehouseFailedShipments)) {
            failedShipments.addAll(warehouseFailedShipments);
        }
        if (!CollectionUtils.isEmpty(shopFailedShipments)) {
            failedShipments.addAll(shopFailedShipments);
        }
        if (isFirst) {
            updateStatus(shopOrder, orderNoteCount, partSend, failedShipments);
        }

        //释放mpos拒单的库存
        if (!CollectionUtils.isEmpty(dispatchOrderItemInfo.getWarehouseShipments()) || !CollectionUtils.isEmpty(dispatchOrderItemInfo.getShopShipments())) {
            orderWriteLogic.releaseRejectShipmentOccupyStock(shopOrder.getId());
        }

        //如果没有派出去的单子则提示库存不足
        if (CollectionUtils.isEmpty(dispatchOrderItemInfo.getWarehouseShipments()) && CollectionUtils.isEmpty(dispatchOrderItemInfo.getShopShipments())) {
            Map<String, String> shopOrderExtra = shopOrder.getExtra();
            if (StringUtils.isNotEmpty(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG))
                    && Objects.equals(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG), OrderNoteProcessingFlag.WAIT_HANLE.name())) {
                shipmentWiteLogic.updateOrderNoteProcessingFlag(shopOrder, OrderNoteProcessingFlag.DONE.name());
                //库存不足，添加备注
                shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.NOTE_ORDER_NO_SOTCK.value());
            } else {
                //库存不足，添加备注
                shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.STOCK_NOT_ENOUGH.value());
            }
        }

        return failedShipments;
    }

    private List<SkuCodeAndQuantity> autoCreateAllChannelShipmentNewLogic(ShopOrder shopOrder, List<SkuOrder> skuOrders) {
        if (log.isDebugEnabled()) {
            log.debug("AllChannelShipmentNewLogic autoCreateAllChannelShipmentNewLogic,shopOrder id:{}", shopOrder.getOrderCode());
        }
        //获取skuCode,数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantities = convert2SkuCodeAndQuantityForNewDispatchLogic(skuOrders);

        //获取addressId
        Response<List<ReceiverInfo>> response = receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find ReceiverInfo failed,shopOrderId is(:{})", shopOrder.getId());
            throw new DispatchException("find.receiverInfo.failed");
        }

        ReceiverInfo receiverInfo = response.getResult().get(0);
        if (Arguments.isNull(receiverInfo.getCity())) {
            log.error("receive info:{} city id is null,so skip auto create shipment", receiverInfo);
            throw new DispatchException("receiveInfo.cityId.isNull");
        }

        if (Arguments.isNull(receiverInfo.getCityId())) {
            receiverInfoCompleter.complete(receiverInfo);
        }

        //选择发货仓库
        List<WarehouseShipment> warehouseShipments = null;
        try {
            warehouseShipments = warehouseChooser.chooseByRegion(receiverInfo, shopOrder, Long.valueOf(receiverInfo.getCityId()), skuCodeAndQuantities);
        } catch (Exception e) {
            throw new DispatchException(e.getMessage());
        }

        //校验寻源结果，是否能够派单
        if (!checkWarehouseStock(warehouseShipments, shopOrder)) {
            throw new DispatchException("stock.not.enough");
        }

        //遍历不同的发货仓生成相应的发货单
        int orderNoteCount = 0;
        //部分派单
        boolean partSend = false;
        //创建失败的发货单
        List<SkuCodeAndQuantity> failedShipments = Lists.newArrayList();
        //判断是否存在部分派单的情况
        Set<Long> allShipmentSkuOrders = Sets.newHashSet();
        warehouseShipments.forEach(warehouseShipment ->
                allShipmentSkuOrders.addAll(warehouseShipment.getSkuCodeAndQuantities().stream().map(SkuCodeAndQuantity::getSkuOrderId).collect(Collectors.toList()))
        );
        if (allShipmentSkuOrders.size() != skuOrders.size()) {
            partSend = true;
        }
        for (WarehouseShipment warehouseShipment : warehouseShipments) {
            Long shipmentId = null;
            try {
                WarehouseDTO warehouse = warehouseCacher.findById(warehouseShipment.getWarehouseId());
                if (Objects.equals(warehouse.getWarehouseSubType(), WarehouseType.SHOP_WAREHOUSE.value())) {
                    ShopShipment shopShipment = new ShopShipment();
                    Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(), Long.parseLong(warehouse.getCompanyId()));
                    shopShipment.setShopId(shop.getId());
                    shopShipment.setShopName(shop.getName());
                    shopShipment.setSkuCodeAndQuantities(warehouseShipment.getSkuCodeAndQuantities());
                    shipmentId = shipmentWiteLogic.createShopShipment(shopOrder, skuOrders, shopShipment);
                } else {
                    shipmentId = shipmentWiteLogic.createShipment(shopOrder, skuOrders, warehouseShipment);
                }

            } catch (Exception e) {
                log.error("shopOrder [{}] failed to gen shipment order error {} ", shopOrder.getId(), Throwables.getStackTraceAsString(e));
                failedShipments.addAll(warehouseShipment.getSkuCodeAndQuantities());
            }
            if (null == shipmentId) {
                continue;
            }

            //修改子单和总单的状态,待处理数量,并同步恒康
            Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
            if (!shipmentRes.isSuccess()) {
                log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
                continue;
            }

            Shipment shipment = shipmentRes.getResult();
            doAfterCreateShipment(shopOrder, shipment, orderNoteCount);

            if (Objects.equals(shipment.getShipWay(), 1)) {
                shipmentWiteLogic.handleSyncShipment(shipment, 2, shopOrder);
            } else {
                shipmentWiteLogic.handleSyncShipment(shipment, 1, shopOrder);
            }
        }

        //更新状态
        updateStatus(shopOrder, orderNoteCount, partSend, failedShipments);
        return failedShipments;
    }


    public List<SkuCodeAndQuantity> autoCreateShipmentLogic(ShopOrder shopOrder, List<SkuOrder> skuOrders) {
        if (log.isDebugEnabled()) {
            log.debug("ShipmentWiteLogic autoCreateShipmentLogic,shopOrder id:{}", shopOrder.getOrderCode());
        }

        //获取skuCode,数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantities = convert2SkuCodeAndQuantityForNewDispatchLogic(skuOrders);

        //获取addressId
        Response<List<ReceiverInfo>> response = receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find ReceiverInfo failed,shopOrderId is(:{})", shopOrder.getId());
            throw new DispatchException("find.receiverInfo.failed");
        }
        ReceiverInfo receiverInfo = response.getResult().get(0);
        if (Arguments.isNull(receiverInfo.getCity())) {
            log.error("receive info:{} city id is null,so skip auto create shipment", receiverInfo);
            throw new DispatchException("receiveInfo.cityId.isNull");
        }
        if (Arguments.isNull(receiverInfo.getCityId())) {
            receiverInfoCompleter.complete(receiverInfo);
        }
        //选择发货仓库
        List<WarehouseShipment> warehouseShipments = null;
        try {
            warehouseShipments = warehouseChooser.choose(shopOrder, Long.valueOf(receiverInfo.getCityId()), skuCodeAndQuantities);
        } catch (Exception e) {
            throw new DispatchException(e.getMessage());
        }

        //校验寻源结果，是否能够派单
        if (!checkWarehouseStock(warehouseShipments, shopOrder)) {
            throw new DispatchException("stock.not.enough");
        }

        //遍历不同的发货仓生成相应的发货单
        int orderNoteCount = 0;
        //部分发货
        boolean partSend = false;
        List<SkuCodeAndQuantity> failedShipents = Lists.newArrayList();
        //判断所有的发货仓库能不能把所有的子单发出去
        Set<Long> allShipmentSkuOrders = Sets.newHashSet();
        warehouseShipments.forEach(warehouseShipment ->
                allShipmentSkuOrders.addAll(warehouseShipment.getSkuCodeAndQuantities().stream().map(SkuCodeAndQuantity::getSkuOrderId).collect(Collectors.toList()))
        );
        //存在缺货发布出去的
        if (allShipmentSkuOrders.size() != skuOrders.size()) {
            partSend = true;
        }
        for (WarehouseShipment warehouseShipment : warehouseShipments) {
            Long shipmentId = null;
            try {
                shipmentId = shipmentWiteLogic.createShipment(shopOrder, skuOrders, warehouseShipment);
            } catch (Exception e) {
                log.error("shopOrder [{}] failed to gen shipment order error {} ", shopOrder.getId(), Throwables.getStackTraceAsString(e));
                failedShipents.addAll(warehouseShipment.getSkuCodeAndQuantities());
            }
            if (null == shipmentId) {
                continue;
            }
            //修改子单和总单的状态,待处理数量,并同步恒康
            Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
            if (!shipmentRes.isSuccess()) {
                log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
                continue;
            }

            Shipment shipment = shipmentRes.getResult();
            doAfterCreateShipment(shopOrder, shipment, orderNoteCount);

            Response<Boolean> syncRes = syncErpShipmentLogic.syncShipment(shipmentRes.getResult());
            if (!syncRes.isSuccess()) {
                log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
            }
        }

        updateStatus(shopOrder, orderNoteCount, partSend, failedShipents);
        return failedShipents;
    }

    public void toDispatchOrderNew(ShopOrder shopOrder, List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        int retryTimes = 0;
        List<SkuCodeAndQuantity> failedSkuCodeAndQuantityList = null;
        while (retryTimes <= dispatchRetryTimes) {
            try {
                failedSkuCodeAndQuantityList = toDispatchOrder(shopOrder, skuCodeAndQuantities);
                if (CollectionUtils.isEmpty(failedSkuCodeAndQuantityList)) {
                    log.info("order {} dispatch finished", shopOrder.getOrderCode());
                    break;
                }
                //MPOS拒收重新派单不重试
                if (!CollectionUtils.isEmpty(skuCodeAndQuantities)) {
                    log.info("mpos reject order {} dispatch finished", shopOrder.getOrderCode());
                    break;
                }
                //云聚jit的单子不重新寻源
                if (MiddleChannel.YUNJUJIT.getValue().equals(shopOrder.getOutFrom())) {
                    //jit整单缺货通知云聚
                    jitOutOfStockSyncYjHandler.syncOutOfStockOrder2Yj(JitOutOfStockSyncYjHandler.JitOutIfStockEvent.builder()
                            .shopOrder(shopOrder)
                            .build());
                    break;
                }
                //skuCodeAndQuantities = failedSkuCodeAndQuantityList;
                retryTimes++;
                log.info("order {} to dispatch skuOrder {}, retry {} times", shopOrder.getOrderCode(), JSON.toJSONString(failedSkuCodeAndQuantityList), retryTimes - 1);
            } catch (Throwable e) {
                if (e instanceof JsonResponseException) {
                    throw e;
                }
                retryTimes++;
                log.error("order {} to dispatch skuOrder {}, retry {} times failed,cause: ", shopOrder.getOrderCode(), JSON.toJSONString(failedSkuCodeAndQuantityList), retryTimes - 1, e);
            }
        }
    }

    public boolean autoCreateAllChannelShipment(ShopOrder shopOrder, List<SkuOrder> skuOrders, boolean newLogic) {
        int retryTimes = 0;
        List<SkuCodeAndQuantity> failedSkuCodeAndQuantityList = null;
        while (retryTimes <= dispatchRetryTimes) {
            try {
                if (newLogic) {
                    failedSkuCodeAndQuantityList = autoCreateAllChannelShipmentNewLogic(shopOrder, skuOrders);
                } else {
                    failedSkuCodeAndQuantityList = autoCreateShipmentLogic(shopOrder, skuOrders);
                }
                if (CollectionUtils.isEmpty(failedSkuCodeAndQuantityList)) {
                    log.info("order {} dispatch finished", shopOrder.getOrderCode());
                    return true;
                }
                //云聚jit的单子不重新寻源
                if (MiddleChannel.YUNJUJIT.getValue().equals(shopOrder.getOutFrom())) {
                    //jit整单缺货通知云聚
                    jitOutOfStockSyncYjHandler.syncOutOfStockOrder2Yj(JitOutOfStockSyncYjHandler.JitOutIfStockEvent.builder()
                            .shopOrder(shopOrder)
                            .build());
                    break;
                }
                retryTimes++;
                log.info("order {} to dispatch skuOrder {}, retry {} times", shopOrder.getOrderCode(), JSON.toJSONString(failedSkuCodeAndQuantityList), retryTimes - 1);
                //重新查询待处理的子单，因为可能已经存在发货的了
                skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(),
                        MiddleOrderStatus.WAIT_HANDLE.getValue());
            } catch (Throwable e) {
                if (e instanceof DispatchException) {
                    log.error("order {} to dispatch skuOrder {} failed,cause: ", shopOrder.getOrderCode(), JSON.toJSON(skuOrders), e);
                    throw new JsonResponseException(e.getMessage());
                }
                retryTimes++;
                log.error("order {} to dispatch skuOrder {}, retry {} times failed,cause: ", shopOrder.getOrderCode(), JSON.toJSONString(skuOrders), retryTimes - 1, e);
            }
        }
        return false;
    }

    /**
     * 生成发货单后处理
     *
     * @param shopOrder
     * @param shipmentId
     * @param isFirst
     * @param type
     * @return
     */
    private int doAfterCreateShipment(ShopOrder shopOrder, Long shipmentId, boolean isFirst, int type) {
        Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
        if (!shipmentRes.isSuccess()) {
            log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
            return 0;
        }
        Shipment shipment = shipmentRes.getResult();
        if (isFirst) {
            if (Objects.equals(shopOrder.getOutFrom(), "yunjujit")) {
                //jit大单 整单发货，所以将所有子单合并处理即可
                middleOrderWriteService.updateOrderStatusForJit(shopOrder, MiddleOrderEvent.HANDLE_DONE.toOrderOperation());
            } else {
                orderWriteLogic.updateSkuHandleNumber(shipment.getSkuInfos());
            }
        }
        //如果存在预售类型的订单，且预售类型的订单没有支付尾款，此时不能同步恒康
        Map<String, String> extraMap = shopOrder.getExtra();
        String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
        String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
        if (!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
            if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                return 0;
            }
            if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                return 0;
            }
        }

        //占库发货单不同步第三方渠道履约
        if (!Objects.equals(shipment.getIsOccupyShipment(), ShipmentOccupyType.SALE_Y.name())) {
            shipmentWiteLogic.handleSyncShipment(shipment, type, shopOrder);
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * 生成发货单后处理
     *
     * @param shopOrder
     * @param shipment
     * @param orderNoteCount
     */
    private void doAfterCreateShipment(ShopOrder shopOrder, Shipment shipment, int orderNoteCount) {
        try {
            if (Objects.equals(shopOrder.getOutFrom(), "yunjujit")) {
                //jit大单 整单发货，所以将所有子单合并处理即可
                middleOrderWriteService.updateOrderStatusForJit(shopOrder, MiddleOrderEvent.HANDLE_DONE.toOrderOperation());
            } else {
                orderWriteLogic.updateSkuHandleNumber(shipment.getSkuInfos());
            }
        } catch (ServiceException e) {
            log.error("shipment id is {} update sku handle number failed.caused by {}", shipment.getId(), Throwables.getStackTraceAsString(e));
        }

        //占库发货单不同步订单派发中心或者mpos
        if (Objects.equals(shipment.getIsOccupyShipment(), ShipmentOccupyType.SALE_Y.name())) {
            orderNoteCount++;
            return;
        }
        //如果存在预售类型的订单，且预售类型的订单没有支付尾款，此时不能同步恒康
        Map<String, String> extraMap = shopOrder.getExtra();
        String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
        String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
        if (!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
            if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                return;
            }
            if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                return;
            }
        }
    }

    /**
     * 将子单行转化成SkuCodeAndQuantity
     *
     * @param skuOrders
     * @return
     */
    private List<SkuCodeAndQuantity> convert2SkuCodeAndQuantity(List<SkuOrder> skuOrders) {
        return skuOrders.stream()
                .filter(skuOrder -> Objects.equals(MiddleOrderStatus.WAIT_HANDLE.getValue(), skuOrder.getStatus()))
                .map(skuOrder -> {
                    SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                    skuCodeAndQuantity.setSkuOrderId(skuOrder.getId());
                    skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
                    skuCodeAndQuantity.setQuantity(skuOrder.getWithHold());
                    return skuCodeAndQuantity;
                }).collect(Collectors.toList());
    }

    private List<SkuCodeAndQuantity> convert2SkuCodeAndQuantityForNewDispatchLogic(List<SkuOrder> skuOrders) {
        return skuOrders.stream()
                .filter(skuOrder -> Objects.equals(MiddleOrderStatus.WAIT_HANDLE.getValue(), skuOrder.getStatus()))
                .map(skuOrder -> {
                    SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                    skuCodeAndQuantity.setSkuOrderId(skuOrder.getId());
                    skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
                    skuCodeAndQuantity.setQuantity(Integer.valueOf(orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.WAIT_HANDLE_NUMBER, skuOrder)));
                    return skuCodeAndQuantity;
                }).collect(Collectors.toList());
    }

    /**
     * 校验寻源仓库，是否库存充足
     *
     * @param warehouseShipments
     * @return
     */
    private boolean checkWarehouseStock(List<WarehouseShipment> warehouseShipments, ShopOrder shopOrder) {
        if (CollectionUtils.isEmpty(warehouseShipments)) {
            Map<String, String> shopOrderExtra = shopOrder.getExtra();
            if (StringUtils.isNotEmpty(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG))
                    && Objects.equals(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG), OrderNoteProcessingFlag.WAIT_HANLE.name())) {
                shipmentWiteLogic.updateOrderNoteProcessingFlag(shopOrder, OrderNoteProcessingFlag.DONE.name());
                //库存不足，添加备注
                shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.NOTE_ORDER_NO_SOTCK.value());
            } else {
                //库存不足，添加备注
                shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.STOCK_NOT_ENOUGH.value());
            }
            return false;
        }
        return true;
    }

    /**
     * @param shopOrder
     * @param orderNoteCount
     * @param partSend
     * @param failedShipments
     */
    private void updateStatus(ShopOrder shopOrder, int orderNoteCount, boolean partSend, List<SkuCodeAndQuantity> failedShipments) {
        //更新备注订单处理状态
        if (orderNoteCount > 0) {
            //handleStatus状态变为：备注订单已经占库
            shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.NOTE_ORDER_OCCUPY_SHIPMENT_CREATED.value());
            Map<String, String> shopOrderExtra = shopOrder.getExtra();
            if (StringUtils.isNotEmpty(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG))
                    && Objects.equals(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG), OrderNoteProcessingFlag.WAIT_HANLE.name())) {
                shipmentWiteLogic.updateOrderNoteProcessingFlag(shopOrder, OrderNoteProcessingFlag.DONE.name());
            }
        } else {
            //如果是云聚jit的渠道且无发货单 则直接返回（在创建发货单的时候已经标记了无库存。故此处不处理直接返回）
            if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.YUNJUJIT.getValue())) {
                if (!CollectionUtils.isEmpty(failedShipments)) {
                    shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.STOCK_LOCK_FAIL.value());
                } else if (partSend) {
                    shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.STOCK_NOT_ENOUGH.value());
                } else {
                    shipmentWiteLogic.updateShipmentNote(shopOrder, OrderWaitHandleType.HANDLE_DONE.value());
                }
            }
        }
    }

    public static class DispatchException extends RuntimeException {

        private static final long serialVersionUID = -1231324445049942207L;

        public DispatchException(String error) {
            super(error);
        }
    }
}