package com.pousheng.middle.web.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.trade.RefundShipmentEvent;
import com.pousheng.middle.web.events.trade.UnLockStockEvent;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 发货单相关api （以 order shipment 为发货单）
 * Created by songrenfei on 2017/6/20
 */
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.SHIPMENT)
public class Shipments {

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @RpcConsumer
    private WarehouseSkuReadService warehouseSkuReadService;
    @Autowired
    private MiddleOrderFlowPicker orderFlowPicker;
    @Autowired
    private MiddleShipmentWriteService middleShipmentWriteService;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private WarehouseSkuWriteService warehouseSkuWriteService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private StockPusher stockPusher;
    @Autowired
    private PermissionUtil permissionUtil;
    @RpcConsumer
    private OrderWriteService orderWriteService;


    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();


    /**
     * 发货单分页 注意查的是 orderShipment
     * @param shipmentCriteria 查询参数
     * @return 发货单分页信息
     */
    @RequestMapping(value = "/api/shipment/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ShipmentPagingInfo> findBy(OrderShipmentCriteria shipmentCriteria) {
        if(shipmentCriteria.getEndAt()!=null){
            shipmentCriteria.setEndAt(new DateTime(shipmentCriteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }

        shipmentCriteria.setShopIds(permissionUtil.getCurrentUserCanOperateShopIDs());

        //判断查询的发货单类型
        if (Objects.equals(shipmentCriteria.getType(),ShipmentType.EXCHANGE_SHIP.value())){
            shipmentCriteria.setAfterSaleOrderId(shipmentCriteria.getOrderId());
            shipmentCriteria.setOrderId(null);
        }
        Response<Paging<ShipmentPagingInfo>> response =  orderShipmentReadService.findBy(shipmentCriteria);
        if(!response.isSuccess()){
            log.error("find shipment by criteria:{} fail,error:{}",shipmentCriteria,response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<ShipmentPagingInfo> shipmentPagingInfos = response.getResult().getData();
        Flow flow = orderFlowPicker.pickShipments();
        shipmentPagingInfos.forEach(shipmentPagingInfo ->{
            Shipment shipment = shipmentPagingInfo.getShipment();
            try {
                shipmentPagingInfo.setOperations(flow.availableOperations(shipment.getStatus()));
                shipmentPagingInfo.setShipmentExtra(shipmentReadLogic.getShipmentExtra(shipment));
            }catch (JsonResponseException e){
                log.error("complete paging info fail,error:{}", e.getMessage());
            }catch (Exception e){
                log.error("complete paging info fail,cause:{}", Throwables.getStackTraceAsString(e));
            }

        });
        return response.getResult();
    }


    //发货单详情
    @RequestMapping(value = "/api/shipment/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ShipmentDetail findDetail(@PathVariable(value = "id") Long shipmentId) {

        return shipmentReadLogic.orderDetail(shipmentId);
    }


    /**
     * 订单下的发货单
     * @param shopOrderId 店铺订单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/order/{id}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Shipment> shipments(@PathVariable("id") Long shopOrderId) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!response.isSuccess()){
            log.error("find shipment by shopOrderId ({}) failed,caused by ({})",shopOrderId,response.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        return response.getResult();
    }

    /**
     * 换货单下的发货单
     * @param afterSaleOrderId 售后单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/refund/{id}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderShipment> changeShipments(@PathVariable("id") Long afterSaleOrderId) {
        return shipmentReadLogic.findByAfterOrderIdAndType(afterSaleOrderId);
    }



    //获取发货单商品明细
    @RequestMapping(value = "/api/shipment/{id}/items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShipmentItem> shipmentItems(@PathVariable("id") Long shipmentId) {

        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        return shipmentReadLogic.getShipmentItems(shipment);

    }


    /**
     * 判断发货单是否有效
     * 1、是否属于当前订单
     * 2、发货单状态是否为已发货
     * @param shipmentId 发货单主键id
     * @param orderId 交易订单id
     * @return 是否有效
     */
    @RequestMapping(value = "/api/shipment/{id}/check/exist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> checkExist(@PathVariable("id") Long shipmentId,@RequestParam Long orderId){

        try {

            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipmentId);
            //是否属于当前订单
            if(!Objects.equals(orderShipment.getOrderId(),orderId)){
                log.error("shipment(id:{}) order id:{} not equal :{}",shipmentId,orderShipment.getOrderId(),orderId);
                return Response.fail("shipment.not.belong.to.order");
            }

            //发货单状态是否为恒康已经确认收货
            if(!Objects.equals(orderShipment.getStatus(),MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                    && !Objects.equals(orderShipment.getStatus(),MiddleShipmentsStatus.SHIPPED.getValue())){
                log.error("shipment(id:{}) current status:{} can not apply after sale",shipmentId,orderShipment.getStatus());
                return Response.fail("shipment.current.status.not.allow.apply.after.sale");
            }
        }catch (JsonResponseException e){
            log.error("check  shipment(id:{}) is exist fail,error:{}",shipmentId,e.getMessage());
            return Response.fail(e.getMessage());
        }

        return Response.ok(Boolean.TRUE);

    }

    /**
     * 自动创建发货单
     * @param shopOrderId
     */
    @RequestMapping(value = "api/shipment/{id}/auto/create/shipment",method = RequestMethod.PUT)
    public void autoCreateSaleShipment(@PathVariable("id") Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        shipmentWiteLogic.doAutoCreateShipment(shopOrder);
    }

    /**
     * todo 发货成功调用大度仓库接口减库存 ，扣减成功再创建发货单
     * 生成销售发货单
     * 发货成功：
     * 1. 更新子单的处理数量
     * 2. 更新子单的状态（如果子单全部为已处理则更新店铺订单为已处理）
     * @param shopOrderId 店铺订单id
     * @return 发货单id集合
     */
    @RequestMapping(value = "/api/order/{id}/ship", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("生成销售发货单")
    public List<Long> createSalesShipment(@PathVariable("id") @OperationLogParam Long shopOrderId,
                                    @RequestParam(value = "dataList") String dataList) {

        List<ShipmentRequest> requestDataList = JsonMapper.nonEmptyMapper().fromJson(dataList, JsonMapper.nonEmptyMapper().createCollectionType(List.class,ShipmentRequest.class));
        List<Long> shipmentIds = Lists.newArrayList();
        //用于判断运费是否已经算过
        int shipmentFeeCount=0;
        for (ShipmentRequest shipmentRequest:requestDataList){
            String data =  JsonMapper.nonEmptyMapper().toJson(shipmentRequest.getData());
            Long warehouseId = shipmentRequest.getWarehouseId();
            Map<Long, Integer> skuOrderIdAndQuantity = analysisSkuOrderIdAndQuantity(data);
            ShopOrder shopOrder =  orderReadLogic.findShopOrderById(shopOrderId);
            if (!orderReadLogic.validateCompanyCode(warehouseId,shopOrder.getShopId())){
                throw new JsonResponseException("warehouse.must.be.in.one.company");
            }
            //获取子单商品
            List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
            skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
            List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
            Map<String,Integer> skuCodeAndQuantityMap = skuOrders.stream().filter(Objects::nonNull)
                    .collect(Collectors.toMap(SkuOrder::getSkuCode, it -> skuOrderIdAndQuantity.get(it.getId())));
            //检查库存是否充足
            checkStockIsEnough(warehouseId,skuCodeAndQuantityMap);
            //检查商品是不是一次发货还是拆成数次发货,如果不是一次发货抛出异常
            checkSkuCodeAndQuantityLegal(skuOrderIdAndQuantity);
            //封装发货信息
            List<ShipmentItem> shipmentItems = makeShipmentItems(skuOrders,skuOrderIdAndQuantity);
            //发货单商品金额
            Long shipmentItemFee=0L;
            //发货单总的优惠
            Long shipmentDiscountFee=0L;
            //发货单总的净价
            Long shipmentTotalFee=0L;
            //运费
            Long shipmentShipFee=0L;
            //运费优惠
            Long shipmentShipDiscountFee=0L;

            //判断运费是否已经加过
            if (!shipmentReadLogic.isShipmentFeeCalculated(shopOrderId)&&shipmentFeeCount==0){
                shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee()==null?0:shopOrder.getOriginShipFee());
                shipmentShipDiscountFee=shipmentShipFee-Long.valueOf(shopOrder.getShipFee()==null?0:shopOrder.getShipFee());
                shipmentFeeCount++;
            }
            for (ShipmentItem shipmentItem : shipmentItems) {
                shipmentItemFee = shipmentItem.getSkuPrice()*shipmentItem.getQuantity() + shipmentItemFee;
                shipmentDiscountFee = (shipmentItem.getSkuDiscount()==null?0:shipmentItem.getSkuDiscount())+shipmentDiscountFee;
                shipmentTotalFee = shipmentItem.getCleanFee()+shipmentTotalFee;
            }
            //订单总金额(运费优惠已经包含在子单折扣中)=商品总净价+运费
            Long shipmentTotalPrice=shipmentTotalFee+shipmentShipFee-shipmentShipDiscountFee;

            Shipment shipment = makeShipment(shopOrderId,warehouseId,shipmentItemFee,shipmentDiscountFee,shipmentTotalFee
                    ,shipmentShipFee,ShipmentType.SALES_SHIP.value(),shipmentShipDiscountFee,shipmentTotalPrice,shopOrder.getShopId());
            shipment.setSkuInfos(skuOrderIdAndQuantity);
            Map<String,String> extraMap = shipment.getExtra();
            extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO,JSON_MAPPER.toJson(shipmentItems));
            shipment.setExtra(extraMap);
            shipment.setShopId(shopOrder.getShopId());
            shipment.setShopName(shopOrder.getShopName());
            //锁定库存
            Response<Boolean> lockStockRlt = lockStock(shipment);
            if (!lockStockRlt.isSuccess()){
                log.error("this shipment can not unlock stock,shipment id is :{}",shipment.getId());
                throw new JsonResponseException("lock.stock.error");
            }

            //创建发货单
            Response<Long> createResp = shipmentWriteService.create(shipment, Lists.newArrayList(shopOrderId), OrderLevel.SHOP);
            if (!createResp.isSuccess()) {
                log.error("fail to create shipment:{} for order(id={}),and level={},cause:{}",
                        shipment, shopOrderId, OrderLevel.SHOP.getValue(), createResp.getError());
                throw new JsonResponseException(createResp.getError());
            }


            Long shipmentId = createResp.getResult();
            shipmentIds.add(shipmentId);

            Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
            if (!shipmentRes.isSuccess()) {
                log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
            }
            //生成发货单之后需要将发货单id添加到子单中
            for (SkuOrder skuOrder:skuOrders){
                try{
                    Map<String,String> skuOrderExtra = skuOrder.getExtra();
                    skuOrderExtra.put(TradeConstants.SKU_ORDER_SHIPMENT_ID, String.valueOf(shipmentId));
                    Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(), OrderLevel.SKU, skuOrderExtra);
                    if (!response.isSuccess()) {
                        log.error("update sku order：{} extra map to:{} fail,error:{}", skuOrder.getId(), skuOrderExtra, response.getError());
                    }
                }catch (Exception e){
                    log.error("update sku shipment id failed,skuOrder id is {},shipmentId is {},caused by {}",skuOrder.getId(),shipmentId,e.getMessage());
                }
            }
            try{
                orderWriteLogic.updateSkuHandleNumber(shipmentRes.getResult().getSkuInfos());
            }catch (ServiceException e){
                log.error("shipment id is {} update sku handle number failed.caused by {}",shipmentId,e.getMessage());
            }
            //销售发货单需要判断预售商品是否已经支付完尾款
            Map<String,String> shopOrderExtra = shopOrder.getExtra();
            String isStepOrder = shopOrderExtra.get(TradeConstants.IS_STEP_ORDER);
            String stepOrderStatus = shopOrderExtra.get(TradeConstants.STEP_ORDER_STATUS);
            if (!org.apache.commons.lang3.StringUtils.isEmpty(isStepOrder)&&Objects.equals(isStepOrder,"true")){
                if (!org.apache.commons.lang3.StringUtils.isEmpty(stepOrderStatus)&&Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(),Integer.valueOf(stepOrderStatus))){
                    continue;
                }
            }
            Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipmentRes.getResult());
            if (!syncRes.isSuccess()) {
                log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
            }
        }
        return shipmentIds;
    }



    /**
     * todo 发货成功调用大度仓库接口减库存 ，扣减成功再创建发货单
     * 生成换货发货单
     * 发货成功：
     * 1. 更新子单的处理数量
     * 2. 更新子单的状态（如果子单全部为已处理则更新店铺订单为已处理）
     * @param refundId 换货单id
     * @param dataList skuCode-quantity 以及仓库id的集合
     * @param shipType 2.换货，3.丢件补发
     * @return 发货单id
     */
    @RequestMapping(value = "/api/refund/{id}/ship", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("生成换货发货单")
    public List<Long> createAfterShipment(@PathVariable("id") @OperationLogParam Long refundId,
                                    @RequestParam(value = "dataList") String dataList,@RequestParam(required = false,defaultValue = "2") Integer shipType) {
        List<ShipmentRequest> requestDataList = JsonMapper.nonEmptyMapper().fromJson(dataList, JsonMapper.nonEmptyMapper().createCollectionType(List.class,ShipmentRequest.class));
        List<Long> shipmentIds = Lists.newArrayList();
        for (ShipmentRequest shipmentRequest:requestDataList){
            String data =  JsonMapper.nonEmptyMapper().toJson(shipmentRequest.getData());
            Long warehouseId = shipmentRequest.getWarehouseId();
            Map<String, Integer> skuCodeAndQuantity = analysisSkuCodeAndQuantity(data);
            Refund refund = refundReadLogic.findRefundById(refundId);
            if (!orderReadLogic.validateCompanyCode(warehouseId,refund.getShopId())){
                throw new JsonResponseException("warehouse.must.be.in.one.company");
            }
            //只有售后类型是换货的,并且处理状态为待发货的售后单才能创建发货单
            if (!validateCreateShipment4Refund(refund)) {
                log.error("can not create shipment becacuse of not right refundtype ({}) or status({}) ", refund.getRefundType(), refund.getStatus());
                throw new JsonResponseException("refund.can.not.create.shipment.error.type.or.status");
            }
            List<RefundItem> refundChangeItems = null;
            if (Objects.equals(shipType,2)){
                refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
            }
            if (Objects.equals(shipType,3)){
                refundChangeItems = refundReadLogic.findRefundLostItems(refund);
            }
            if (refundReadLogic.checkRefundWaitHandleNumber(refundChangeItems)){
                throw new JsonResponseException("refund.wait.shipment.item.can.not.dupliacte");
            }
            OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);

            //检查库存是否充足
            checkStockIsEnough(warehouseId,skuCodeAndQuantity);
            //封装发货信息
            List<ShipmentItem> shipmentItems = makeChangeShipmentItems(refundChangeItems,skuCodeAndQuantity);
            //发货单商品金额
            Long shipmentItemFee=0L;
            //发货单总的优惠
            Long shipmentDiscountFee=0L;
            //发货单总的净价
            Long shipmentTotalFee=0L;
            //运费
            Long shipmentShipFee =0L;
            //运费优惠
            Long shipmentShipDiscountFee=0L;
            //运费优惠
            //订单总金额
            for (ShipmentItem shipmentItem : shipmentItems) {
                shipmentItemFee = shipmentItem.getSkuPrice()*shipmentItem.getQuantity() + shipmentItemFee;
                shipmentDiscountFee = (shipmentItem.getSkuDiscount()==null?0:shipmentItem.getSkuDiscount())+shipmentDiscountFee;
                shipmentTotalFee = shipmentItem.getCleanFee()+shipmentTotalFee;
            }
            //发货单中订单总金额
            Long shipmentTotalPrice=shipmentTotalFee+shipmentShipFee-shipmentShipDiscountFee;;
            Shipment shipment = makeShipment(orderRefund.getOrderId(),warehouseId,shipmentItemFee,
                    shipmentDiscountFee,shipmentTotalFee,shipmentShipFee,shipType,shipmentShipDiscountFee,shipmentTotalPrice,refund.getShopId());
            //换货
            shipment.setReceiverInfos(findRefundReceiverInfo(refundId));
            Map<String,String> extraMap = shipment.getExtra();
            extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO,JSON_MAPPER.toJson(shipmentItems));
            shipment.setExtra(extraMap);
            shipment.setShopId(refund.getShopId());
            shipment.setShopName(refund.getShopName());
            //锁定库存
            Response<Boolean> lockStockRlt = lockStock(shipment);
            if (!lockStockRlt.isSuccess()){
                log.error("this shipment can not unlock stock,shipment id is :{}",shipment.getId());
                throw new JsonResponseException("lock.stock.error");
            }
            //换货的发货关联的订单id 为换货单id
            Response<Long> createResp = middleShipmentWriteService.createForAfterSale(shipment, orderRefund.getOrderId(),refundId);
            if (!createResp.isSuccess()) {
                log.error("fail to create shipment:{} for refund(id={}),and level={},cause:{}",
                        shipment, refundId, OrderLevel.SHOP.getValue(), createResp.getError());
                throw new JsonResponseException(createResp.getError());
            }
            Long shipmentId = createResp.getResult();
            eventBus.post(new RefundShipmentEvent(shipmentId));
            shipmentIds.add(shipmentId);
        }
        return shipmentIds;
    }

    /**
     * 同步发货单到恒康
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/sync/hk",method = RequestMethod.PUT)
    @OperationLogType("同步发货单到恒康")
    public void syncHkShipment(@PathVariable(value = "id")@OperationLogParam Long shipmentId){
        OrderShipment orderShipment =  shipmentReadLogic.findOrderShipmentByShipmentId(shipmentId);
        if (Objects.equals(orderShipment.getType(),1)){
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            Map<String,String> extraMap = shopOrder.getExtra();
            String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
            String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
            if (!org.apache.commons.lang3.StringUtils.isEmpty(isStepOrder)&&Objects.equals(isStepOrder,"true")){
                if (!org.apache.commons.lang3.StringUtils.isEmpty(stepOrderStatus)&&Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(),Integer.valueOf(stepOrderStatus))){
                    throw new JsonResponseException("step.order.not.all.paid.can.not.sync.hk");
                }
            }
        }
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipment);
        if(!syncRes.isSuccess()){
            log.error("sync shipment(id:{}) to hk fail,error:{}",shipmentId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }



    /**
     * 取消发货单,这个时候没有同步到恒康
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/cancel",method = RequestMethod.PUT)
    @OperationLogType("取消发货单")
    public void cancleShipment(@PathVariable(value = "id")@OperationLogParam Long shipmentId){
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> cancelRes = shipmentWiteLogic.updateStatus(shipment, MiddleOrderEvent.CANCEL_SHIP.toOrderOperation());
        if(!cancelRes.isSuccess()){
            log.error("cancel shipment(id:{}) fail,error:{}",shipmentId,cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
        //解锁库存
        UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
        unLockStockEvent.setShipment(shipment);
        eventBus.post(unLockStockEvent);
    }

    /**
     * 同步发货单取消状态到恒康
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/cancel/sync/hk",method = RequestMethod.PUT)
    @OperationLogType("同步取消状态")
    public void syncHkCancelShipment(@PathVariable(value = "id") Long shipmentId){
        log.info("try to auto cancel shipment,shipment id is {},operationType is {}",shipmentId,0);
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncShipmentLogic.syncShipmentCancelToHk(shipment,0);
        if(!syncRes.isSuccess()){
            log.error("sync cancel shipment(id:{}) to hk fail,error:{}",shipmentId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }

        //通知恒康取消同步之后需要解锁库存
        UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
        unLockStockEvent.setShipment(shipment);
        eventBus.post(unLockStockEvent);
    }
    /**
     * 发货单确认收货失败通知恒康
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/done/sync/hk",method = RequestMethod.PUT)
    public void syncShipmentDoneToHk(@PathVariable(value="id") Long shipmentId){
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncShipmentLogic.syncShipmentDoneToHk(shipment,2,MiddleOrderEvent.HK_CONFIRME_FAILED.toOrderOperation());
        if(!syncRes.isSuccess()){
            log.error("sync  shipment(id:{}) done to hk fail,error:{}",shipmentId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }


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

    private String findRefundReceiverInfo(Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        MiddleChangeReceiveInfo receiveInfo = refundReadLogic.findMiddleChangeReceiveInfo(refund);
        try {
            return objectMapper.writeValueAsString(receiveInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }


    private List<ReceiverInfo> doFindReceiverInfos(Long orderId, OrderLevel orderLevel) {
        Response<List<ReceiverInfo>> receiversResp = receiverInfoReadService.findByOrderId(orderId, orderLevel);
        if (!receiversResp.isSuccess()) {
            log.error("fail to find receiver info by order id={},and order level={},cause:{}",
                    orderId, orderLevel.getValue(), receiversResp.getError());
            throw new JsonResponseException(receiversResp.getError());
        }
        return receiversResp.getResult();
    }





    //获取指定仓库中指定商品的库存信息
    private Map<String, Integer> findStocksForSkus(Long warehouseId,List<String> skuCodes){
        Response<Map<String, Integer>> r = warehouseSkuReadService.findByWarehouseIdAndSkuCodes(warehouseId, skuCodes);
        if(!r.isSuccess()){
            log.error("failed to find stock in warehouse(id={}) for skuCodes:{}, error code:{}",
                    warehouseId, skuCodes, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    //检查库存是否充足
    private void checkStockIsEnough(Long warehouseId, Map<String,Integer> skuCodeAndQuantityMap){


        List<String> skuCodes = Lists.newArrayListWithCapacity(skuCodeAndQuantityMap.size());
        skuCodes.addAll(skuCodeAndQuantityMap.keySet());
        Map<String, Integer> warehouseStockInfo = findStocksForSkus(warehouseId,skuCodes);
        for (String skuCode : warehouseStockInfo.keySet()){
            if(warehouseStockInfo.get(skuCode)<skuCodeAndQuantityMap.get(skuCode)){
                log.error("sku code:{} warehouse stock:{} ship applyQuantity:{} stock not enough",skuCode,warehouseStockInfo.get(skuCode),skuCodeAndQuantityMap.get(skuCode));
                throw new JsonResponseException(skuCode+".stock.not.enough");
            }
        }
    }

    //检查子单是否是全部发货
    private void checkSkuCodeAndQuantityLegal(Map<Long, Integer> skuOrderIdAndQuantity){
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        for (Long skuOrderId:skuOrderIds){
            SkuOrder skuOrder = (SkuOrder) orderReadLogic.findOrder(skuOrderId,OrderLevel.SKU);
            if (!Objects.equals(skuOrder.getQuantity(),skuOrderIdAndQuantity.get(skuOrderId))){
               log.error("sku order id:{} data quantity {} not equal sku quantity {}",skuOrderId,skuOrderIdAndQuantity.get(skuOrderId),skuOrder.getQuantity());
               throw new JsonResponseException("sku.order.create.shipment.must.be.full");
            }
        }
    }


    private Map<Long, Integer> analysisSkuOrderIdAndQuantity(String data){
        Map<Long, Integer> skuOrderIdAndQuantity = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, Long.class, Integer.class));
        if(skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuOrderIdAndQuantity:{}",data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }

    private Map<String, Integer> analysisSkuCodeAndQuantity(String data){
        Map<String, Integer> skuOrderIdAndQuantity = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, String.class, Integer.class));
        if(skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuCodeAndQuantity:{}",data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }





    private Warehouse findWarehouseById(Long warehouseId){
        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }

        return warehouseRes.getResult();
    }



    private Shipment makeShipment(Long shopOrderId,Long warehouseId, Long shipmentItemFee
            ,Long shipmentDiscountFee, Long shipmentTotalFee,Long shipmentShipFee,Integer shipType,
                                  Long shipmentShipDiscountFee,
                                          Long shipmentTotalPrice,Long shopId){
        Shipment shipment = new Shipment();
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue());
        shipment.setReceiverInfos(findReceiverInfos(shopOrderId, OrderLevel.SHOP));

        //发货仓库信息
        Warehouse warehouse = findWarehouseById(warehouseId);
        Map<String,String> extraMap = Maps.newHashMap();
        ShipmentExtra shipmentExtra = new ShipmentExtra();
        shipmentExtra.setWarehouseId(warehouse.getId());
        shipmentExtra.setWarehouseName(warehouse.getName());
        Map<String,String> warehouseExtra = warehouse.getExtra();
        if (Objects.nonNull(warehouseExtra)){
            shipmentExtra.setWarehouseOutCode(warehouseExtra.get("outCode")!=null?warehouseExtra.get("outCode"):"");
        }

        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
        String shopCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE,openShop);
        String shopName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME,openShop);
        shipmentExtra.setErpOrderShopCode(shopCode);
        shipmentExtra.setErpOrderShopName(shopName);

        shipmentExtra.setShipmentItemFee(shipmentItemFee);
        //发货单运费金额
        shipmentExtra.setShipmentShipFee(shipmentShipFee);
        //运费优惠
        shipmentExtra.setShipmentShipDiscountFee(shipmentShipDiscountFee);
        //发货单优惠金额
        shipmentExtra.setShipmentDiscountFee(shipmentDiscountFee);
        //发货单总的净价
        shipmentExtra.setShipmentTotalFee(shipmentTotalFee);
        //发货单的订单总金额
        shipmentExtra.setShipmentTotalPrice(shipmentTotalPrice);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);

        shipmentExtra.setErpPerformanceShopCode(shopCode);
        shipmentExtra.setErpPerformanceShopName(shopName);
        //物流编码
        Map<String,String> shopOrderMap = shopOrder.getExtra();
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())&&Objects.equals(shipType,ShipmentType.SALES_SHIP.value())){
            shipmentExtra.setVendCustID(TradeConstants.JD_VEND_CUST_ID);
        }else{
            String expressCode = shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE);
            if (!StringUtils.isEmpty(expressCode)){
                shipmentExtra.setVendCustID(expressCode);
            }else{
                shipmentExtra.setVendCustID(TradeConstants.OPTIONAL_VEND_CUST_ID);
            }
        }
        shipmentExtra.setOrderHkExpressCode(shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE));
        shipmentExtra.setOrderHkExpressName(shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_NAME));
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO,JSON_MAPPER.toJson(shipmentExtra));
        //店铺信息塞值
        shipment.setShopId(Long.valueOf(openShop.getId()));
        shipment.setShopName(openShop.getShopName());
        shipment.setExtra(extraMap);
        shipment.setType(shipType);
        return shipment;
    }


    private List<ShipmentItem> makeShipmentItems(List<SkuOrder> skuOrders, Map<Long,Integer> skuOrderIdAndQuantity){
        Map<Long,SkuOrder> skuOrderMap = skuOrders.stream().filter(Objects::nonNull).collect(Collectors.toMap(SkuOrder::getId,it -> it));
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithExpectedSize(skuOrderIdAndQuantity.size());
        for (Long skuOrderId : skuOrderIdAndQuantity.keySet()){
            ShipmentItem shipmentItem = new ShipmentItem();
            SkuOrder skuOrder = skuOrderMap.get(skuOrderId);
            shipmentItem.setQuantity(skuOrderIdAndQuantity.get(skuOrderId));
            //初始情况下,refundQuery为0
            if (skuOrder.getShipmentType()!=null&&Objects.equals(skuOrder.getShipmentType(),1)){
                shipmentItem.setIsGift(true);
            }else{
                shipmentItem.setIsGift(false);
            }
            shipmentItem.setRefundQuantity(0);
            shipmentItem.setSkuOrderId(skuOrderId);
            shipmentItem.setSkuName(skuOrder.getItemName());
            shipmentItem.setSkuOutId(skuOrder.getOutId());
            SkuOrder originSkuOrder = (SkuOrder) orderReadLogic.findOrder(skuOrder.getId(),OrderLevel.SKU);
            //原始价格
            shipmentItem.setSkuPrice(Integer.valueOf(Math.round(skuOrder.getOriginFee()/originSkuOrder.getQuantity())));
            //积分
            String originIntegral = "";
            try{
                originIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_INTEGRAL,skuOrder);
            }catch (JsonResponseException e){
                log.info("sku order(id:{}) extra map not contains key:{}",skuOrder.getId(),TradeConstants.SKU_INTEGRAL);
            }
            Integer integral = StringUtils.isEmpty(originIntegral)?0: Integer.valueOf(originIntegral);
            shipmentItem.setIntegral(this.getIntegral(integral,originSkuOrder.getQuantity(),skuOrderIdAndQuantity.get(skuOrderId)));
            //skuDisCount,根据生成发货单的数量与skuOrder的数量按照比例四舍五入计算金额
            Long disCount = skuOrder.getDiscount()+Long.valueOf(this.getShareDiscount(skuOrder));
            shipmentItem.setSkuDiscount(this.getDiscount(originSkuOrder.getQuantity(),skuOrderIdAndQuantity.get(skuOrderId), Math.toIntExact(disCount)));
            //总净价
            shipmentItem.setCleanFee(this.getCleanFee(shipmentItem.getSkuPrice(),shipmentItem.getSkuDiscount(),shipmentItem.getQuantity()));
            //商品净价
            shipmentItem.setCleanPrice(this.getCleanPrice(shipmentItem.getCleanFee(),shipmentItem.getQuantity()));

            shipmentItem.setOutSkuCode(skuOrder.getOutSkuId());
            shipmentItem.setSkuCode(skuOrder.getSkuCode());
            //商品id
            String outItemId="";
            try{
                outItemId =  orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.MIDDLE_OUT_ITEM_ID,skuOrder);
            }catch (Exception e){
                log.info("outItemmId is not exist");
            }
            shipmentItem.setItemId(outItemId);
            //商品属性
            shipmentItem.setAttrs(skuOrder.getSkuAttrs());

            shipmentItems.add(shipmentItem);

        }


        return shipmentItems;
    }

    private List<ShipmentItem> makeChangeShipmentItems(List<RefundItem> refundChangeItems,Map<String,Integer> skuCodeAndQuantity){
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithExpectedSize(skuCodeAndQuantity.size());

        Map<String,RefundItem> refundItemMap = refundChangeItems.stream().filter(Objects::nonNull).collect(Collectors.toMap(RefundItem::getSkuCode,it -> it));
        for (String skuCode : skuCodeAndQuantity.keySet()){
            ShipmentItem shipmentItem = new ShipmentItem();
            RefundItem refundItem = refundItemMap.get(skuCode);
            SkuOrder skuOrder = (SkuOrder) orderReadLogic.findOrder(refundItem.getSkuOrderId(),OrderLevel.SKU);
            shipmentItem.setQuantity(skuCodeAndQuantity.get(skuCode));
            //退货数量
            shipmentItem.setRefundQuantity(refundItem.getApplyQuantity());
            shipmentItem.setSkuName(refundItem.getSkuName());
            //商品单价
            shipmentItem.setSkuPrice(refundItem.getSkuPrice());
            //商品积分
            shipmentItem.setIntegral(0);
            shipmentItem.setSkuDiscount(refundItem.getSkuDiscount());
            shipmentItem.setCleanFee(refundItem.getCleanFee());
            shipmentItem.setCleanPrice(refundItem.getCleanPrice());
            shipmentItem.setSkuCode(refundItem.getSkuCode());
            shipmentItem.setOutSkuCode(refundItem.getOutSkuCode());
            //商品id
            shipmentItem.setItemId(refundItem.getItemId());
            //商品属性
            shipmentItem.setAttrs(refundItem.getAttrs());
            shipmentItems.add(shipmentItem);
        }


        return shipmentItems;
    }

    /**
     * 判断类型为换货的售后单是否可以创建发货单
     *
     * @param refund
     * @return
     */
    private boolean validateCreateShipment4Refund(Refund refund) {
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())
                && Objects.equals(refund.getStatus(), MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue())) {
            return true;
        }
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())
                && Objects.equals(refund.getStatus(), MiddleRefundStatus.LOST_WAIT_CREATE_SHIPMENT.getValue())) {
            return true;
        }
        return false;
    }

    /**
     * 根据发货单id查询公司规则
     * @param shipmentId 发货单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/shipment/{id}/warehouse/company/rule", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseCompanyRule findWarehouseCompanyRule(@PathVariable("id") Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Long warehouseId = shipmentExtra.getWarehouseId();
        Warehouse warehouse = findWarehouseById(warehouseId);
        Response<WarehouseCompanyRule> ruleRes = shipmentReadLogic.findCompanyRuleByWarehouseCode(warehouse.getCode());
        if (!ruleRes.isSuccess()) {
            log.error("find warehouse company rule by company code:{} fail,error:{}", warehouse.getCode(), ruleRes.getError());
            throw new JsonResponseException(ruleRes.getError());
        }

        return ruleRes.getResult();
    }

    private Response<Boolean> lockStock(Shipment shipment){
        //获取发货单下的sku订单信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);

        List<WarehouseShipment> warehouseShipmentList = Lists.newArrayList();
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        //组装sku订单数量信息
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.transform(shipmentItems, new Function<ShipmentItem, SkuCodeAndQuantity>() {
            @Nullable
            @Override
            public SkuCodeAndQuantity apply(@Nullable ShipmentItem shipmentItem) {
                SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                skuCodeAndQuantity.setSkuCode(shipmentItem.getSkuCode());
                skuCodeAndQuantity.setQuantity(shipmentItem.getQuantity());
                return skuCodeAndQuantity;
            }
        });
        warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
        warehouseShipment.setWarehouseId(extra.getWarehouseId());
        warehouseShipment.setWarehouseName(extra.getWarehouseName());
        warehouseShipmentList.add(warehouseShipment);
        Response<Boolean> result = warehouseSkuWriteService.lockStock(warehouseShipmentList);

        //触发库存推送
        List<String> skuCodes = Lists.newArrayList();
        for (WarehouseShipment ws : warehouseShipmentList) {
            for (SkuCodeAndQuantity skuCodeAndQuantity : ws.getSkuCodeAndQuantities()) {
                skuCodes.add(skuCodeAndQuantity.getSkuCode());
            }
        }
        stockPusher.submit(skuCodes);
        return result;

    }

    /**
     *
     * @param skuQuantity  sku订单中商品的数量
     * @param shipSkuQuantity 发货的sku商品的数量
     * @param skuDiscount sku订单中商品的折扣
     * @return 返回四舍五入的计算结果,得到发货单中的sku商品的折扣
     */
    private  Integer getDiscount(Integer skuQuantity,Integer shipSkuQuantity,Integer skuDiscount){
        return Math.round(skuDiscount*shipSkuQuantity/skuQuantity);
    }

    /**
     * 计算总净价
     * @param skuPrice 商品原价
     * @param discount 发货单中sku商品的折扣
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return
     */
    private Integer getCleanFee(Integer skuPrice,Integer discount,Integer shipSkuQuantity){

        return skuPrice*shipSkuQuantity-discount;
    }

    /**
     * 计算商品净价
     * @param cleanFee 商品总净价
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return
     */
    private Integer getCleanPrice(Integer cleanFee,Integer shipSkuQuantity){
        return Math.round(cleanFee/shipSkuQuantity);
    }

    /**
     * 计算积分
     * @param integral sku订单获取的积分
     * @param skuQuantity sku订单总的数量
     * @param shipmentSkuQuantity 发货单中该sku订单的数量
     * @return 获取发货单中sku订单的积分
     */
    private Integer getIntegral(Integer integral,Integer skuQuantity,Integer shipmentSkuQuantity){
        return Math.round(integral*shipmentSkuQuantity/skuQuantity);
    }

    private String getShareDiscount(SkuOrder skuOrder){
        String skuShareDiscount="";
        try{
            skuShareDiscount = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_SHARE_DISCOUNT,skuOrder);
        }catch (JsonResponseException e){
            log.info("sku order(id:{}) extra map not contains key:{}",skuOrder.getId(),TradeConstants.SKU_SHARE_DISCOUNT);
        }
        return org.apache.commons.lang3.StringUtils.isEmpty(skuShareDiscount)?"0":skuShareDiscount;
    }

    /**
     * 宝胜二期--单个发货单撤销功能
     * @param shipmentId
     */
    @RequestMapping(value = "api/single/shipment/{id}/rollback", method = RequestMethod.PUT)
    @OperationLogType("单个发货单取消")
    public void rollbackShopOrder(@PathVariable("id") @OperationLogParam Long shipmentId) {
        log.info("try to cancel shipemnt, shipmentId is {}",shipmentId);
        boolean isRollBackSuccess = shipmentWiteLogic.rollbackShipment(shipmentId);
        if (!isRollBackSuccess){
            throw new JsonResponseException("cancel.shipment.failed");
        }
    }

    /**
     * 根据电商店铺id获取发货单下所有的货品集合
     * @param id
     * @return
     */
    @RequestMapping(value = "api/order/{id}/shipment/items", method = RequestMethod.GET)
    public List<ShipmentItem> findShipmentItemByOrder(@PathVariable("id") Long id){
        //获取订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
        //获取订单下的所有发货单
        List<Shipment> originShipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
        List<Shipment> shipments = originShipments.stream().
                filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()))
                .collect(Collectors.toList());
        //获取订单下对应发货单的所有发货商品列表
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItemsForList(shipments);
        return shipmentItems;
    }
}
