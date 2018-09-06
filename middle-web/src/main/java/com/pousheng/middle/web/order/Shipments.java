package com.pousheng.middle.web.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.service.*;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.manager.WarehouseSkuStockManager;
import com.pousheng.middle.web.events.trade.UnLockStockEvent;
import com.pousheng.middle.web.events.warehouse.StockRecordEvent;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.erp.SyncErpShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import com.pousheng.middle.web.warehouses.component.WarehouseSkuStockLogic;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMeId;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.enums.ShipmentOccupyType;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private WarehouseClient warehouseClient;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @RpcConsumer
    private InventoryClient inventoryClient;
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
    private SyncErpShipmentLogic syncErpShipmentLogic;
    @Autowired
    private WarehouseSkuStockManager warehouseSkuStockManager;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private StockPusher stockPusher;
    @Autowired
    private PermissionUtil permissionUtil;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private PoushengSettlementPosReadService poushengSettlementPosReadService;
    @Autowired
    private PoushengSettlementPosWriteService poushengSettlementPosWriteService;
    @RpcConsumer
    private RefundReadService refundReadService;
    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private WarehouseSkuStockLogic warehouseSkuStockLogic;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private ShopReadService shopReadService;
    @Autowired
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private SkuOrderReadService skuOrderReadService;
    @Autowired
    private OrderShipmentWriteService orderShipmentWriteService;
    @Autowired
    private ShipmentWriteManger shipmentWriteManger;
    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;
    @Autowired
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private ShopCacher shopCacher;

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private MiddleRefundWriteService middleRefundWriteService;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    /**
     * 快递单号
     */
    private static final Integer MIN_LENGTH = 8;

    /**
     * 发货单分页 注意查的是 orderShipment
     *
     * @param shipmentCriteria 查询参数
     * @return 发货单分页信息
     */
    @RequestMapping(value = "/api/shipment/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ShipmentPagingInfo> findBy(OrderShipmentCriteria shipmentCriteria) {
        String shipmentCriteriaStr =JsonMapper.nonEmptyMapper().toJson(shipmentCriteria);
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-PAGING-START param: shipmentCriteria [{}]",shipmentCriteriaStr);
        }
        if (shipmentCriteria.getStatus() != null && MiddleShipmentsStatus.PART_SHIPPED.getValue() == shipmentCriteria.getStatus()) {
            shipmentCriteria.setStatus(MiddleShipmentsStatus.SHIPPED.getValue());
            shipmentCriteria.setPartShip(Boolean.TRUE);
        }
        if (shipmentCriteria.getEndAt() != null) {
            shipmentCriteria.setEndAt(new DateTime(shipmentCriteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        if (shipmentCriteria.getOrderEndAt() != null) {
            shipmentCriteria.setOrderEndAt(new DateTime(shipmentCriteria.getOrderEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        if (shipmentCriteria.getOutOrderId() != null) {
            Response<List<ShopOrder>> response = shopOrderReadService.findByOutId(shipmentCriteria.getOutOrderId());
            if (!response.isSuccess()) {
                throw new JsonResponseException(response.getError());
            }
            List<ShopOrder> list = response.getResult();
            if (list.isEmpty()) {
                Paging<ShipmentPagingInfo> paging = new Paging<>();
                return paging;
            }
            shipmentCriteria.setOrderIds(list.stream().map(ShopOrder::getId).collect(Collectors.toList()));
        }
        if (shipmentCriteria.getShipmentSerialNo() != null){
            if (shipmentCriteria.getShipmentSerialNo().length() < MIN_LENGTH) {
                throw new JsonResponseException("shipment.serial.no.too.short");
            }
            Response<List<Shipment>> response = shipmentReadService.findBySerialNo(shipmentCriteria.getShipmentSerialNo());
            if (!response.isSuccess()) {
                throw new JsonResponseException(response.getError());
            }
            List<Shipment> list = response.getResult();
            if (list.isEmpty()) {
                Paging<ShipmentPagingInfo> paging = new Paging<>();
                return paging;
            }
            shipmentCriteria.setShipmentIds(list.stream().map(Shipment::getId).collect(Collectors.toList()));
        }
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (shipmentCriteria.getShopId() == null) {
            shipmentCriteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(shipmentCriteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        //判断查询的发货单类型
        if (Objects.equals(shipmentCriteria.getType(), ShipmentType.EXCHANGE_SHIP.value())||Objects.equals(shipmentCriteria.getType(), 3)) {
            shipmentCriteria.setAfterSaleOrderCode(shipmentCriteria.getOrderCode());
            shipmentCriteria.setOrderCode(null);
        }
        //正常销售的占库发货单需要显示，售后的不显示
        shipmentCriteria.setIsOccupyShipment(ShipmentOccupyType.SALE_Y.name());
        Response<Paging<ShipmentPagingInfo>> response = orderShipmentReadService.findBy(shipmentCriteria);
        if (!response.isSuccess()) {
            log.error("find shipment by criteria:{} fail,error:{}", shipmentCriteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<ShipmentPagingInfo> shipmentPagingInfos = response.getResult().getData();
        Flow flow = orderFlowPicker.pickShipments();
        shipmentPagingInfos.forEach(shipmentPagingInfo -> {
            Shipment shipment = shipmentPagingInfo.getShipment();
            try {
                if (!StringUtils.isEmpty(shipment.getShipmentSerialNo())) {
                    shipmentPagingInfo.setShipmentSerialNos(Splitters.COMMA.splitToList(shipment.getShipmentSerialNo()));
                }
                shipmentPagingInfo.setOperations(flow.availableOperations(shipment.getStatus()));
                shipmentPagingInfo.setShipmentExtra(shipmentReadLogic.getShipmentExtra(shipment));
            } catch (JsonResponseException e) {
                log.error("complete paging info fail,error:{}", Throwables.getStackTraceAsString(e));
            } catch (Exception e) {
                log.error("complete paging info fail,cause:{}", Throwables.getStackTraceAsString(e));
            }
            if(shipmentPagingInfo.getOrderShipment().getStatus().equals(MiddleShipmentsStatus.SHIPPED.getValue())&&shipmentPagingInfo.getOrderShipment().getPartShip()){
                shipmentPagingInfo.getOrderShipment().setStatus(MiddleShipmentsStatus.PART_SHIPPED.getValue());
            }

        });
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-PAGING-END param: shipmentCriteria [{}] ,resp: [{}]",shipmentCriteriaStr,JsonMapper.nonEmptyMapper().toJson(response));
        }
        return response.getResult();
    }


    //发货单详情
    @RequestMapping(value = "/api/shipment/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ShipmentDetail findDetail(@PathVariable(value = "id") Long shipmentId) {
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-DETAIL-START param: shipmentId [{}]",shipmentId);
        }
        ShipmentDetail detail = shipmentReadLogic.orderDetail(shipmentId);
        Integer status=detail.getShipment().getStatus();
        if (!status.equals(MiddleShipmentsStatus.SHIPPED.getValue()) && !status.equals(MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                && !status.equals(MiddleShipmentsStatus.CONFIRMED_FAIL.getValue())) {
            for (ShipmentItem item : detail.getShipmentItems()) {
                if (item.getShipQuantity() == null) {
                    item.setShipQuantity(0);
                }
            }
        }
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-DETAIL-END param: shipmentId [{}] ,resp: [{}]",shipmentId,JsonMapper.nonEmptyMapper().toJson(detail));
        }
        return detail;
    }


    /**
     * 订单下的发货单
     *
     * @param shopOrderId 店铺订单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/order/{id}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Shipment> shipments(@PathVariable("id") Long shopOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-SHIPMENTS-START param: shopOrderId [{}]",shopOrderId);
        }
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment by shopOrderId ({}) failed,caused by ({})", shopOrderId, response.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-SHIPMENTS-END param: shopOrderId [{}] ,resp: [{}]",shopOrderId,JsonMapper.nonEmptyMapper().toJson(response.getResult()));
        }
        return response.getResult();
    }

    /**
     * 订单下的发货单
     *
     * @param orderCode 店铺订单code
     * @return 发货单
     */
    @RequestMapping(value = "/api/ps/order/{code}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Shipment> shipmentsByCode(@PathVariable("code") String orderCode) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderCodeAndOrderLevel(orderCode, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment by shopOrderCode ({}) failed,caused by ({})", orderCode, response.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        return response.getResult();
    }


    /**
     * 换货单下的发货单
     *
     * @param afterSaleOrderId 售后单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/refund/{id}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderShipment> changeShipments(@PathVariable("id") Long afterSaleOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-REFUND-SHIPMENTS-START param: afterSaleOrderId [{}]",afterSaleOrderId);
        }
        List<OrderShipment> shipments= shipmentReadLogic.findByAfterOrderIdAndType(afterSaleOrderId);
        if(log.isDebugEnabled()){
            log.debug("API-REFUND-SHIPMENTS-END param: afterSaleOrderId [{}] ,resp: [{}]",afterSaleOrderId,JsonMapper.nonEmptyMapper().toJson(shipments));
        }

        return shipments;
    }


    /**
     * 售后单下的发货单
     *
     * @param afterSaleOrderId 售后单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/after/sale/{id}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Shipment> afterSaleShipments(@PathVariable("id") Long afterSaleOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-AFTER-SALE-SHIPMENTS-START param: afterSaleOrderId [{}]",afterSaleOrderId);
        }
        OrderShipmentCriteria orderShipmentCriteria = new OrderShipmentCriteria();
        orderShipmentCriteria.setAfterSaleOrderId(afterSaleOrderId);
        Response<Paging<ShipmentPagingInfo>> pagingResponse = orderShipmentReadService.findBy(orderShipmentCriteria);
        if (!pagingResponse.isSuccess()){
            throw new  JsonResponseException("find.shipments.failed");
        }
        List<ShipmentPagingInfo> shipmentPagingInfos = pagingResponse.getResult().getData();
        List<Shipment> shipments = Lists.newArrayList();
        for (ShipmentPagingInfo shipmentPagingInfo:shipmentPagingInfos){
            Shipment shipment = shipmentPagingInfo.getShipment();
            if (Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())){
                continue;
            }
            shipments.add(shipment);
        }
        if(log.isDebugEnabled()){
            log.debug("API-AFTER-SALE-SHIPMENTS-END param: afterSaleOrderId [{}] ,resp: [{}]",afterSaleOrderId,JsonMapper.nonEmptyMapper().toJson(shipments));
        }
        return shipments;
    }

    /**
     * 售后单下的发货单
     *
     * @param afterSaleOrderCode 售后单code
     * @return 发货单
     */
    @RequestMapping(value = "/api/ps/after/sale/{code}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Shipment> afterSaleShipmentsByCode(@PathVariable("code") String afterSaleOrderCode) {
        OrderShipmentCriteria orderShipmentCriteria = new OrderShipmentCriteria();
        orderShipmentCriteria.setAfterSaleOrderCode(afterSaleOrderCode);
        Response<Paging<ShipmentPagingInfo>> pagingResponse = orderShipmentReadService.findBy(orderShipmentCriteria);
        if (!pagingResponse.isSuccess()){
            throw new  JsonResponseException("find.shipments.failed");
        }
        List<ShipmentPagingInfo> shipmentPagingInfos = pagingResponse.getResult().getData();
        List<Shipment> shipments = Lists.newArrayList();
        for (ShipmentPagingInfo shipmentPagingInfo:shipmentPagingInfos){
            Shipment shipment = shipmentPagingInfo.getShipment();
            if (Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())){
                continue;
            }
            shipments.add(shipment);
        }
        return shipments;
    }


    //获取发货单商品明细
    @RequestMapping(value = "/api/shipment/{id}/items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShipmentItem> shipmentItems(@PathVariable("id") String shipmentCode) {

        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
        return shipmentReadLogic.getShipmentItems(shipment);

    }


    /**
     * 判断发货单是否有效
     * 1、是否属于当前订单
     * 2、发货单状态是否为已发货
     *
     * @param shipmentCode 发货单主键id
     * @param orderCode    订单或售后单code
     * @param type    1 订单 2售后单
     * @return 是否有效
     */
    @RequestMapping(value = "/api/shipment/{id}/check/exist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> checkExist(@PathVariable("id") String shipmentCode, @RequestParam String orderCode, @RequestParam Integer type) {

        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-CHECK-EXIST-START param: shipmentCode [{}] orderCode [{}] type [{}]",shipmentCode,orderCode,type);
        }
        try {
            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            //是否属于当前订单
            if(Objects.equals(type,1)){
                if (!Objects.equals(orderShipment.getOrderCode(), orderCode)) {
                    log.error("shipment(id:{}) order code:{} not equal :{}", shipment.getId(), orderShipment.getOrderId(), orderCode);
                    return Response.fail("shipment.not.belong.to.order");
                }
            }else {
                if (!Objects.equals(orderShipment.getAfterSaleOrderCode(), orderCode)) {
                    log.error("shipment(id:{}) after sale order code:{} not equal :{}", shipment.getId(), orderShipment.getAfterSaleOrderId(), orderCode);
                    return Response.fail("shipment.not.belong.to.refund.order");
                }
            }

            //发货单状态是否为恒康已经确认收货
            if(!Objects.equals(orderShipment.getStatus(),MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                    && !Objects.equals(orderShipment.getStatus(),MiddleShipmentsStatus.SHIPPED.getValue())
                    && !Objects.equals(orderShipment.getStatus(),MiddleShipmentsStatus.CONFIRMED_FAIL.getValue())){
                log.error("shipment(id:{}) current status:{} can not apply after sale",shipmentCode,orderShipment.getStatus());
                return Response.fail("shipment.current.status.not.allow.apply.after.sale");
            }
        } catch (JsonResponseException e) {
            log.error("check  shipment(id:{}) is exist fail,error:{}", shipmentCode, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-CHECK-EXIST-END param: shipmentCode [{}] orderCode [{}] type [{}]",shipmentCode,orderCode,type);
        }
        return Response.ok(Boolean.TRUE);

    }

    /**
     * 自动创建发货单
     *
     * @param shopOrderId
     */
    @RequestMapping(value = "api/shipment/{id}/auto/create/shipment", method = RequestMethod.PUT)
    public void autoCreateSaleShipment(@PathVariable("id") Long shopOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-AUTO-CREATE-START param: shopOrderId [{}]",shopOrderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        shipmentWiteLogic.doAutoCreateShipment(shopOrder);
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-AUTO-CREATE-END param: shopOrderId [{}]",shopOrderId);
        }
    }

    /**
     *
     * 生成销售发货单
     * 发货成功：
     * 1. 更新子单的处理数量
     * 2. 更新子单的状态（如果子单全部为已处理则更新店铺订单为已处理）
     *
     * @param shopOrderId 店铺订单id
     * @return 发货单id集合
     */
    @RequestMapping(value = "/api/order/{id}/ship", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "生成销售发货单")
    public List<Long> createSalesShipment(@PathVariable("id") Long shopOrderId,
                                          @RequestParam(value = "dataList") String dataList) {
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-SHIP-START param: shopOrderId [{}] dataList [{}]",shopOrderId,dataList);
        }
        List<ShipmentRequest> requestDataList = JsonMapper.nonEmptyMapper().fromJson(dataList, JsonMapper.nonEmptyMapper().createCollectionType(List.class, ShipmentRequest.class));
        List<Long> warehouseIds = requestDataList.stream().filter(Objects::nonNull).map(ShipmentRequest::getWarehouseId).collect(Collectors.toList());
        //创建订单不能选择店仓
        this.validateIsCreateOrderImportOrder(warehouseIds,shopOrderId);
        //生成新的发货单之后需要释放之前占用的库存
        orderWriteLogic.releaseRejectShipmentOccupyStock(shopOrderId);

        List<Long> shipmentIds = Lists.newArrayList();
        //用于判断运费是否已经算过
        int shipmentFeeCount = 0;
        for (ShipmentRequest shipmentRequest : requestDataList) {
            String data = JsonMapper.nonEmptyMapper().toJson(shipmentRequest.getData());
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
            Long warehouseId = shipmentRequest.getWarehouseId();
            Map<Long, Integer> skuOrderIdAndQuantity = analysisSkuOrderIdAndQuantity(data);
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopOrder.getShopId());
            /*String erpType = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.ERP_SYNC_TYPE, openShop);
            if (StringUtils.isEmpty(erpType) || Objects.equals(erpType, "hk")) {
                if (!orderReadLogic.validateCompanyCode(warehouseId, shopOrder.getShopId())) {
                    throw new JsonResponseException("warehouse.must.be.in.one.company");
                }
            }*/
            //判断是否可以生成发货单
            for (Long skuOrderId : skuOrderIdAndQuantity.keySet()) {
                SkuOrder skuOrder = (SkuOrder) orderReadLogic.findOrder(skuOrderId, OrderLevel.SKU);
                Map<String, String> skuOrderExtraMap = skuOrder.getExtra();
                Integer waitHandleNumber = Integer.valueOf(skuOrderExtraMap.get(TradeConstants.WAIT_HANDLE_NUMBER));
                if (waitHandleNumber <= 0) {
                    log.error("sku order(id:{}) extra wait handle number:{} ,not enough to ship", skuOrder.getId(), waitHandleNumber);
                    throw new ServiceException("wait.handle.number.is.zero.can.not.create.shipment");
                }
            }
            //获取子单商品
            List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
            skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
            List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
            Map<String, Integer> skuCodeAndQuantityMap = skuOrders.stream().filter(Objects::nonNull)
                    .collect(Collectors.toMap(SkuOrder::getSkuCode, it -> skuOrderIdAndQuantity.get(it.getId())));
            //检查库存是否充足
            checkStockIsEnough(warehouseId, skuCodeAndQuantityMap, shopOrder.getShopId());
            //检查商品是不是一次发货还是拆成数次发货,如果不是一次发货抛出异常
            checkSkuCodeAndQuantityLegal(skuOrderIdAndQuantity);
            //封装发货信息
            List<ShipmentItem> shipmentItems = makeShipmentItems(skuOrders, skuOrderIdAndQuantity);
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
            if (!shipmentReadLogic.isShipmentFeeCalculated(shopOrderId) && shipmentFeeCount == 0) {
                shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee() == null ? 0 : shopOrder.getOriginShipFee());
                shipmentShipDiscountFee = shipmentShipFee - Long.valueOf(shopOrder.getShipFee() == null ? 0 : shopOrder.getShipFee());
                shipmentFeeCount++;
            }
            for (ShipmentItem shipmentItem : shipmentItems) {
                shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
                shipmentDiscountFee = (shipmentItem.getSkuDiscount() == null ? 0 : shipmentItem.getSkuDiscount()) + shipmentDiscountFee;
                shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
            }
            //订单总金额(运费优惠已经包含在子单折扣中)=商品总净价+运费
            Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;

            Shipment shipment = makeShipment(shopOrderId, warehouseId, shipmentItemFee, shipmentDiscountFee, shipmentTotalFee
                    , shipmentShipFee, ShipmentType.SALES_SHIP.value(), shipmentShipDiscountFee, shipmentTotalPrice, shopOrder.getShopId());
            shipment.setSkuInfos(skuOrderIdAndQuantity);
            Map<String, String> extraMap = shipment.getExtra();
            extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(shipmentItems));
            extraMap.put(TradeConstants.SHOP_ORDER_ID, String.valueOf(shopOrderId));
            shipment.setExtra(extraMap);
            shipment.setShopId(shopOrder.getShopId());
            shipment.setShopName(shopOrder.getShopName());
           /* //锁定库存
            Response<Boolean> lockStockRlt = mposSkuStockLogic.lockStock(shipment);
            if (!lockStockRlt.isSuccess()) {
                log.error("this shipment can not unlock stock,shipment id is :{}", shipment.getId());
                throw new JsonResponseException("lock.stock.error");
            }*/
            //创建发货单
            Long shipmentId;
            try {
                shipmentId = shipmentWriteManger.createShipmentByConcurrent(shipment, shopOrder, Boolean.TRUE);

                // 异步订阅 用于记录库存数量的日志
                eventBus.post(new StockRecordEvent(shipmentId, StockRecordType.MIDDLE_CREATE_SHIPMENT.toString()));
            } catch (ServiceException e){
                log.error("failed to create shipment shopOrderId : {} error:{}:", shopOrderId, e.getMessage());
                continue;
            }catch (Exception e) {
                log.error("failed to create shipment shopOrderId : {} cause:{}:", shopOrderId, Throwables.getStackTraceAsString(e));
                continue;
            }
            shipmentIds.add(shipmentId);
            Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
            if (!shipmentRes.isSuccess()) {
                log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
                continue;
            }
            //生成发货单之后需要将发货单id添加到子单中
            for (SkuOrder skuOrder : skuOrders) {
                try {
                    Map<String, String> skuOrderExtra = skuOrder.getExtra();
                    skuOrderExtra.put(TradeConstants.SKU_ORDER_SHIPMENT_CODE, TradeConstants.SHIPMENT_PREFIX + shipmentId);
                    Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(), OrderLevel.SKU, skuOrderExtra);
                    if (!response.isSuccess()) {
                        log.error("update sku order：{} extra map to:{} fail,error:{}", skuOrder.getId(), skuOrderExtra, response.getError());
                    }
                } catch (Exception e) {
                    log.error("update sku shipment id failed,skuOrder id is {},shipmentId is {},caused by {}", skuOrder.getId(), shipmentId, Throwables.getStackTraceAsString(e));
                }
            }
            try {
                orderWriteLogic.updateSkuHandleNumber(shipmentRes.getResult().getSkuInfos());
            } catch (ServiceException e) {
                log.error("shipment id is {} update sku handle number failed.caused by {}", shipmentId, Throwables.getStackTraceAsString(e));
            }
            //销售发货单需要判断预售商品是否已经支付完尾款
            Map<String, String> shopOrderExtra = shopOrder.getExtra();
            String isStepOrder = shopOrderExtra.get(TradeConstants.IS_STEP_ORDER);
            String stepOrderStatus = shopOrderExtra.get(TradeConstants.STEP_ORDER_STATUS);
            if (!org.apache.commons.lang3.StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
                if (!org.apache.commons.lang3.StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                    continue;
                }
            }

            //手动生成销售发货单可以支持同步到店铺
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if (Objects.equals(shipmentExtra.getShipmentWay(),TradeConstants.MPOS_SHOP_DELIVER)){
                log.info("sync shipment to mpos,shipmentId is {}",shipment.getId());
                shipmentWiteLogic.handleSyncShipment(shipment,2,shopOrder);;
            }else{
                Response<Boolean> syncRes = syncErpShipmentLogic.syncShipment(shipmentRes.getResult());
                if (!syncRes.isSuccess()) {
                    log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
                }
            }
        }
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-SHIP-END param: shopOrderId [{}] dataList [{}] resp: [{}]",shopOrderId,dataList,shipmentIds);
        }
            return shipmentIds;

    }


    /**
     *
     * 生成换货发货单
     * 发货成功：
     * 1. 更新子单的处理数量
     * 2. 更新子单的状态（如果子单全部为已处理则更新店铺订单为已处理）
     *
     * @param refundId 换货单id
     * @param dataList skuCode-quantity 以及仓库id的集合
     * @param shipType 2.换货，3.丢件补发
     * @return 发货单id
     */
    @RequestMapping(value = "/api/refund/{id}/ship", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "生成换货发货单")
    public List<Long> createAfterShipment(@PathVariable("id") Long refundId,
                                          @RequestParam(value = "dataList") String dataList,
                                          @RequestParam(required = false, defaultValue = "2") Integer shipType) {

        log.info("Shipments.createAfterShipment start refundId:{}, dataList:{}, shipType:{}",
                refundId,dataList,shipType);
        List<ShipmentRequest> requestDataList = JsonMapper.nonEmptyMapper()
                .fromJson(dataList, JsonMapper.nonEmptyMapper()
                .createCollectionType(List.class, ShipmentRequest.class));
        //判断是否是全渠道订单的售后单，如果不是全渠道订单的售后单，不能选择店仓
        //this.validateRefundAllChannelWarehouse(warehouseIds,refundId);
        //售后单生成换货发货单时加锁
        Response<Boolean> startResponse = middleRefundWriteService
                .updateTradeNo(refundId,TradeConstants.AFTER_SALE_EXHCANGE_UN_LOCK,
                        TradeConstants.AFTER_SALE_EXHCANGE_LOCK);
        if (!startResponse.isSuccess()){
            log.info("lock refunds failed,refundId {},caused by {}",refundId,startResponse.getError());
            throw new JsonResponseException(startResponse.getError());
        }
        //生成换货发货单首先需要把该售后单下拒单占用的库存全部释放
        refundWriteLogic.releaseRejectShipmentOccupyStock(refundId);

        List<Long> shipmentIds = Lists.newArrayList();
        for (ShipmentRequest shipmentRequest : requestDataList) {
            try {
                String data = JsonMapper.nonEmptyMapper().toJson(shipmentRequest.getData());
                Long warehouseId = shipmentRequest.getWarehouseId();
                Map<String, Integer> skuCodeAndQuantity = analysisSkuCodeAndQuantity(data);
                Refund refund = refundReadLogic.findRefundById(refundId);
                OpenShop openShop = orderReadLogic.findOpenShopByShopId(refund.getShopId());
                String erpType = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.ERP_SYNC_TYPE, openShop);
                if (StringUtils.isEmpty(erpType) || Objects.equals(erpType, "hk")) {
                    if (!orderReadLogic.validateCompanyCode(warehouseId, refund.getShopId())) {
                        throw new JsonResponseException("warehouse.must.be.in.one.company");
                    }
                }
                //只有售后类型是换货的,并且处理状态为待发货的售后单才能创建发货单
                if (!validateCreateShipment4Refund(refund)) {
                    log.error("can not create shipment becacuse of not right refundtype ({}) or status({}) ",
                            refund.getRefundType(), refund.getStatus());
                    throw new JsonResponseException("refund.can.not.create.shipment.error.type.or.status");
                }
                List<RefundItem> refundChangeItems = null;
                if (Objects.equals(shipType, ShipmentType.EXCHANGE_SHIP.value())) {
                    refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
                }
                if (Objects.equals(shipType, MiddleShipmentType.LOST_SHIP.getValue())) {
                    refundChangeItems = refundReadLogic.findRefundLostItems(refund);
                }
                if (!refundReadLogic.checkRefundWaitHandleNumber(refundChangeItems,skuCodeAndQuantity)) {
                    throw new JsonResponseException("refund.wait.shipment.item.can.not.dupliacte");
                }
                OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);

                //检查库存是否充足
                checkStockIsEnough(warehouseId, skuCodeAndQuantity, openShop.getId());
                //封装发货信息
                List<ShipmentItem> shipmentItems = makeChangeShipmentItems(refundChangeItems, skuCodeAndQuantity);
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
                //运费优惠
                //订单总金额
                for (ShipmentItem shipmentItem : shipmentItems) {
                    shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
                    shipmentDiscountFee = (shipmentItem.getSkuDiscount() == null ? 0 : shipmentItem.getSkuDiscount())
                            + shipmentDiscountFee;
                    shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
                }
                //发货单中订单总金额
                Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;
                ;
                Shipment shipment = makeShipment(orderRefund.getOrderId(), warehouseId, shipmentItemFee,
                        shipmentDiscountFee, shipmentTotalFee, shipmentShipFee, shipType,
                        shipmentShipDiscountFee, shipmentTotalPrice, refund.getShopId());
                //换货
                shipment.setReceiverInfos(findRefundReceiverInfo(refundId));
                Map<String, String> extraMap = shipment.getExtra();
                extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(shipmentItems));
                shipment.setExtra(extraMap);
                shipment.setShopId(refund.getShopId());
                shipment.setShopName(refund.getShopName());
                //换货的发货关联的订单id 为换货单id
                Long shipmentId = shipmentWriteManger.createForAfterSale(shipment, orderRefund, refundId);
                if (!refundWriteLogic.refundShipment(shipmentId)){
                    throw new JsonResponseException("update.refund.error");
                }
                //同步订单派发中心或者同步mpos
                shipmentWiteLogic.syncExchangeShipment(shipmentId);
                shipmentIds.add(shipmentId);

            }catch (JsonResponseException | ServiceException e){
                log.error("handle shipmentRequest:{} fail,cause;{}", shipmentRequest, Throwables.getStackTraceAsString(e));
                unLock(refundId);
                throw new JsonResponseException(e.getMessage());
            } catch (Exception e){
                log.error("handle shipmentRequest:{} fail,cause;{}", shipmentRequest, Throwables.getStackTraceAsString(e));
                unLock(refundId);
                throw new JsonResponseException("生成换货发货单失败");
            }
        }
        unLock(refundId);
        return shipmentIds;
    }

    private void unLock(Long refundId) {
        //生成发货单之后解锁
        Response<Boolean> endResponse = middleRefundWriteService
                .updateTradeNo(refundId, TradeConstants.AFTER_SALE_EXHCANGE_LOCK,
                        TradeConstants.AFTER_SALE_EXHCANGE_UN_LOCK);
        if (!endResponse.isSuccess()) {
            log.error("unlock refunds failed,refundId {},caused by {}", refundId, endResponse.getError());
        }
    }

    /**
     * 同步发货单到恒康
     * 同步发货单到erp
     *
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/sync/hk", method = RequestMethod.PUT)
    public void syncHkShipment(@PathVariable(value = "id") Long shipmentId) {
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipmentId);
        if (Objects.equals(orderShipment.getType(), 1)) {
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            //如果是占库发货单同步订单派发中心，则订单未处理原因修改为已经处理
            if (Objects.equals(orderShipment.getIsOccupyShipment(),ShipmentOccupyType.SALE_Y.toString())){
                shipmentWiteLogic.updateShipmentNote(shopOrder,OrderWaitHandleType.HANDLE_DONE.value());
                //占库发货单变为非占库发货单
                shipmentWiteLogic.updateOccupyShipmentTypeByShipmentId(orderShipment.getShipmentId(),ShipmentOccupyType.CHANGE_N.name());
            }
            Map<String, String> extraMap = shopOrder.getExtra();
            String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
            String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
            if (!org.apache.commons.lang3.StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
                if (!org.apache.commons.lang3.StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                    throw new JsonResponseException("step.order.not.all.paid.can.not.sync.hk");
                }
            }
        }
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        //发货仓库是mpos仓且是店仓则同步到门店
        if (Objects.equals(shipmentExtra.getShipmentWay(),TradeConstants.MPOS_SHOP_DELIVER)){
            log.info("sync shipment to mpos,shipmentId is {}",shipment.getId());
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            shipmentWiteLogic.handleSyncShipment(shipment,2,shopOrder);;
        }else{
            log.info("sync shipment to hk,shipmentId is {}",shipment.getId());
            Response<Boolean> syncRes = syncErpShipmentLogic.syncShipment(shipment);
            if (!syncRes.isSuccess()) {
                log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
                throw new JsonResponseException(syncRes.getError());
            }
        }

    }


    /**
     * 取消发货单,这个时候没有同步到恒康
     *
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/cancel", method = RequestMethod.PUT)
    public void cancleShipment(@PathVariable(value = "id") Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> cancelRes = shipmentWiteLogic.updateStatusLocking(shipment, MiddleOrderEvent.CANCEL_SHIP.toOrderOperation());
        if (!cancelRes.isSuccess()) {
            log.error("cancel shipment(id:{}) fail,error:{}", shipmentId, cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);

        Response<Boolean> unlockRlt =  mposSkuStockLogic.unLockStock(shipment);
        if (!unlockRlt.isSuccess()){
            log.error("this shipment can not unlock stock,shipment id is :{},warehouse id is:{}",shipment.getId(),extra.getWarehouseId());
        }
        //解锁库存
        // UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
        // unLockStockEvent.setShipment(shipment);
        // eventBus.post(unLockStockEvent);
    }

    /**
     * 同步发货单取消状态到恒康
     *
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/cancel/sync/hk", method = RequestMethod.PUT)
    public void syncHkCancelShipment(@PathVariable(value = "id") Long shipmentId) {
        log.info("try to auto cancel shipment,shipment id is {}", shipmentId);
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncErpShipmentLogic.syncShipmentCancel(shipment);
        if (!syncRes.isSuccess()) {
            log.error("sync cancel shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }

        //通知恒康取消同步之后需要解锁库存
        UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
        unLockStockEvent.setShipment(shipment);
        eventBus.post(unLockStockEvent);
    }

    /**
     * 发货单确认收货失败通知恒康
     *
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/done/sync/hk", method = RequestMethod.PUT)
    public void syncShipmentDoneToHk(@PathVariable(value = "id") Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncErpShipmentLogic.syncShipmentDone(shipment, 2, MiddleOrderEvent.HK_CONFIRME_FAILED.toOrderOperation());
        if (!syncRes.isSuccess()) {
            log.error("sync  shipment(id:{}) done to hk fail,error:{}", shipmentId, syncRes.getError());
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

    private String findRefundReceiverInfo(Long refundId) {
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
    private Map<String, Integer> findStocksForSkus(Long warehouseId, List<String> skuCodes, Long shopId) {
        Response<Map<String, Integer>> r = warehouseSkuStockLogic.findByWarehouseIdAndSkuCodes(warehouseId, skuCodes, shopId);
        if (!r.isSuccess()) {
            log.error("failed to find stock in warehouse(id={}) for skuCodes:{}, error code:{}",
                    warehouseId, skuCodes, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    //检查库存是否充足
    private void checkStockIsEnough(Long warehouseId, Map<String, Integer> skuCodeAndQuantityMap, Long shopId) {


        List<String> skuCodes = Lists.newArrayListWithCapacity(skuCodeAndQuantityMap.size());
        skuCodes.addAll(skuCodeAndQuantityMap.keySet());
        Map<String, Integer> warehouseStockInfo = findStocksForSkus(warehouseId, skuCodes, shopId);
        for (String skuCode : warehouseStockInfo.keySet()) {
            if (warehouseStockInfo.get(skuCode) < skuCodeAndQuantityMap.get(skuCode)) {
                log.error("sku code:{} warehouse stock:{} ship applyQuantity:{} stock not enough",
                        skuCode, warehouseStockInfo.get(skuCode), skuCodeAndQuantityMap.get(skuCode));
                throw new JsonResponseException(skuCode + ".stock.not.enough");
            }
        }
    }

    //检查子单是否是全部发货
    private void checkSkuCodeAndQuantityLegal(Map<Long, Integer> skuOrderIdAndQuantity) {
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        for (Long skuOrderId : skuOrderIds) {
            SkuOrder skuOrder = (SkuOrder) orderReadLogic.findOrder(skuOrderId, OrderLevel.SKU);
            if (!Objects.equals(skuOrder.getQuantity(), skuOrderIdAndQuantity.get(skuOrderId))) {
                log.error("sku order id:{} data quantity {} not equal sku quantity {}", skuOrderId, skuOrderIdAndQuantity.get(skuOrderId), skuOrder.getQuantity());
                throw new JsonResponseException("sku.order.create.shipment.must.be.full");
            }
        }
    }


    private Map<Long, Integer> analysisSkuOrderIdAndQuantity(String data) {
        Map<Long, Integer> skuOrderIdAndQuantity = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, Long.class, Integer.class));
        if (skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuOrderIdAndQuantity:{}", data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }

    private Map<String, Integer> analysisSkuCodeAndQuantity(String data) {
        Map<String, Integer> skuOrderIdAndQuantity = JSON_MAPPER.fromJson(data,
                JSON_MAPPER.createCollectionType(HashMap.class, String.class, Integer.class));
        if (skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuCodeAndQuantity:{}", data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }


    private WarehouseDTO findWarehouseById(Long warehouseId) {
        Response<WarehouseDTO> warehouseRes = warehouseClient.findById(warehouseId);
        if (!warehouseRes.isSuccess()) {
            log.error("find warehouse by id:{} fail,error:{}", warehouseId, warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }

        return warehouseRes.getResult();
    }


    private Shipment makeShipment(Long shopOrderId, Long warehouseId, Long shipmentItemFee
            , Long shipmentDiscountFee, Long shipmentTotalFee, Long shipmentShipFee, Integer shipType,
                                  Long shipmentShipDiscountFee,
                                  Long shipmentTotalPrice, Long shopId) {
        Shipment shipment = new Shipment();
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue());
        shipment.setReceiverInfos(findReceiverInfos(shopOrderId, OrderLevel.SHOP));

        //发货仓库信息
        WarehouseDTO warehouse = findWarehouseById(warehouseId);
        Map<String, String> extraMap = Maps.newHashMap();
        ShipmentExtra shipmentExtra = new ShipmentExtra();
        //仓库区分是店仓还是总仓
        if (Objects.equals(warehouse.getWarehouseSubType(), WarehouseType.TOTAL_WAREHOUSE.value())){
            shipment.setShipWay(Integer.valueOf(TradeConstants.MPOS_WAREHOUSE_DELIVER));
            shipment.setShipId(warehouse.getId());
            shipmentExtra.setShipmentWay(TradeConstants.MPOS_WAREHOUSE_DELIVER);
            shipmentExtra.setWarehouseId(warehouse.getId());
        }else {
            shipment.setShipWay(Integer.valueOf(TradeConstants.MPOS_SHOP_DELIVER));
            shipmentExtra.setShipmentWay(TradeConstants.MPOS_SHOP_DELIVER);
            if(StringUtils.isEmpty(warehouse.getOutCode())){
                log.error("warehouse(id:{}) out code invalid",warehouse.getId());
                throw new ServiceException("warehouse.out.code.invalid");
            }
            Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(), Long.valueOf(warehouse.getCompanyId()));
            shipmentExtra.setWarehouseId(shop.getId());
            shipment.setShipId(getShipIdByDeliverId(shop.getId()));
        }

        shipmentExtra.setWarehouseName(warehouse.getWarehouseName());

        shipmentExtra.setWarehouseOutCode(!StringUtils.isEmpty(warehouse.getOutCode()) ? warehouse.getOutCode() : "");

        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
        String shopCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE, openShop);
        String shopName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME, openShop);
        String shopOutCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE, openShop);
        //设置绩效店铺
        shipmentWiteLogic.defaultPerformanceShop(openShop, shopCode, shopName, shopOutCode);
        shipmentExtra.setErpOrderShopCode(shopCode);
        shipmentExtra.setErpOrderShopName(shopName);
        shipmentExtra.setErpOrderShopOutCode(shopOutCode);
        shipmentExtra.setErpPerformanceShopOutCode(shopOutCode);


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
        shipmentExtra.setIsStepOrder(shopOrder.getExtra().get(TradeConstants.IS_STEP_ORDER));
        shipmentExtra.setErpPerformanceShopCode(shopCode);
        shipmentExtra.setErpPerformanceShopName(shopName);
        //物流编码
        Map<String, String> shopOrderMap = shopOrder.getExtra();
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue()) && Objects.equals(shipType, ShipmentType.SALES_SHIP.value())) {
            shipmentExtra.setVendCustID(TradeConstants.JD_VEND_CUST_ID);
        } else {
            String expressCode = shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE);
            if (!StringUtils.isEmpty(expressCode)) {
                shipmentExtra.setVendCustID(expressCode);
            } else {
                shipmentExtra.setVendCustID(TradeConstants.OPTIONAL_VEND_CUST_ID);
            }
        }
        shipmentExtra.setOrderHkExpressCode(shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE));
        shipmentExtra.setOrderHkExpressName(shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_NAME));
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
        //店铺信息塞值
        shipment.setShopId(Long.valueOf(openShop.getId()));
        shipment.setShopName(openShop.getShopName());
        shipment.setExtra(extraMap);
        shipment.setType(shipType);
        return shipment;
    }


    private List<ShipmentItem> makeShipmentItems(List<SkuOrder> skuOrders, Map<Long, Integer> skuOrderIdAndQuantity) {
        Map<Long, SkuOrder> skuOrderMap = skuOrders.stream().filter(Objects::nonNull).collect(Collectors.toMap(SkuOrder::getId, it -> it));
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithExpectedSize(skuOrderIdAndQuantity.size());
        for (Long skuOrderId : skuOrderIdAndQuantity.keySet()) {
            ShipmentItem shipmentItem = new ShipmentItem();
            SkuOrder skuOrder = skuOrderMap.get(skuOrderId);
            shipmentItem.setQuantity(skuOrderIdAndQuantity.get(skuOrderId));
            //初始情况下,refundQuery为0
            if (skuOrder.getShipmentType() != null && Objects.equals(skuOrder.getShipmentType(), 1)) {
                shipmentItem.setIsGift(true);
            } else {
                shipmentItem.setIsGift(false);
            }
            shipmentItem.setRefundQuantity(0);
            shipmentItem.setSkuOrderId(skuOrderId);
            shipmentItem.setSkuName(skuOrder.getItemName());
            shipmentItem.setSkuOutId(skuOrder.getOutId());
            SkuOrder originSkuOrder = (SkuOrder) orderReadLogic.findOrder(skuOrder.getId(), OrderLevel.SKU);
            //原始价格
            shipmentItem.setSkuPrice(Integer.valueOf(Math.round(skuOrder.getOriginFee() / originSkuOrder.getQuantity())));
            //目前是子单整单发货，所以不需要分摊平台优惠金额
            Map<String,String> skuExtra = skuOrder.getExtra();
            String skuPlatformDiscount = skuExtra.get(TradeConstants.PLATFORM_DISCOUNT_FOR_SKU);
            shipmentItem.setSharePlatformDiscount(org.apache.commons.lang3.StringUtils.isNotEmpty(skuPlatformDiscount)?Integer.valueOf(skuPlatformDiscount):0);
            //积分
            String originIntegral = "";
            try {
                originIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_INTEGRAL, skuOrder);
            } catch (JsonResponseException e) {
                log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_INTEGRAL);
            }
            Integer integral = StringUtils.isEmpty(originIntegral) ? 0 : Integer.valueOf(originIntegral);
            shipmentItem.setIntegral(this.getIntegral(integral, originSkuOrder.getQuantity(), skuOrderIdAndQuantity.get(skuOrderId)));
            //skuDisCount,根据生成发货单的数量与skuOrder的数量按照比例四舍五入计算金额
            Long disCount = skuOrder.getDiscount() + Long.valueOf(this.getShareDiscount(skuOrder));
            shipmentItem.setSkuDiscount(this.getDiscount(originSkuOrder.getQuantity(), skuOrderIdAndQuantity.get(skuOrderId), Math.toIntExact(disCount)));
            //总净价
            shipmentItem.setCleanFee(this.getCleanFee(shipmentItem.getSkuPrice(), shipmentItem.getSkuDiscount(), shipmentItem.getQuantity()));
            //商品净价
            shipmentItem.setCleanPrice(this.getCleanPrice(shipmentItem.getCleanFee(), shipmentItem.getQuantity()));

            shipmentItem.setOutSkuCode(skuOrder.getOutSkuId());
            shipmentItem.setSkuCode(skuOrder.getSkuCode());
            //商品id
            String outItemId = "";
            try {
                outItemId = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.MIDDLE_OUT_ITEM_ID, skuOrder);
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

    private List<ShipmentItem> makeChangeShipmentItems(List<RefundItem> refundChangeItems, Map<String, Integer> skuCodeAndQuantity) {
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithExpectedSize(skuCodeAndQuantity.size());

        Map<String, RefundItem> refundItemMap = refundChangeItems.stream().filter(Objects::nonNull).collect(Collectors.toMap(RefundItem::getSkuCode, it -> it));
        for (String skuCode : skuCodeAndQuantity.keySet()) {
            ShipmentItem shipmentItem = new ShipmentItem();
            RefundItem refundItem = refundItemMap.get(skuCode);
            shipmentItem.setQuantity(skuCodeAndQuantity.get(skuCode));
            //退货数量,因为丢件补发或者是换货是允许继续售后的，所以这里面的数量为0
            shipmentItem.setRefundQuantity(0);
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
     *
     * @param shipmentId 发货单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/shipment/{id}/warehouse/company/rule", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseCompanyRule findWarehouseCompanyRule(@PathVariable("id") Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Long warehouseId = shipmentExtra.getWarehouseId();
        WarehouseDTO warehouse = findWarehouseById(warehouseId);
        Response<WarehouseCompanyRule> ruleRes = shipmentReadLogic.findCompanyRuleByWarehouseCode(warehouse.getWarehouseCode());
        if (!ruleRes.isSuccess()) {
            log.error("find warehouse company rule by company code:{} fail,error:{}", warehouse.getWarehouseCode(), ruleRes.getError());
            throw new JsonResponseException(ruleRes.getError());
        }

        return ruleRes.getResult();
    }


    /**
     * @param skuQuantity     sku订单中商品的数量
     * @param shipSkuQuantity 发货的sku商品的数量
     * @param skuDiscount     sku订单中商品的折扣
     * @return 返回四舍五入的计算结果, 得到发货单中的sku商品的折扣
     */
    private Integer getDiscount(Integer skuQuantity, Integer shipSkuQuantity, Integer skuDiscount) {
        return Math.round(Long.valueOf(skuDiscount) * Long.valueOf(shipSkuQuantity) / Long.valueOf(skuQuantity));
    }

    /**
     * 计算总净价
     *
     * @param skuPrice        商品原价
     * @param discount        发货单中sku商品的折扣
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return
     */
    private Integer getCleanFee(Integer skuPrice, Integer discount, Integer shipSkuQuantity) {

        return skuPrice * shipSkuQuantity - discount;
    }

    /**
     * 计算商品净价
     *
     * @param cleanFee        商品总净价
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return
     */
    private Integer getCleanPrice(Integer cleanFee, Integer shipSkuQuantity) {
        return Math.round(Long.valueOf(cleanFee) / Long.valueOf(shipSkuQuantity));
    }

    /**
     * 计算积分
     *
     * @param integral            sku订单获取的积分
     * @param skuQuantity         sku订单总的数量
     * @param shipmentSkuQuantity 发货单中该sku订单的数量
     * @return 获取发货单中sku订单的积分
     */
    private Integer getIntegral(Integer integral, Integer skuQuantity, Integer shipmentSkuQuantity) {
        return Math.round(integral * shipmentSkuQuantity / skuQuantity);
    }

    private String getShareDiscount(SkuOrder skuOrder) {
        String skuShareDiscount = "";
        try {
            skuShareDiscount = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_SHARE_DISCOUNT, skuOrder);
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_SHARE_DISCOUNT);
        }
        return org.apache.commons.lang3.StringUtils.isEmpty(skuShareDiscount) ? "0" : skuShareDiscount;
    }



    /**
     * 宝胜二期--单个发货单撤销功能
     * @param shipmentId
     */
    @RequestMapping(value = "api/single/shipment/{id}/rollback", method = RequestMethod.PUT)
    public void rollbackShopOrder(@PathVariable("id") Long shipmentId) {
        log.info("try to cancel shipemnt, shipmentId is {}",shipmentId);
        Response<Boolean> response = shipmentWiteLogic.rollbackShipment(shipmentId);
        if (!response.isSuccess()){
            log.info("try to cancel shipment, shipmentId is {}", shipmentId);
            throw new JsonResponseException(response.getError());
        }
    }


    /**
     * 根据电商店铺id获取发货单下所有的货品集合
     *
     * @param code
     * @return
     */
    @RequestMapping(value = "api/order/{id}/shipment/items", method = RequestMethod.GET)
    public List<ShipmentItem> findShipmentItemByOrder(@PathVariable("id") String code) {
        //获取订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(code);
        //获取订单下的所有发货单
        List<Shipment> originShipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
        List<Shipment> shipments = originShipments.stream().
                filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue())
                && !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))
                .collect(Collectors.toList());
        //获取订单下对应发货单的所有发货商品列表
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItemsForList(shipments);
        return shipmentItems;
    }

    /**
     * 订单派发中心取消发货单
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "api/shipment/cancel/{id}/to/yyedi", method = RequestMethod.GET)
    public boolean cancelShipmentForEdi(@PathVariable("id") Long id) {
        Shipment shipment = shipmentReadLogic.findShipmentById(id);
        Response<Boolean> r = syncErpShipmentLogic.syncShipmentCancel(shipment);
        return r.getResult();
    }

    /*
     * 同步发货单到mpos
     *
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/sync/mpos", method = RequestMethod.PUT)
    public void syncMposShipment(@PathVariable(value = "id") Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncMposShipmentLogic.syncShipmentToMpos(shipment);
        if (!syncRes.isSuccess()) {
            log.error("sync shipment(id:{}) to mpos fail,error:{}", shipmentId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }

    /**
     * 测试同步发货单到恒康开pos单
     *
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/sync/hk/pos", method = RequestMethod.GET)
    public void syncShipmentToHk(@PathVariable(value = "id") Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
        if (!syncRes.isSuccess()) {
            log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }

    }

    /**
     * 测试同步发货单到恒康开pos单
     *
     * @param shipmentId 发货单id
     */
    @RequestMapping(value = "api/shipment/{id}/confirm/at/sync/hk", method = RequestMethod.GET)
    public void syncShipmentDoneToHkForPos(@PathVariable(value = "id") Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncShipmentPosLogic.syncShipmentDoneToHk(shipment);
        if (!syncRes.isSuccess()) {
            log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }

    /**
     *根据shipmentcode获取shipmentExtr信息
     * @param code
     */
    @RequestMapping(value = "api/shipment/{code}/extra/get", method = RequestMethod.GET)
    public ShipmentExtra findShipmentExtra(@PathVariable(value = "code") @OperationLogParam String code) {
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(code);
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        return shipmentExtra;
    }


    /**
     *全渠道订单判断是否是店仓发货
     * @param warehouseIds 仓库列表
     * @param shopOrderId  店铺订单id
     */
    private void validateOrderAllChannelWarehouse(List<Long> warehouseIds,Long shopOrderId){

        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //mpos门店可以指定店仓发货
        if(orderReadLogic.isMposOpenShop(shopOrder.getShopId())){
            return;
        }
        if (!orderReadLogic.isAllChannelOpenShop(shopOrder.getShopId())) {
            Response<List<WarehouseDTO>> r = warehouseClient.findByIds(warehouseIds);
            if (!r.isSuccess()) {
                log.error("find warehouses failed,ids are {},caused by {}", warehouseIds, r.getError());
                throw new JsonResponseException("find.warehouse.failed");
            }
            List<WarehouseDTO> warehouses = r.getResult();
            int count = 0;
            for (WarehouseDTO warehouse : warehouses) {
                //如果是店仓
                if (Objects.equals(warehouse.getWarehouseSubType(), 1)) {
                    count++;
                }
            }
            if (count>0){
                throw new JsonResponseException("can.not.contain.shop.warehouse");
            }
        }

    }

    /**
     * 判断是否是导入订单，导入订单不能使用店仓
     * @param warehouseIds
     * @param shopOrderId
     */
    private void validateIsCreateOrderImportOrder(List<Long> warehouseIds,Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        if (orderReadLogic.isCreateOrderImportOrder(shopOrder)){
            validateOrderAllChannelWarehouse(warehouseIds,shopOrderId);
        }

    }
    /**
     *全渠道订单判断是否是店仓发货
     * @param warehouseIds 仓库列表
     * @param refundId  售后订单id
     */
    private void validateRefundAllChannelWarehouse(List<Long> warehouseIds,Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        //mpos门店可以指定店仓发货
        if(orderReadLogic.isMposOpenShop(refund.getShopId())){
            return;
        }
        checkCanShopWarehouseShip(warehouseIds,refund.getShopId());
    }

    private void checkCanShopWarehouseShip(List<Long> warehouseIds,Long shopId) {

        if (!orderReadLogic.isAllChannelOpenShop(shopId)) {
            Response<List<WarehouseDTO>> r = warehouseClient.findByIds(warehouseIds);
            if (!r.isSuccess()) {
                log.error("find warehouses failed,ids are {},caused by {}", warehouseIds, r.getError());
                throw new JsonResponseException("find.warehouse.failed");
            }
            List<WarehouseDTO> warehouses = r.getResult();
            int count = 0;
            for (WarehouseDTO warehouse : warehouses) {
                //如果是店仓
                if (Objects.equals(warehouse.getWarehouseSubType(), 1)) {
                    count++;
                }
            }
            if (count > 0) {
                throw new JsonResponseException("can.not.contain.shop.warehouse");
            }
        }
    }
    /**
     * 根据售后单id获取发货单下所有的货品集合
     *
     * @param code
     * @return
     */
    @RequestMapping(value = "api/order/{id}/aftersale/shipment/items", method = RequestMethod.GET)
    public List<ShipmentItem> findShipmentItemByAfterSaleId(@PathVariable("id") String code) {
        Refund refund = refundReadLogic.findRefundByRefundCode(code);
       return shipmentReadLogic.findAfterSaleShipmentItems(refund.getRefundCode());
    }

    /**
     * 单笔修复shipment的发货方式数据
     *
     * @param shipmentId
     * @return
     */
    @ApiOperation("修复发货方式数据(单笔)")
    @RequestMapping(value = "api/shipment/{id}/update/shipway", method = RequestMethod.PUT)
    public Response<Boolean> singleFixShipWay(@PathVariable(value = "id") @LogMeId Long shipmentId) {

        Shipment shipment = null;
        try {
            shipment = shipmentReadLogic.findShipmentById(shipmentId);
//            String shipmentWay = shipment.getExtra().get("shipmentWay");
            String shipmentWay = (String) JSON_MAPPER.fromJson(shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO), Map.class).get("shipmentWay");

            shipment.setShipWay(Integer.parseInt(shipmentWay));
            return shipmentWriteService.update(shipment);
        } catch (Exception e) {
            log.error("failed to update {}, cause:{}", shipment, Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.update.fail");
        }
    }

    /**
     * 一次性修复全部的数据
     * @return
     */
    @ApiOperation("修复发货方式数据(一次更新全部)")
    @RequestMapping(value = "api/shipment/batch/update/shipway", method = RequestMethod.PUT)
    public Response<Boolean> batchFixShipWay() {

        int pageNo = 1;
        int pageSize = 5000;
        try {
            while (true) {

                Response<Paging<Shipment>> response = shipmentReadService.pagingByStatus(pageNo, pageSize, null);
                if (response.isSuccess()) {

                    if (response.getResult().getData().size() == 0) {
                        break;
                    }
                    Stream<Shipment> afterFiler = response.getResult().getData().stream().filter(item -> null == item.getShipWay());

                    afterFiler.parallel().forEach(shipment -> {
                        if (CollectionUtils.isEmpty(shipment.getExtra()) || shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO) == null) {

                            shipment.setShipWay(Integer.parseInt(TradeConstants.MPOS_WAREHOUSE_DELIVER));//不存在设置仓发
                        } else {
                            String shipmentWay = (String) JSON_MAPPER.fromJson(shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO), Map.class).get("shipmentWay");

                            if (StringUtils.isEmpty(shipmentWay)) { //不存在设置仓发
                                shipment.setShipWay(Integer.parseInt(TradeConstants.MPOS_WAREHOUSE_DELIVER));
                            } else {
                                //处理旧数据 shipmentWay不是数字的情况
                                if (!Pattern.compile("^[0-9]+$").matcher(shipmentWay).matches()) {
                                    return;
                                }
                                shipment.setShipWay(Integer.parseInt(shipmentWay));
                            }
                        }
                        shipmentWriteService.update(shipment);
                    });
                    pageNo++;
                } else {
                    throw new JsonResponseException("shipment.update.fail");
                }
            }
            return Response.ok(true);
        } catch (Exception e) {
            log.error("failed to batch update {}, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.update.fail");
        }

    }




    @RequestMapping(value = "api/order/shipment/fix")
    public Integer fixOrderShipment(OrderShipmentCriteria criteria){
        int count = 0;
        int errorCount = 0;
        /**
         * 分页获取数据，再做转换
         */
        int pageNo = 0,pageSize = 100;
        boolean next = true;
        log.info("start fix order shipment spucode and address.....");
        /*while(next){
            pageNo ++;
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);
            Response<Paging<OrderShipment>> response = orderShipmentReadService.paging(criteria);
            if(!response.isSuccess()){
                log.error("find order shipment by criteria:{} failed,cause:{}",criteria,response.getError());
                throw new JsonResponseException(response.getError());
            }
            List<OrderShipment> list = response.getResult().getData();
            for (OrderShipment os:list) {
                if(os.getProvinceId() != null) {
                    continue;
                }
                OrderShipment os1 = new OrderShipment();
                try{
                    Shipment shipment = shipmentReadLogic.findShipmentById(os.getShipmentId());
                    os1.setId(os.getId());
                    os1.setSpuCodes(getSpusFromShipment(shipment));
                    ReceiverInfo receiverInfo = this.trans2ReceiverInfo(shipment.getReceiverInfos());
                    if(receiverInfo != null){
                        if(receiverInfo.getProvinceId() != null) {
                            os1.setProvinceId(receiverInfo.getProvinceId().longValue());
                        }
                        if(receiverInfo.getCityId() != null) {
                            os1.setCityId(receiverInfo.getCityId().longValue());
                        }
                        if(receiverInfo.getRegionId() != null) {
                            os1.setRegionId(receiverInfo.getRegionId().longValue());
                        }
                    }
                    orderShipmentWriteService.update(os1);
                }catch (Exception e){
                    errorCount ++;
                    log.error("fix shipment(id:{}) failed,cause:{}", os.getShipmentId(),Throwables.getStackTraceAsString(e));
                    continue;
                }
            }
            count += list.size();
            log.info("fix order shipment count:{},failed count:{}",count,errorCount);
            if(list.size() < pageSize) {
                next = false;
            }
        }*/
        log.info("fix order shipment spucode and address end.....");
        return count;
    }

    private String getSpusFromShipment(Shipment shipment){
        StringBuilder sb = new StringBuilder();
        if(shipment.getSkuInfos() == null) {
            return null;
        }
        Set<Long> skuOrderIds = shipment.getSkuInfos().keySet();
        Response<List<SkuOrder>> response = skuOrderReadService.findByIds(Lists.newArrayList(skuOrderIds));
        if(!response.isSuccess()){
            log.error("find sku order by ids:{} failed,cause:{}",skuOrderIds,response.getError());
            return null;
        }
        List<Long> skuTemplatesId = response.getResult().stream().map(SkuOrder::getSkuId).collect(Collectors.toList());
        Response<List<SkuTemplate>> response1 = skuTemplateReadService.findByIds(skuTemplatesId);
        if(!response1.isSuccess()){
            log.error("find sku by ids:{} failed,cause:{}",skuOrderIds,response.getError());
            return null;
        }
        List<SkuTemplate> list = response1.getResult();
        for (SkuTemplate sku:list) {
            String spuCode = sku.getExtra().get("materialCode");
            if(sb.length() > 0) {
                sb.append(",").append(spuCode);
            } else {
                sb.append(spuCode);
            }
        }
        return sb.toString();
    }

    private ReceiverInfo trans2ReceiverInfo(String receiverJson){
        try{
            return JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(receiverJson,ReceiverInfo.class);
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 一次性修复全部的数据
     * @return
     */
    @ApiOperation("修复发货仓库对应的店铺ID数据(一次更新全部)")
    @RequestMapping(value = "api/shipment/batch/update/shipId", method = RequestMethod.GET)
    public Response<Boolean> batchFixShipId() {

        log.info("******begin batch fix shipment`s shipId******");

        int pageNo = 1;
        int pageSize = 5000;
        try {
            while (true) {

                Response<Paging<Shipment>> response = shipmentReadService.pagingByShipId(pageNo, pageSize, null);
                if (response.isSuccess()) {

                    if (response.getResult().getData().size() == 0) {
                        break;
                    }
                    Stream<Shipment> afterFiler = response.getResult().getData().stream().filter(item -> null == item.getShipId());

                    afterFiler.parallel().forEach(shipment -> {
                        if (! CollectionUtils.isEmpty(shipment.getExtra()) && shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO) != null) {

                            Integer wareHouserId = (Integer) JSON_MAPPER.fromJson(shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO), Map.class).get("warehouseId");

                            if (!StringUtils.isEmpty(shipment.getShipWay()) && Objects.equals(shipment.getShipWay(), 2) ) { //存在并且为仓发设置为仓库ID
                                shipment.setShipId(wareHouserId == null ? null: Long.valueOf(wareHouserId));
                            } else if (!StringUtils.isEmpty(shipment.getShipWay()) && Objects.equals(shipment.getShipWay(), 1) ) {
                                //店仓发货，根据发货仓库id查找对应的店铺信息
                                if (!StringUtils.isEmpty(wareHouserId)){
                                    try {
                                        Long shopId = getShipIdByDeliverId(Long.valueOf(wareHouserId));
                                        shipment.setShipId(shopId);
                                    } catch (Exception e){
                                        log.error("find paranaShops is error,the wareHouserId:{}", wareHouserId);
                                    }

                                }
                            }
                            if (shipment.getShipId() != null){
                               Response result = shipmentWriteService.updateShipId(shipment.getId(), shipment.getShipId());
                               if (!result.isSuccess()){
                                   log.error("update shipment ship id to:{} by shipment id:{} fail,error:{}",
                                           shipment.getShipId(),shipment.getId(),result.getError());
                               }
                            }
                        }
                    });
                    pageNo++;
                } else {
                    throw new JsonResponseException("can`t find shipment,shipment.update.fail");
                }
            }
            log.info("******end fix shipment`s shipId******");
            return Response.ok(true);
        } catch (Exception e) {
            log.error("failed to batch update, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.update.fail");
            }

    }

    /**
     * 查找店发时，店仓对应的店铺id
     * @param deliverShopId
     * @returnshipID
     *
     */
    private Long getShipIdByDeliverId(Long deliverShopId) {
        Shop shop = shopCacher.findShopById(deliverShopId);
        ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
        return shopExtraInfo != null ? shopExtraInfo.getOpenShopId(): null ;
    }

    //快递单详情
    @ApiOperation("快递单详情")
    @RequestMapping(value = "/api/shipmentExpress/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ShipmentExpress findDetail(@RequestParam(required = true, value = "shipmentCode") String shipmentCode,@RequestParam(required = true, value = "shipmentExpressCode") String shipmentExpressCode) {
        Response<ShipmentExpress> result = shipmentReadService.findShipmentExpress(shipmentCode, Splitters.COMMA.splitToList(shipmentExpressCode).get(0));
        return  result.getResult();
    }


    /**
     * 修复发货单金额
     * @param shopId
     */
    @RequestMapping(value = "api/shipment/{shopId}/update/amount",method = RequestMethod.GET)
    public void updateShipmentsAmount(@PathVariable(value = "shopId")Long shopId){
        int pageNo= 0;
        while(true){
            OrderShipmentCriteria shipmentCriteria = new OrderShipmentCriteria();
            shipmentCriteria.setShopId(shopId);
            log.info("pageNo is {}",pageNo);
            shipmentCriteria.setPageNo(pageNo);
            Response<Paging<ShipmentPagingInfo>> response = orderShipmentReadService.findBy(shipmentCriteria);
            if (!response.isSuccess()) {
                log.error("find shipment by criteria:{} fail,error:{}", shipmentCriteria, response.getError());
                throw new JsonResponseException(response.getError());
            }
            List<ShipmentPagingInfo> shipmentPagingInfos = response.getResult().getData();
            if (shipmentPagingInfos.isEmpty()){
                break;
            }
            for (ShipmentPagingInfo shipmentPagingInfo:shipmentPagingInfos) {
                try{

                    Shipment shipment  = shipmentPagingInfo.getShipment();
                    if (shipment.getStatus()<0){
                        log.info("shipment status <0");
                        continue;
                    }
                    Map<Long,Integer> skuInfos = shipment.getSkuInfos();
                    ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

                    List<Long> skuOrderIds = skuInfos.keySet().stream().collect(Collectors.toList());
                    log.info("skuOrderIds is {}",skuOrderIds);
                    List<SkuOrder> skuOrders =  orderReadLogic.findSkuOrdersByIds(skuOrderIds);
                    ShopOrder shopOrder = orderReadLogic.findShopOrderById(shipmentPagingInfo.getOrderShipment().getOrderId());
                    //判断发货单中的运费是否被计算过
                    List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
                    //查询发货单中的shipmentExtra的运费金额是否大于0
                    Optional<ShipmentExtra> shipFeeShipmentExtra = shipments.stream().filter(Objects::nonNull).filter(s->(s.getStatus()>0)).
                            flatMap(s1->Lists.newArrayList(shipmentReadLogic.getShipmentExtra(s1)).stream()).filter(extra->(extra.getShipmentShipFee()>0)).findAny();
                    //运费
                    Long shipmentShipFee = 0L;
                    //运费优惠
                    Long shipmentShipDiscountFee = 0L;

                    if (!shipFeeShipmentExtra.isPresent()){
                        shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee() == null ? 0 : shopOrder.getOriginShipFee());
                        shipmentShipDiscountFee = shipmentShipFee - Long.valueOf(shopOrder.getShipFee() == null ? 0 : shopOrder.getShipFee());
                    }

                    List<ShipmentItem> newShipmentItems =  shipmentWiteLogic.makeShipmentItems(skuOrders,skuInfos);
                    //发货单商品金额
                    Long shipmentItemFee = 0L;
                    //发货单总的优惠
                    Long shipmentDiscountFee = 0L;
                    //发货单总的净价
                    Long shipmentTotalFee = 0L;
                    for (ShipmentItem shipmentItem : newShipmentItems) {
                        shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
                        shipmentDiscountFee = shipmentItem.getSkuDiscount() + shipmentDiscountFee;
                        shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
                    }
                    Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;

                    shipmentExtra.setShipmentItemFee(shipmentItemFee);
                    //发货单运费金额
                    shipmentExtra.setShipmentShipFee(shipmentShipFee);
                    //发货单优惠金额
                    shipmentExtra.setShipmentDiscountFee(shipmentDiscountFee);
                    //发货单总的净价
                    shipmentExtra.setShipmentTotalFee(shipmentTotalFee);
                    shipmentExtra.setShipmentShipDiscountFee(shipmentShipDiscountFee);

                    shipmentExtra.setShipmentTotalPrice(shipmentTotalPrice);
                    Map<String, String> extraMap = shipment.getExtra();
                    extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(newShipmentItems));
                    extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
                    shipment.setExtra(extraMap);
                    shipmentWiteLogic.update(shipment);
                }catch (Exception e){
                    log.error("update shipment amount failed, caused by {}",Throwables.getStackTraceAsString(e));
                }
            }
            pageNo++;
        }
    }


    /**
     * 修复发货单金额，根据发货单id
     * @param shopId
     */
    @RequestMapping(value = "api/shipment/{shopId}/update/amount/by/order/id",method = RequestMethod.GET)
    public void updateShipmentsAmountByShopOrderId(@PathVariable(value = "shopId")Long shopId,@RequestParam("shopOrderId") Long shopOrderId){
            log.info("update shipment amount ,shopId is {},shopOrderId is {}",shopId,shopOrderId);
            Response<List<OrderShipment>> response = orderShipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
            if (!response.isSuccess()) {
                log.error("find shipment by response:{} fail,error:{}", response, response.getError());
                throw new JsonResponseException(response.getError());
            }
            List<OrderShipment> orderShipments = response.getResult();

            for (OrderShipment orderShipment:orderShipments) {
                try{

                    Shipment shipment  = shipmentReadLogic.findShipmentById(orderShipment.getShipmentId());
                    if (shipment.getStatus()<0){
                        log.info("shipment status <0");
                        continue;
                    }
                    Map<Long,Integer> skuInfos = shipment.getSkuInfos();
                    ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

                    List<Long> skuOrderIds = skuInfos.keySet().stream().collect(Collectors.toList());
                    log.info("skuOrderIds is {}",skuOrderIds);
                    List<SkuOrder> skuOrders =  orderReadLogic.findSkuOrdersByIds(skuOrderIds);
                    ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
                    //判断发货单中的运费是否被计算过
                    List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
                    //查询发货单中的shipmentExtra的运费金额是否大于0
                    Optional<ShipmentExtra> shipFeeShipmentExtra = shipments.stream().filter(Objects::nonNull).filter(s->(s.getStatus()>0)).
                            flatMap(s1->Lists.newArrayList(shipmentReadLogic.getShipmentExtra(s1)).stream()).filter(extra->(extra.getShipmentShipFee()>0)).findAny();
                    //运费
                    Long shipmentShipFee = 0L;
                    //运费优惠
                    Long shipmentShipDiscountFee = 0L;

                    if (!shipFeeShipmentExtra.isPresent()){
                        shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee() == null ? 0 : shopOrder.getOriginShipFee());
                        shipmentShipDiscountFee = shipmentShipFee - Long.valueOf(shopOrder.getShipFee() == null ? 0 : shopOrder.getShipFee());
                    }

                    List<ShipmentItem> newShipmentItems =  shipmentWiteLogic.makeShipmentItems(skuOrders,skuInfos);
                    //发货单商品金额
                    Long shipmentItemFee = 0L;
                    //发货单总的优惠
                    Long shipmentDiscountFee = 0L;
                    //发货单总的净价
                    Long shipmentTotalFee = 0L;
                    for (ShipmentItem shipmentItem : newShipmentItems) {
                        shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
                        shipmentDiscountFee = shipmentItem.getSkuDiscount() + shipmentDiscountFee;
                        shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
                    }
                    Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;

                    shipmentExtra.setShipmentItemFee(shipmentItemFee);
                    //发货单运费金额
                    shipmentExtra.setShipmentShipFee(shipmentShipFee);
                    //发货单优惠金额
                    shipmentExtra.setShipmentDiscountFee(shipmentDiscountFee);
                    //发货单总的净价
                    shipmentExtra.setShipmentTotalFee(shipmentTotalFee);
                    shipmentExtra.setShipmentShipDiscountFee(shipmentShipDiscountFee);

                    shipmentExtra.setShipmentTotalPrice(shipmentTotalPrice);
                    Map<String, String> extraMap = shipment.getExtra();
                    extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(newShipmentItems));
                    extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
                    shipment.setExtra(extraMap);
                    shipmentWiteLogic.update(shipment);
                }catch (Exception e){
                    log.error("update shipment amount failed, caused by {}",Throwables.getStackTraceAsString(e));
                }

            }
    }

    /**
     * 同步恒康确认收货失败批量补偿脚本
     */
    @RequestMapping(value = "api/shipment/recover/confirm/hk",method = RequestMethod.GET)
    public void recoverShipmentConfirmAt(){
        int pageNo = 1;
        int pageSize = 40;
        while(true){
            Response<Paging<Shipment>> response = shipmentReadService.pagingByStatus(pageNo,pageSize,MiddleShipmentsStatus.CONFIRMED_FAIL.getValue());
            if (!response.isSuccess()){
                log.error("pagingByStatus failed");
                pageNo++;
                continue;
            }
            Paging<Shipment> shipmentPaging = response.getResult();
            List<Shipment> shipments = shipmentPaging.getData();
            if (shipments.isEmpty()){
                log.error("all recoverShipmentConfirmAt,pageNo {}",pageNo);
                break;
            }
            for (Shipment shipment:shipments){
                Response<Boolean> syncRes = syncErpShipmentLogic.syncShipmentDone(shipment, 2, MiddleOrderEvent.HK_CONFIRME_FAILED.toOrderOperation());
                if (!syncRes.isSuccess()) {
                    log.error("sync  shipment(id:{}) done to hk fail,error:{}", shipment.getId(), syncRes.getError());
                }
            }
            pageNo++;
        }
    }
}
