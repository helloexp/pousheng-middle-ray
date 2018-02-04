package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.DispatchOrderEngine;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import com.pousheng.middle.web.warehouses.algorithm.WarehouseChooser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.service.MsgService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发货单写服务
 * Created by songrenfei on 2017/7/2
 */
@Component
@Slf4j
public class ShipmentWiteLogic {
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private WarehouseChooser warehouseChooser;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SkuOrderReadService skuOrderReadService;
    @Autowired
    private DispatchOrderEngine dispatchOrderEngine;
    @RpcConsumer
    private MiddleOrderWriteService middleOrderWriteService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;
    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private MsgService msgService;
    @Autowired
    private ShopReadService shopReadService;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    public Response<Boolean> updateStatus(Shipment shipment, OrderOperation orderOperation) {

        Flow flow = flowPicker.pickShipments();
        if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
            log.error("shipment(id:{}) current status:{} not allow operation:{}", shipment.getId(), shipment.getStatus(), orderOperation.getText());
            return Response.fail("shipment.status.not.allow.current.operation");
        }

        Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
        Response<Boolean> updateRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), targetStatus);
        if (!updateRes.isSuccess()) {
            log.error("update shipment(id:{}) status to:{} fail,error:{}", shipment.getId(), updateRes.getError());
            return Response.fail(updateRes.getError());
        }
        return Response.ok();

    }


    //更新发货单
    public void update(Shipment shipment) {
        Response<Boolean> updateRes = shipmentWriteService.update(shipment);
        if (!updateRes.isSuccess()) {
            log.error("update shipment:{} fail,error:{}", shipment, updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    //更新发货单Extra
    public void updateExtra(Long shipmentId, Map<String, String> extraMap) {

        Shipment updateShipment = new Shipment();
        updateShipment.setId(shipmentId);
        updateShipment.setExtra(extraMap);

        this.update(updateShipment);
    }

    /**
     * 取消/删除发货单逻辑(撤销订单的时候通知恒康删除发货单,电商取消订单的时候取消发货单)
     *
     * @param shipment 发货单
     * @param type     0 取消 1 删除
     * @return 取消成功 返回返回true,取消失败返回false
     */
    public boolean cancelShipment(Shipment shipment, Integer type) {
        try {
            log.info("try to auto cancel shipment,shipment id is {},operationType is {}", shipment.getId(), type);
            Flow flow = flowPicker.pickShipments();
            //未同步恒康,现在只需要将发货单状态置为已取消即可
            if (flow.operationAllowed(shipment.getStatus(), MiddleOrderEvent.CANCEL_SHIP.toOrderOperation())) {
                Response<Boolean> cancelRes = this.updateStatus(shipment, MiddleOrderEvent.CANCEL_SHIP.toOrderOperation());
                if (!cancelRes.isSuccess()) {
                    log.error("cancel shipment(id:{}) fail,error:{}", shipment.getId(), cancelRes.getError());
                    throw new JsonResponseException(cancelRes.getError());
                }
            }
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            //已经同步过恒康,现在需要取消同步恒康,根据恒康返回的结果判断是否取消成功(如果是mpos订单发货，则不用同步恒康)
            if (!Objects.equals(shipmentExtra.getShipmentWay(),TradeConstants.MPOS_SHOP_DELIVER) && flow.operationAllowed(shipment.getStatus(), MiddleOrderEvent.CANCEL_HK.toOrderOperation())) {
                Response<Boolean> syncRes = syncShipmentLogic.syncShipmentCancelToHk(shipment, type);
                if (!syncRes.isSuccess()) {
                    log.error("sync cancel shipment(id:{}) to hk fail,error:{}", shipment.getId(), syncRes.getError());
                    throw new JsonResponseException(syncRes.getError());
                }
            }
            //解锁库存
            DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
            mposSkuStockLogic.unLockStock(dispatchOrderItemInfo);
            return true;
        } catch (Exception e) {
            log.error("cancel shipment failed,shipment id is :{},error{}", shipment.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 自动创建发货单
     * @param shopOrder 店铺订单
     */
    public void doAutoCreateShipment(ShopOrder shopOrder) {
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(),
                MiddleOrderStatus.WAIT_HANDLE.getValue());
        if (skuOrders.size() == 0) {
            return;
        }
        //判断是否满足自动生成发货单
        if (!commValidateOfOrder(shopOrder, skuOrders)) {
            return;
        }
        this.autoCreateShipmentLogic(shopOrder, skuOrders);
    }


    /**
     * 订单自动处理逻辑
     * @param shopOrder 店铺订单
     */
    public Response<String> autoHandleOrder(ShopOrder shopOrder) {
        //没有经过自动生成发货单逻辑的订单时不能自动处理的，可能存在冲突
        Map<String, String> shopOrderExtra = shopOrder.getExtra();
        String orderWaitHandleType = shopOrderExtra.get(TradeConstants.NOT_AUTO_CREATE_SHIPMENT_NOTE);
        if (Objects.equals(orderWaitHandleType, String.valueOf(OrderWaitHandleType.WAIT_HANDLE.value()))) {
            return Response.fail(OrderWaitHandleType.WAIT_HANDLE.getDesc());
        }
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(),
                MiddleOrderStatus.WAIT_HANDLE.getValue());
        if (skuOrders.size() == 0) {
            return Response.fail("sku.order.not.wait.handle.status");
        }
        //判断是否满足自动生成发货单
        if (!autoHandleOrderParam(shopOrder, skuOrders)) {
            return Response.fail(OrderWaitHandleType.SKU_NOT_MATCH.getDesc());
        }
        if (!this.autoCreateShipmentLogic(shopOrder, skuOrders)) {
            return Response.fail(OrderWaitHandleType.STOCK_NOT_ENOUGH.getDesc());
        }
        return Response.ok("");
    }

    /**
     * 自动生成发货单逻辑
     * @param shopOrder
     * @param skuOrders
     */
    private boolean autoCreateShipmentLogic(ShopOrder shopOrder, List<SkuOrder> skuOrders) {
        //获取skuCode,数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayListWithCapacity(skuOrders.size());
        skuOrders.forEach(skuOrder -> {
            SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
            skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
            skuCodeAndQuantity.setQuantity(Integer.valueOf(orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.WAIT_HANDLE_NUMBER, skuOrder)));
            skuCodeAndQuantities.add(skuCodeAndQuantity);
        });
        //获取addressId
        Response<List<ReceiverInfo>> response = receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find ReceiverInfo failed,shopOrderId is(:{})", shopOrder.getId());
            return false;
        }
        ReceiverInfo receiverInfo = response.getResult().get(0);
        if (Arguments.isNull(receiverInfo.getCityId())) {
            log.error("receive info:{} city id is null,so skip auto create shipment", receiverInfo);
            return false;
        }
        //选择发货仓库
        List<WarehouseShipment> warehouseShipments = warehouseChooser.choose(shopOrder.getShopId(), Long.valueOf(receiverInfo.getCityId()), skuCodeAndQuantities);
        if (Objects.isNull(warehouseShipments) || warehouseShipments.isEmpty()) {
            //库存不足，添加备注
            this.updateShipmentNote(shopOrder, OrderWaitHandleType.STOCK_NOT_ENOUGH.value());
            return false;
        }
        //遍历不同的发货仓生成相应的发货单
        for (WarehouseShipment warehouseShipment : warehouseShipments) {

            Long shipmentId = this.createShipment(shopOrder, skuOrders, warehouseShipment);
            //修改子单和总单的状态,待处理数量,并同步恒康
            if (shipmentId != null) {
                Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
                if (!shipmentRes.isSuccess()) {
                    log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
                    return false;
                }
                try {
                    orderWriteLogic.updateSkuHandleNumber(shipmentRes.getResult().getSkuInfos());
                } catch (ServiceException e) {
                    log.error("shipment id is {} update sku handle number failed.caused by {}", shipmentId, e.getMessage());
                }
                //如果存在预售类型的订单，且预售类型的订单没有支付尾款，此时不能同步恒康
                Map<String, String> extraMap = shopOrder.getExtra();
                String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
                String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
                if (!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
                    if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                        continue;
                    }
                }
                Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipmentRes.getResult());
                if (!syncRes.isSuccess()) {
                    log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
                }
            }
        }
        this.updateShipmentNote(shopOrder, OrderWaitHandleType.HANDLE_DONE.value());
        return true;
    }

    /**
     * 添加不能自动生成发货单的原因
     * @param shopOrder 店铺订单
     * @param type      不能自动生成发货单的类型
     */
    public void updateShipmentNote(ShopOrder shopOrder, int type) {
        //添加备注
        if (type > 0) {
            //shopOrderextra中添加字段
            ShopOrder order = orderReadLogic.findShopOrderById(shopOrder.getId());
            Map<String, String> extraMap = order.getExtra();
            extraMap.put(TradeConstants.NOT_AUTO_CREATE_SHIPMENT_NOTE, type == 6 ? "" : String.valueOf(type));
            Response<Boolean> rltRes = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, extraMap);
            if (!rltRes.isSuccess()) {
                log.error("update shopOrder：{} extra map to:{} fail,error:{}", shopOrder.getId(), extraMap, rltRes.getError());

            }
        }
    }

    /**
     * 创建发货单
     * @param shopOrder         店铺订单
     * @param skuOrders         子单
     * @param warehouseShipment 发货仓库信息
     */
    private Long createShipment(ShopOrder shopOrder, List<SkuOrder> skuOrders, WarehouseShipment warehouseShipment) {
        //获取该仓库中可发货的skuCode和数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantitiesChooser = warehouseShipment.getSkuCodeAndQuantities();
        //获取仓库的id
        long warehouseId = warehouseShipment.getWarehouseId();
        //获取skuOid,quantity的集合
        Map<Long, Integer> skuOrderIdAndQuantity = Maps.newHashMap();

        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantitiesChooser) {
            if (skuCodeAndQuantity.getQuantity() == 0) {
                continue;
            }
            skuOrderIdAndQuantity.put(this.getSkuOrder(skuOrders, skuCodeAndQuantity.getSkuCode()).getId(), skuCodeAndQuantity.getQuantity());
        }
        if (skuOrderIdAndQuantity.size() == 0) {
            return null;
        }
        //获取该发货单中涉及到的sku订单
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrdersShipment = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        log.info("auto create shipment,step four");
        //封装发货信息
        List<ShipmentItem> shipmentItems = makeShipmentItems(skuOrdersShipment, skuOrderIdAndQuantity);
        //发货单商品金额
        Long shipmentItemFee = 0L;
        //发货单总的优惠
        Long shipmentDiscountFee = 0L;
        //发货单总的净价
        Long shipmentTotalFee = 0L;
        //运费
        Long shipmentShipFee = 0L;
        //运费优惠
        Long shipmentShipDiscountFee = 0L;
        //判断运费是否已经加过
        if (!isShipmentFeeCalculated(shopOrder.getId())) {
            shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee() == null ? 0 : shopOrder.getOriginShipFee());
            shipmentShipDiscountFee = shipmentShipFee - Long.valueOf(shopOrder.getShipFee() == null ? 0 : shopOrder.getShipFee());
        }
        for (ShipmentItem shipmentItem : shipmentItems) {
            shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
            shipmentDiscountFee = shipmentItem.getSkuDiscount() + shipmentDiscountFee;
            shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
        }
        //订单总金额(运费优惠已经包含在子单折扣中)=商品总净价+运费
        Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;
        Shipment shipment = this.makeShipment(shopOrder, warehouseId, shipmentItemFee
                , shipmentDiscountFee, shipmentTotalFee, shipmentShipFee, shipmentShipDiscountFee, shipmentTotalPrice, shopOrder.getShopId());
        shipment.setSkuInfos(skuOrderIdAndQuantity);
        shipment.setType(ShipmentType.SALES_SHIP.value());
        shipment.setShopId(shopOrder.getShopId());
        shipment.setShopName(shopOrder.getShopName());
        Map<String, String> extraMap = shipment.getExtra();
        extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(shipmentItems));
        shipment.setExtra(extraMap);
        //创建发货单
        Response<Long> createResp = shipmentWriteService.create(shipment, Arrays.asList(shopOrder.getId()), OrderLevel.SHOP);
        log.info("auto create shipment,step eight");
        if (!createResp.isSuccess()) {
            log.error("fail to create shipment:{} for order(id={}),and level={},cause:{}",
                    shipment, shopOrder.getId(), OrderLevel.SHOP.getValue(), createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }

        //生成发货单之后需要将发货单id添加到子单中
        for (SkuOrder skuOrder : skuOrdersShipment) {
            try {
                Map<String, String> skuOrderExtra = skuOrder.getExtra();
                skuOrderExtra.put(TradeConstants.SKU_ORDER_SHIPMENT_ID, String.valueOf(createResp.getResult()));
                Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(), OrderLevel.SKU, skuOrderExtra);
                if (!response.isSuccess()) {
                    log.error("update sku order：{} extra map to:{} fail,error:{}", skuOrder.getId(), skuOrderExtra, response.getError());
                }
            } catch (Exception e) {
                log.error("update sku shipment id failed,skuOrder id is {},shipmentId is {},caused by {}", skuOrder.getId(), createResp.getResult(), e.getMessage());
            }
        }
        return createResp.getResult();
    }

    /**
     * 创建发货单
     *
     * @param shopOrder    店铺订单
     * @param skuOrders    子单
     * @param shopShipment 发货店铺信息
     */
    private Long createShopShipment(ShopOrder shopOrder, List<SkuOrder> skuOrders, ShopShipment shopShipment) {
        //获取该仓库中可发货的skuCode和数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantitiesChooser = shopShipment.getSkuCodeAndQuantities();
        //获取发货店铺的id
        long deliveyShopId = shopShipment.getShopId();
        //获取skuOid,quantity的集合
        Map<Long, Integer> skuOrderIdAndQuantity = Maps.newHashMap();

        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantitiesChooser) {
            if (skuCodeAndQuantity.getQuantity() == 0) {
                continue;
            }
            skuOrderIdAndQuantity.put(this.getSkuOrder(skuOrders, skuCodeAndQuantity.getSkuCode()).getId(), skuCodeAndQuantity.getQuantity());
        }
        if (skuOrderIdAndQuantity.size() == 0) {
            return null;
        }
        //获取该发货单中涉及到的sku订单
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrdersShipment = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        log.info("auto create shipment,step four");
        //封装发货信息
        List<ShipmentItem> shipmentItems = makeShipmentItems(skuOrdersShipment, skuOrderIdAndQuantity);
        //发货单商品金额
        Long shipmentItemFee = 0L;
        //发货单总的优惠
        Long shipmentDiscountFee = 0L;
        //发货单总的净价
        Long shipmentTotalFee = 0L;
        //运费
        Long shipmentShipFee = 0L;
        //运费优惠
        Long shipmentShipDiscountFee = 0L;
        //判断运费是否已经加过
        if (!isShipmentFeeCalculated(shopOrder.getId())) {
            shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee() == null ? 0 : shopOrder.getOriginShipFee());
            shipmentShipDiscountFee = shipmentShipFee - Long.valueOf(shopOrder.getShipFee() == null ? 0 : shopOrder.getShipFee());
        }
        for (ShipmentItem shipmentItem : shipmentItems) {
            shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
            shipmentDiscountFee = shipmentItem.getSkuDiscount() + shipmentDiscountFee;
            shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
        }
        //订单总金额(运费优惠已经包含在子单折扣中)=商品总净价+运费
        Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;
        ;

        Shipment shipment = this.makeShopShipment(shopOrder, deliveyShopId,shopShipment.getShopName(), shipmentItemFee
                , shipmentDiscountFee, shipmentTotalFee, shipmentShipFee, shipmentShipDiscountFee, shipmentTotalPrice, shopOrder.getShopId());
        shipment.setSkuInfos(skuOrderIdAndQuantity);
        shipment.setType(ShipmentType.SALES_SHIP.value());
        shipment.setShopId(shopOrder.getShopId());
        shipment.setShopName(shopOrder.getShopName());
        Map<String, String> extraMap = shipment.getExtra();
        extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(shipmentItems));
        shipment.setExtra(extraMap);
        //创建发货单
        Response<Long> createResp = shipmentWriteService.create(shipment, Arrays.asList(shopOrder.getId()), OrderLevel.SHOP);
        log.info("auto create shipment,step eight");
        if (!createResp.isSuccess()) {
            log.error("fail to create shipment:{} for order(id={}),and level={},cause:{}",
                    shipment, shopOrder.getId(), OrderLevel.SHOP.getValue(), createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
        //生成发货单之后需要将发货单id添加到子单中
        for (SkuOrder skuOrder : skuOrdersShipment) {
            try {
                Map<String, String> skuOrderExtra = skuOrder.getExtra();
                skuOrderExtra.put(TradeConstants.SKU_ORDER_SHIPMENT_ID, String.valueOf(createResp.getResult()));
                Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(), OrderLevel.SKU, skuOrderExtra);
                if (!response.isSuccess()) {
                    log.error("update sku order：{} extra map to:{} fail,error:{}", skuOrder.getId(), skuOrderExtra, response.getError());
                }
            } catch (Exception e) {
                log.error("update sku shipment id failed,skuOrder id is {},shipmentId is {},caused by {}", skuOrder.getId(), createResp.getResult(), e.getMessage());
            }
        }
        return createResp.getResult();
    }

    /**
     * 是否满足自动创建发货单的校验
     * @param shopOrder 店铺订单
     * @param skuOrders 子单
     * @return 不可以自动创建发货单(false), 可以自动创建发货单(true)
     */
    private boolean commValidateOfOrder(ShopOrder shopOrder, List<SkuOrder> skuOrders) {
        int orderWaitHandleType = 0;
        //3.判断订单有无备注
        if (StringUtils.isNotEmpty(shopOrder.getBuyerNote())) {
            orderWaitHandleType = OrderWaitHandleType.ORDER_HAS_NOTE.value();
            this.updateShipmentNote(shopOrder, orderWaitHandleType);
            return false;
        }
        //4.判断skuCode是否为空,如果存在skuCode为空则不能自动生成发货单
        int count = 0;
        for (SkuOrder skuOrder : skuOrders) {
            if (StringUtils.isEmpty(skuOrder.getSkuCode())) {
                count++;
            }
        }
        if (count > 0) {
            orderWaitHandleType = OrderWaitHandleType.SKU_NOT_MATCH.value();
        }
        if (orderWaitHandleType > 0) {
            //添加备注
            this.updateShipmentNote(shopOrder, orderWaitHandleType);
        }
        return count <= 0;
    }

    /**
     * 是否满足自动创建发货单的校验
     * @param shopOrder 店铺订单
     * @param skuOrders 子单
     * @return 不可以自动创建发货单(false), 可以自动创建发货单(true)
     */
    private boolean autoHandleOrderParam(ShopOrder shopOrder, List<SkuOrder> skuOrders) {
        int orderWaitHandleType = 0;
        //判断skuCode是否为空,如果存在skuCode为空则不能自动生成发货单
        int count = 0;
        for (SkuOrder skuOrder : skuOrders) {
            if (StringUtils.isEmpty(skuOrder.getSkuCode())) {
                count++;
            }
        }
        if (count > 0) {
            orderWaitHandleType = OrderWaitHandleType.SKU_NOT_MATCH.value();
        }
        if (orderWaitHandleType > 0) {
            //添加备注
            this.updateShipmentNote(shopOrder, orderWaitHandleType);
        }
        return count <= 0;
    }

    /**
     * 根据skuCode获取skuOrder
     * @param skuOrders 子单集合
     * @param skuCode   sku代码
     * @return 返回经过过滤的skuOrder记录
     */
    private SkuOrder getSkuOrder(List<SkuOrder> skuOrders, String skuCode) {
        return skuOrders.stream().filter(Objects::nonNull).filter(it -> Objects.equals(it.getSkuCode(), skuCode)).collect(Collectors.toList()).get(0);
    }

    /**
     * 组装发货单参数
     * @param shopOrder   店铺订单
     * @param warehouseId 发货仓主键
     * @return 返回组装的发货单
     */
    private Shipment makeShipment(ShopOrder shopOrder, Long warehouseId, Long shipmentItemFee, Long shipmentDiscountFee,
                                  Long shipmentTotalFee, Long shipmentShipFee, Long shipmentShipDiscountFee,
                                  Long shipmentTotalPrice, Long shopId) {
        Shipment shipment = new Shipment();
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue());
        shipment.setReceiverInfos(findReceiverInfos(shopOrder.getId(), OrderLevel.SHOP));

        //发货仓库信息
        Warehouse warehouse = findWarehouseById(warehouseId);
        Map<String, String> extraMap = Maps.newHashMap();
        ShipmentExtra shipmentExtra = new ShipmentExtra();
        shipmentExtra.setShipmentWay(TradeConstants.MPOS_WAREHOUSE_DELIVER);
        shipmentExtra.setWarehouseId(warehouse.getId());
        shipmentExtra.setWarehouseName(warehouse.getName());
        Map<String, String> warehouseExtra = warehouse.getExtra();
        if (Objects.nonNull(warehouseExtra)) {
            shipmentExtra.setWarehouseOutCode(warehouseExtra.get("outCode") != null ? warehouseExtra.get("outCode") : "");
        }

        //绩效店铺代码
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
        log.info("auto create shipment,step seven");
        String shopCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE, openShop);
        String shopName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME, openShop);
        shipmentExtra.setErpOrderShopCode(shopCode);
        shipmentExtra.setErpOrderShopName(shopName);
        shipmentExtra.setErpPerformanceShopCode(shopCode);
        shipmentExtra.setErpPerformanceShopName(shopName);

        shipmentExtra.setShipmentItemFee(shipmentItemFee);
        //发货单运费金额
        shipmentExtra.setShipmentShipFee(shipmentShipFee);
        //发货单优惠金额
        shipmentExtra.setShipmentDiscountFee(shipmentDiscountFee);
        //发货单总的净价
        shipmentExtra.setShipmentTotalFee(shipmentTotalFee);
        shipmentExtra.setShipmentShipDiscountFee(shipmentShipDiscountFee);
        shipmentExtra.setShipmentTotalPrice(shipmentTotalPrice);
        //添加物流编码
        Map<String, String> shopOrderMap = shopOrder.getExtra();
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())) {
            shipmentExtra.setVendCustID(TradeConstants.JD_VEND_CUST_ID);
        } else {
            String expressCode = shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE);
            if (!org.springframework.util.StringUtils.isEmpty(expressCode)) {
                shipmentExtra.setVendCustID(expressCode);
            } else {
                shipmentExtra.setVendCustID(TradeConstants.OPTIONAL_VEND_CUST_ID);
            }
        }
        shipmentExtra.setOrderHkExpressCode(shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE));
        shipmentExtra.setOrderHkExpressName(shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_NAME));
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));

        shipment.setExtra(extraMap);

        return shipment;
    }

    /**
     * 组装发货单参数
     * @param shopOrder     店铺订单
     * @param deliverShopId 接单店铺id
     * @return 返回组装的发货单
     */
    private Shipment makeShopShipment(ShopOrder shopOrder, Long deliverShopId,String deliverShopName, Long shipmentItemFee, Long shipmentDiscountFee,
                                      Long shipmentTotalFee, Long shipmentShipFee, Long shipmentShipDiscountFee,
                                      Long shipmentTotalPrice, Long shopId) {
        Shipment shipment = new Shipment();
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue());
        shipment.setReceiverInfos(findReceiverInfos(shopOrder.getId(), OrderLevel.SHOP));

        Map<String, String> extraMap = Maps.newHashMap();
        ShipmentExtra shipmentExtra = new ShipmentExtra();
        shipmentExtra.setShipmentWay(TradeConstants.MPOS_SHOP_DELIVER);
        shipmentExtra.setWarehouseId(deliverShopId);
        shipmentExtra.setWarehouseName(deliverShopName);
        shipmentExtra.setTakeWay(shopOrder.getExtra().get(TradeConstants.IS_SINCE));
        shipmentExtra.setIsAppint(shopOrder.getExtra().get(TradeConstants.IS_ASSIGN_SHOP));

        //下单店铺代码
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
        String shopCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE, openShop);
        String shopName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME, openShop);
        shipmentExtra.setErpOrderShopCode(shopCode);
        shipmentExtra.setErpOrderShopName(shopName);
        shipmentExtra.setErpPerformanceShopCode(shopCode);
        shipmentExtra.setErpPerformanceShopName(shopName);

        shipmentExtra.setShipmentItemFee(shipmentItemFee);
        //发货单运费金额
        shipmentExtra.setShipmentShipFee(shipmentShipFee);
        //发货单优惠金额
        shipmentExtra.setShipmentDiscountFee(shipmentDiscountFee);
        //发货单总的净价
        shipmentExtra.setShipmentTotalFee(shipmentTotalFee);
        shipmentExtra.setShipmentShipDiscountFee(shipmentShipDiscountFee);
        shipmentExtra.setShipmentTotalPrice(shipmentTotalPrice);
        //添加物流编码
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())) {
            shipmentExtra.setVendCustID(TradeConstants.JD_VEND_CUST_ID);
        } else {
            shipmentExtra.setVendCustID(TradeConstants.OPTIONAL_VEND_CUST_ID);
        }
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));

        shipment.setExtra(extraMap);

        return shipment;
    }



    /**
     * 查找收货人信息
     * @param orderId    订单主键
     * @param orderLevel 订单级别 店铺订单or子单
     * @return 收货人信息的json串
     */
    private String findReceiverInfos(Long orderId, OrderLevel orderLevel) {

        List<ReceiverInfo> receiverInfos = doFindReceiverInfos(orderId, orderLevel);

        if (CollectionUtils.isEmpty(receiverInfos)) {
            log.error("receiverInfo not found where orderId={}", orderId);
            throw new JsonResponseException("receiver.info.not.found");
        }

        ReceiverInfo receiverInfo = receiverInfos.get(0);

        try {
            return objectMapper.writeValueAsString(receiverInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 查找收货人信息
     * @param orderId    订单主键
     * @param orderLevel 订单级别 店铺订单or子单
     * @return 收货人信息的list集合
     */
    private List<ReceiverInfo> doFindReceiverInfos(Long orderId, OrderLevel orderLevel) {
        Response<List<ReceiverInfo>> receiversResp = receiverInfoReadService.findByOrderId(orderId, orderLevel);
        if (!receiversResp.isSuccess()) {
            log.error("fail to find receiver info by order id={},and order level={},cause:{}",
                    orderId, orderLevel.getValue(), receiversResp.getError());
            throw new JsonResponseException(receiversResp.getError());
        }
        return receiversResp.getResult();
    }

    /**
     * 获取发货仓库信息
     * @param warehouseId 仓库主键
     * @return 仓库信息
     */
    private Warehouse findWarehouseById(Long warehouseId) {
        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if (!warehouseRes.isSuccess()) {
            log.error("find warehouse by id:{} fail,error:{}", warehouseId, warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }

        return warehouseRes.getResult();
    }

    /**
     * 发货单中填充sku订单信息
     * @param skuOrders             子单集合
     * @param skuOrderIdAndQuantity 子单的主键和数量的集合
     * @return shipmentItem的集合
     */
    private List<ShipmentItem> makeShipmentItems(List<SkuOrder> skuOrders, Map<Long, Integer> skuOrderIdAndQuantity) {
        Map<Long, SkuOrder> skuOrderMap = skuOrders.stream().filter(Objects::nonNull).collect(Collectors.toMap(SkuOrder::getId, it -> it));
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithExpectedSize(skuOrderIdAndQuantity.size());
        for (Long skuOrderId : skuOrderIdAndQuantity.keySet()) {
            ShipmentItem shipmentItem = new ShipmentItem();
            SkuOrder skuOrder = skuOrderMap.get(skuOrderId);
            if (skuOrder.getShipmentType() != null && Objects.equals(skuOrder.getShipmentType(), 1)) {
                shipmentItem.setIsGift(true);
            } else {
                shipmentItem.setIsGift(false);
            }
            shipmentItem.setQuantity(skuOrderIdAndQuantity.get(skuOrderId));
            shipmentItem.setRefundQuantity(0);
            shipmentItem.setSkuOrderId(skuOrderId);
            shipmentItem.setSkuName(skuOrder.getItemName());
            shipmentItem.setSkuOutId(skuOrder.getOutId());
            shipmentItem.setSkuPrice(Math.round(skuOrder.getOriginFee() / shipmentItem.getQuantity()));
            //积分
            String originIntegral = "";
            try {
                originIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_INTEGRAL, skuOrder);
            } catch (JsonResponseException e) {
                log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_INTEGRAL);
            }
            Integer integral = StringUtils.isEmpty(originIntegral) ? 0 : Integer.valueOf(originIntegral);
            shipmentItem.setIntegral(this.getIntegral(integral, skuOrder.getQuantity(), skuOrderIdAndQuantity.get(skuOrderId)));
            Long disCount = skuOrder.getDiscount() + Long.valueOf(this.getShareDiscount(skuOrder));
            shipmentItem.setSkuDiscount(this.getDiscount(skuOrder.getQuantity(), skuOrderIdAndQuantity.get(skuOrderId), Math.toIntExact(disCount)));
            shipmentItem.setCleanFee(this.getCleanFee(shipmentItem.getSkuPrice(), shipmentItem.getSkuDiscount(), shipmentItem.getQuantity()));
            shipmentItem.setCleanPrice(this.getCleanPrice(shipmentItem.getCleanFee(), shipmentItem.getQuantity()));
            shipmentItem.setOutSkuCode(skuOrder.getOutSkuId());
            shipmentItem.setSkuCode(skuOrder.getSkuCode());
            //商品id
            String outItemId = "";
            try {
                outItemId = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.MIDDLE_OUT_ITEM_ID, skuOrder);
                log.info("auto create shipment,step five");
            } catch (Exception e) {
                log.info("outItemmId is not exist");
            }
            shipmentItem.setItemId(outItemId);
            //商品属性
            shipmentItem.setAttrs(skuOrder.getSkuAttrs());

            shipmentItems.add(shipmentItem);

        }


        return shipmentItems;
    }


    /**
     * @param skuQuantity     sku订单中商品的数量
     * @param shipSkuQuantity 发货的sku商品的数量
     * @param skuDiscount     sku订单中商品的折扣
     * @return 返回四舍五入的计算结果, 得到发货单中的sku商品的折扣
     */
    private Integer getDiscount(Integer skuQuantity, Integer shipSkuQuantity, Integer skuDiscount) {
        return Math.round(skuDiscount * shipSkuQuantity / skuQuantity);
    }

    /**
     * 计算总净价
     * @param skuPrice        商品原价
     * @param discount        发货单中sku商品的折扣
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return 返回sku商品总的净价
     */
    private Integer getCleanFee(Integer skuPrice, Integer discount, Integer shipSkuQuantity) {

        return skuPrice * shipSkuQuantity - discount;
    }

    /**
     * 计算商品净价
     * @param cleanFee        商品总净价
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return 返回sku商品净价
     */
    private Integer getCleanPrice(Integer cleanFee, Integer shipSkuQuantity) {
        return Math.round(cleanFee / shipSkuQuantity);
    }

    /**
     * 计算积分
     * @param integral            sku订单获取的积分
     * @param skuQuantity         sku订单总的数量
     * @param shipmentSkuQuantity 发货单中该sku订单的数量
     * @return 获取发货单中sku订单的积分
     */
    private Integer getIntegral(Integer integral, Integer skuQuantity, Integer shipmentSkuQuantity) {
        return Math.round(integral * shipmentSkuQuantity / skuQuantity);
    }

    /**
     * 判断是否存在有效的发货单
     * @param shopOrderId 店铺订单主键
     * @return true:已经计算过发货单,false:没有计算过发货单
     */
    private boolean isShipmentFeeCalculated(long shopOrderId) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new JsonResponseException("shipment.find.fail");
        }
        log.info("auto create shipment,step six");
        //获取有效的销售发货单
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(),MiddleShipmentsStatus.REJECTED.getValue())).
                filter(it -> Objects.equals(it.getType(), ShipmentType.SALES_SHIP.value())).collect(Collectors.toList());
        int count = 0;
        for (Shipment shipment : shipments) {
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if (shipmentExtra.getShipmentShipFee() > 0) {
                count++;
            }
        }
        //如果已经有发货单计算过运费,返回true
        return count > 0;
    }

    private String getShareDiscount(SkuOrder skuOrder) {
        String skuShareDiscount = "";
        try {
            skuShareDiscount = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_SHARE_DISCOUNT, skuOrder);
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_SHARE_DISCOUNT);
        }
        return StringUtils.isEmpty(skuShareDiscount) ? "0" : skuShareDiscount;
    }

    /**
     * 更新发货单同步淘宝的状态
     *
     * @param shipment       发货单
     * @param orderOperation 可用的操作
     * @return
     */
    public Response<Boolean> updateShipmentSyncTaobaoStatus(Shipment shipment, OrderOperation orderOperation) {
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Flow flow = flowPicker.pickSyncTaobao();
        //判断当前状态是否可以操作
        if (!flow.operationAllowed(shipmentExtra.getSyncTaobaoStatus(), orderOperation)) {
            log.error("shipment(id:{}) current status:{} not allow operation:{}", shipment.getId(), shipmentExtra.getSyncTaobaoStatus(), orderOperation.getText());
            return Response.fail("sync.taobao.status.not.allow.current.operation");
        }
        //获取下一步状态
        Integer targetStatus = flow.target(shipmentExtra.getSyncTaobaoStatus(), orderOperation);
        shipmentExtra.setSyncTaobaoStatus(targetStatus);
        Map<String, String> extraMap = shipment.getExtra();
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
        shipment.setExtra(extraMap);
        Response<Boolean> updateRes = shipmentWriteService.update(shipment);
        if (!updateRes.isSuccess()) {
            log.error("update shipment:{} fail,error:{}", shipment, updateRes.getError());
            return Response.fail(updateRes.getError());
        }
        return Response.ok();
    }

    /**
     * 订单拆单派单
     * @param shopOrder  订单
     */
    public void toDispatchOrder(ShopOrder shopOrder){
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(),
                MiddleOrderStatus.WAIT_HANDLE.getValue());
        if (skuOrders.size() == 0) {
            return;
        }
        //判断是否满足自动生成发货单
        if (!commValidateOfOrder(shopOrder, skuOrders)) {
            return;
        }
        this.toDispatchOrder(shopOrder,Collections.EMPTY_LIST);
    }

    /**
     * 拆单派单(拒单后重新派单)
     * @param skuCodeAndQuantities 被拒单的商品
     * @param shopOrder            订单
     */
    public void toDispatchOrder(ShopOrder shopOrder,List<SkuCodeAndQuantity> skuCodeAndQuantities) {
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
        List<SkuOrder> skuOrders = skuOrdersRes.getResult();
        if(isFirst){
            //获取skuCode,数量的集合
            skuCodeAndQuantities = Lists.newArrayListWithCapacity(skuOrders.size());
            for (SkuOrder skuOrder:skuOrders) {
                SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
                skuCodeAndQuantity.setQuantity(skuOrder.getQuantity());
                skuCodeAndQuantities.add(skuCodeAndQuantity);
            }
        }
        Response<DispatchOrderItemInfo> response = dispatchOrderEngine.toDispatchOrder(shopOrder, receiveInfosRes.getResult().get(0), skuCodeAndQuantities);
        if (!response.isSuccess()) {
            log.error("dispatch fail,error:{}", response.getError());
            //记录未处理原因
            Map<String, String> extraMap = shopOrder.getExtra();
            extraMap.put(TradeConstants.NOT_AUTO_CREATE_SHIPMENT_NOTE, messageSource.getMessage(response.getError(),null,Locale.CHINA));
            Response<Boolean> rltRes = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, extraMap);
            if (!rltRes.isSuccess()) {
                log.error("update shopOrder：{} extra map to:{} fail,error:{}", shopOrder.getId(), extraMap, rltRes.getError());
            }
            if(!isFirst){
                //如果不是第一次派单，将订单状态恢复至待处理
                this.makeSkuOrderWaitHandle(skuCodeAndQuantities,skuOrders);
            }
            throw new JsonResponseException(response.getError());
        }
        DispatchOrderItemInfo dispatchOrderItemInfo = response.getResult();
        log.info("MPOS DISPATCH SUCCESS result:{}",dispatchOrderItemInfo);

        for (WarehouseShipment warehouseShipment : dispatchOrderItemInfo.getWarehouseShipments()) {
            Long shipmentId = this.createShipment(shopOrder, skuOrders, warehouseShipment);
            if (shipmentId != null) {
                Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
                if (!shipmentRes.isSuccess()) {
                    log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
                }
                Shipment shipment = shipmentRes.getResult();
                if(isFirst)
                    orderWriteLogic.updateSkuHandleNumber(shipment.getSkuInfos());
                this.handleSyncShipment(shipment,1,shopOrder);
            }
        }
        for (ShopShipment shopShipment : dispatchOrderItemInfo.getShopShipments()) {
            Long shipmentId = this.createShopShipment(shopOrder, skuOrders, shopShipment);
            if (shipmentId != null) {
                Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
                if (!shipmentRes.isSuccess()) {
                    log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
                }
                Shipment shipment = shipmentRes.getResult();
                if(isFirst)
                    orderWriteLogic.updateSkuHandleNumber(shipment.getSkuInfos());
                this.handleSyncShipment(shipment,2,shopOrder);
            }
        }

        List<SkuCodeAndQuantity> skuCodeAndQuantityList = dispatchOrderItemInfo.getSkuCodeAndQuantities();
        if(!CollectionUtils.isEmpty(skuCodeAndQuantityList)){
            //如果是恒康pos订单，暂不处理
            if(!shopOrder.getExtra().containsKey("isHkPosOrder")){
                //取消子单
                for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantityList) {
                    SkuOrder skuOrder = this.getSkuOrder(skuOrders, skuCodeAndQuantity.getSkuCode());
                    orderWriteService.skuOrderStatusChanged(skuOrder.getId(),skuOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue());
                    //添加取消原因
                    Map<String,String> skuOrderExtra = skuOrder.getExtra();
                    skuOrderExtra.put(TradeConstants.SKU_ORDER_CANCEL_REASON,TradeConstants.SKU_CANNOT_BE_DISPATCHED);
                    orderWriteService.updateOrderExtra(skuOrder.getId(),OrderLevel.SKU,skuOrderExtra);
                }
                syncMposOrderLogic.syncNotDispatcherSkuToMpos(shopOrder,skuCodeAndQuantityList);
            }else{
                if(!isFirst){
                    //如果不是第一次派单，将订单状态恢复至待处理
                    this.makeSkuOrderWaitHandle(skuCodeAndQuantityList,skuOrders);
                }
            }
        }
        if(isFirst)
            this.updateShipmentNote(shopOrder, OrderWaitHandleType.HANDLE_DONE.value());
    }

    /**
     * 将子单置为待处理
     * @param skuCodeAndQuantities
     * @param skuOrders
     */
    private void makeSkuOrderWaitHandle(List<SkuCodeAndQuantity> skuCodeAndQuantities,List<SkuOrder> skuOrders){
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            SkuOrder skuOrder = this.getSkuOrder(skuOrders, skuCodeAndQuantity.getSkuCode());
            orderWriteService.skuOrderStatusChanged(skuOrder.getId(),skuOrder.getStatus(), MiddleOrderStatus.WAIT_HANDLE.getValue());
            Map<String, String> extraMap = skuOrder.getExtra();
            Integer waitHandleNumber = skuOrder.getQuantity();
            extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(waitHandleNumber));
            Response<Boolean> response1 = orderWriteService.updateOrderExtra(skuOrder.getId(), OrderLevel.SKU, extraMap);
            if (!response1.isSuccess()) {
                log.error("update sku order：{} extra map to:{} fail,error:{}", skuOrder.getId(), extraMap, response1.getError());
            }
        }
    }

    /**
     * 处理同步发货单
     * @param shipment  发货单
     * @param type      类型 1.仓发 2.店发
     * @param shopOrder 订单
     */
    private void handleSyncShipment(Shipment shipment,Integer type,ShopOrder shopOrder){
        try{
            if(Objects.equals(type,1)){
                //发货单同步恒康
                log.info("sync shipment(id:{}) to hk",shipment.getId());
                Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipment);
                if (!syncRes.isSuccess()) {
                    log.error("sync shipment(id:{}) to hk fail,error:{}", shipment.getId(), syncRes.getError());
                }
            }else if(Objects.equals(type,2)){
                //同步mpos
                log.info("sync shipment(id:{}) to mpos", shipment.getId());
                Response response = syncMposShipmentLogic.syncShipmentToMpos(shipment);
                if (!response.isSuccess()) {
                    log.error("sync shipment(id:{}) to mpos fail", shipment.getId());
                }else{
                    //指定门店暂不处理
                    if(Objects.equals(shopOrder.getExtra().get(TradeConstants.IS_ASSIGN_SHOP),1)) {
                        return ;
                    }
                    //邮件提醒接单店铺
                    ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                    Response<Shop> shopResponse = shopReadService.findById(shipmentExtra.getWarehouseId());
                    if(!shopResponse.isSuccess()){
                        log.error("email notify shop(id:{}) failed,cause:{}",shipmentExtra.getWarehouseId(),shopResponse.getError());
                    }
                    Shop shop = shopResponse.getResult();
                    ShopExtraInfo extraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                    if(extraInfo.getEmail() != null){
                        Map<String,Serializable> context = Maps.newHashMap();
                        context.put("shopName",shop.getName());
                        context.put("orderId",shopOrder.getOutId());
                        msgService.send(extraInfo.getEmail(),"email.order.confirm",context,null);
                    }
                }
            }
        }catch (Exception e){
            log.error("sync shipment(id:{}) failed,cause:{}",shipment.getId(), Throwables.getStackTraceAsString(e));
        }
    }


    /** 单个发货单撤销
     * @param shipmentId 发货单主键
     * @return
     */
    public boolean rollbackShipment(Long shipmentId) {

        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipmentId);
        //判断该发货单是否可以撤销，已取消的或者已经发货的发货单是不能撤销的
        if (Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())||shipment.getStatus()>MiddleShipmentsStatus.SHIPPED.getValue()){
            throw new JsonResponseException("invalid.shipment.status.can.not.cancel");
        }
        //判断发货单类型，如果发货单类型是销售发货单正常处理
        if (Objects.equals(orderShipment.getType(),ShipmentType.SALES_SHIP.value())){
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            List<String> skuCodes = shipmentItems.stream().map(ShipmentItem::getSkuCode).collect(Collectors.toList());
            List<SkuOrder> skuOrders = this.getSkuOrders(skuCodes,shopOrder.getId());
            int count= 0 ;//计数器用来记录是否有发货单取消失败的
            if (!this.cancelShipment(shipment,1))
            {
                count++;
            }
            //获取该订单下所有的子单和发货单
            if (count>0){
                middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.REVOKE_FAIL.toOrderOperation());
            }else{
                middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.REVOKE.toOrderOperation());
            }
            if (count>0){
                return false;
            }else{
                return true;
            }
        }
        //换货
        if (Objects.equals(orderShipment.getType(),ShipmentType.EXCHANGE_SHIP.value())){
            Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
            if (Objects.equals(refund.getStatus(), MiddleRefundStatus.WAIT_CONFIRM_RECEIVE.getValue())){
                return false;
            }
            int count= 0 ;//计数器用来记录是否有发货单取消失败的
            if (!this.cancelShipment(shipment,1))
            {
                count++;
            }
            //换货发货单发货之前可以取消发货单，将已经处理的发货单数量恢复成初始状态，如果所有的发货单的alreadyHandleNumber已经是0，则将售后待状态变更为退货完成待创建发货单,取消发货单失败则订单状态不作变更
            if (count==0){
                this.rollbackChangeRefund(shipment, orderShipment, refund);
            }else {
                return false;
            }

        }
        //丢件补发
        if (Objects.equals(orderShipment.getType(),3)){
            Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
            if (Objects.equals(refund.getStatus(), MiddleRefundStatus.LOST_SHIPPED.getValue())){
                return false;
            }
            int count= 0 ;//计数器用来记录是否有发货单取消失败的
            if (!this.cancelShipment(shipment,1))
            {
                count++;
            }
            //丢件补发类型的发货单在发货之前可以取消发货单，将已经处理的发货单的数量恢复成初始状态，如果所有的发货单的alreadyHandleNumber已经是0，则将售后单状态恢复成待创建发货单，取消发货单失败订单状态不作变更
            if (count==0){
                this.rollbackLostRefund(shipment,orderShipment,refund);
            }else{
                return false;
            }
        }
        return true;
    }

    /**
     * 撤销售后单发货--换货类型
     * @param shipment 发货单
     * @param orderShipment 发货单
     * @param refund
     */
    private void rollbackChangeRefund(Shipment shipment, OrderShipment orderShipment, Refund refund) {
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        Map<String,Integer> skuCodesAndQuantity = Maps.newHashMap();
        shipmentItems.forEach(shipmentItem -> {
            skuCodesAndQuantity.put(shipmentItem.getSkuCode(),shipmentItem.getQuantity());
        });
        List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
        refundChangeItems.forEach(refundChangeItem -> {
            if (skuCodesAndQuantity.containsKey(refundChangeItem.getSkuCode())){
                refundChangeItem.setAlreadyHandleNumber(refundChangeItem.getAlreadyHandleNumber()==null?0:refundChangeItem.getAlreadyHandleNumber()-skuCodesAndQuantity.get(refundChangeItem.getSkuCode()));
            }
        });
        Map<String, String> refundExtra = refund.getExtra();
        refundExtra.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(refundChangeItems));
        refund.setExtra(refundExtra);
        refundWriteLogic.update(refund);
        Refund newRefund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
        List<RefundItem> newRefundChangeItems = refundReadLogic.findRefundChangeItems(newRefund);
        int newCount=0;
        for (RefundItem refundItem:newRefundChangeItems){
            if (refundItem.getAlreadyHandleNumber()>0){
                newCount++;
            }
        }
        //说明所有的发货单都已经取消了，这个时候可以将换货单的状态改为退货完成待创建发货单的状态了
        if (newCount==0){
            updateRefundStatus(refund);
        }
    }

    /**
     * 撤销售后单发货---丢件补发类型
     * @param shipment 发货单
     * @param orderShipment 发货单售后单关联表
     * @param refund 售后单
     */
    private void rollbackLostRefund(Shipment shipment, OrderShipment orderShipment, Refund refund) {
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        Map<String,Integer> skuCodesAndQuantity = Maps.newHashMap();
        shipmentItems.forEach(shipmentItem -> {
            skuCodesAndQuantity.put(shipmentItem.getSkuCode(),shipmentItem.getQuantity());
        });
        List<RefundItem> refundLostItems = refundReadLogic.findRefundLostItems(refund);
        refundLostItems.forEach(refundLostItem -> {
            if (skuCodesAndQuantity.containsKey(refundLostItem.getSkuCode())){
                refundLostItem.setAlreadyHandleNumber(refundLostItem.getAlreadyHandleNumber()==null?0:refundLostItem.getAlreadyHandleNumber()-skuCodesAndQuantity.get(refundLostItem.getSkuCode()));
            }
        });
        Map<String, String> refundExtra = refund.getExtra();
        refundExtra.put(TradeConstants.REFUND_LOST_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(refundLostItems));
        refund.setExtra(refundExtra);
        refundWriteLogic.update(refund);
        Refund newRefund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
        List<RefundItem> newRefundChangeItems = refundReadLogic.findRefundLostItems(newRefund);
        int newCount=0;
        for (RefundItem refundItem:newRefundChangeItems){
            if (refundItem.getAlreadyHandleNumber()>0){
                newCount++;
            }
        }
        //说明所有的发货单都已经取消了，这个时候可以将换货单的状态改为退货完成待创建发货单的状态了
        if (newCount==0){
            updateRefundStatus(refund);
        }
    }

    /**
     * 撤销售后单发货状态
     * @param refund 售后单
     */
    private void updateRefundStatus(Refund refund) {
        Flow flow = flowPicker.pickAfterSales();
        Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.REVOKE.toOrderOperation());
        Response<Boolean> updateStatusRes = refundWriteService.updateStatus(refund.getId(),targetStatus);
        if(!updateStatusRes.isSuccess()){
            log.error("update refund(id:{}) status to:{} fail,error:{}",refund,targetStatus,updateStatusRes.getError());
        }
    }

    /**
     * 获取指定的子单集合
     * @param skuCodes 商品条码集合
     * @param shopOrderId 店铺订单集合
     * @return
     */
    private List<SkuOrder> getSkuOrders(List<String> skuCodes,Long shopOrderId){
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrderId);
        List<SkuOrder> skuOrdersFilter = Lists.newArrayList();
        skuOrders.forEach(skuOrder -> {
           if (skuCodes.contains(skuOrder.getSkuCode())){
               skuOrdersFilter.add(skuOrder);
           }
        });
        return skuOrdersFilter;
    }
}
