package com.pousheng.middle.web.order;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.pousheng.middle.open.OPMessageSources;
import com.pousheng.middle.open.component.OpenClientOrderLogic;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.MiddleOrderInfo;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import com.pousheng.middle.web.order.sync.vip.SyncVIPLogic;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentOccupyType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tony on 2017/7/18.
 * pousheng-middle
 */
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.ORDER)
@PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
@Api(description = "订单管理")
public class AdminOrderWriter {
    @Autowired
    private SyncOrderToEcpLogic syncOrderToEcpLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private ExpressCodeReadService expressCodeReadService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private RefundReadService refundReadService;
    @Autowired
    private OpenClientOrderLogic openClientOrderLogic;
    @Autowired
    private OPMessageSources opMessageSources;
    @Autowired
    private SyncVIPLogic syncVIPLogic;
    @Autowired
    private ShopOrderReadService shopOrderReadService;


    @Value("${logging.path}")
    private String filePath;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    /**
     * 发货单已发货,同步订单信息到电商
     *
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/sync/ecp", method = RequestMethod.PUT)
    @PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
    @ApiOperation("发货单已发货,同步订单信息到电商")
    @LogMe(description = "发货单已发货,同步订单信息到电商",ignore = true)
    public void syncOrderInfoToEcp(@PathVariable(value = "id") @PermissionCheckParam @LogMeContext Long shopOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-SYNCORDERINFOTOECP-START param: shopOrderId [{}] ",shopOrderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue()) && !Objects.equals(shopOrder.getOutFrom(), MiddleChannel.OFFICIAL.getValue())
                && !Objects.equals(shopOrder.getOutFrom(), MiddleChannel.SUNINGSALE.getValue())) {
            //获取发货单id
            String ecpShipmentId = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_SHIPMENT_ID, shopOrder);
            Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(ecpShipmentId));
            //如果是唯品会的渠道
            if (shopOrder.getOutFrom().equals(MiddleChannel.VIP.getValue())) {
                if (shipment.getShipWay() == 2) {
                    Response<Boolean> response = syncVIPLogic.syncOrderStoreToVIP(shipment);
                    if (!response.isSuccess()) {
                        log.error("fail to notice oxo store order  shipment (id:{})  ", ecpShipmentId);
                        throw new JsonResponseException(response.getError());
                    }
                } else {
                    OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                    orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
                }
                return;
            }
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            ExpressCode expressCode = makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
            //同步到电商平台
            String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);
            if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.YUNJUBBC.getValue())
                || Objects.equals(shopOrder.getOutFrom(), MiddleChannel.YUNJUJIT.getValue())) {
                //同步到云聚
                syncOrderToEcpLogic.syncToYunJu(shopOrder);

            }
            else {
                Response<Boolean> syncRes = syncOrderToEcpLogic.syncOrderToECP(shopOrder, expressCompanyCode, shipment.getId());
                if (!syncRes.isSuccess()) {
                    log.error("sync shopOrder(id:{}) to ecp fail,error:{}", shopOrderId, syncRes.getError());
                    throw new JsonResponseException(syncRes.getError());
                }
            }

        } else {
            Response<Boolean> syncRes = syncOrderToEcpLogic.syncShipmentsToEcp(shopOrder);
            if (!syncRes.isSuccess()) {
                log.error("sync shopOrder(id:{}) to ecp fail,error:{}", shopOrderId, syncRes.getError());
                throw new JsonResponseException(syncRes.getError());
            }
        }
        if(log.isDebugEnabled()){
            log.debug("API-SYNCORDERINFOTOECP-END param: shopOrderId [{}] ",shopOrderId);
        }

    }

    /**
     * 取消子订单(自动)
     *
     * @param shopOrderId
     * @param skuCode
     */
    @RequestMapping(value = "api/order/{id}/auto/cancel/sku/order", method = RequestMethod.PUT)
    @ApiOperation("取消子订单(自动)")
    public void autoCancelSkuOrder(@PathVariable("id") @PermissionCheckParam Long shopOrderId, @RequestParam("skuCode") String skuCode) {
        log.info("try to auto cancel sku order shop orderId is {},skuCode is {}", shopOrderId, skuCode);
        orderWriteLogic.autoCancelSkuOrder(shopOrderId, skuCode);
        log.info("end try to auto cancel sku order shop orderId is {},skuCode is {}", shopOrderId, skuCode);

    }

    /**
     * 整单撤销,状态恢复成初始状态
     *
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/rollback/shop/order", method = RequestMethod.PUT)
    @OperationLogType("整单撤销")
    public void rollbackShopOrder(@PathVariable("id") @PermissionCheckParam Long shopOrderId) {
        log.info("try to roll back shop order shopOrderId is {}", shopOrderId);
        Response<Boolean> response = orderWriteLogic.rollbackShopOrder(shopOrderId);
        if (!response.isSuccess()) {
            throw new JsonResponseException("rollback.shop.order.failed");
        }
        //如果未处理原因是备注订单已经占库被撤销的，则未处理原因变为备注订单客服取消占库发货单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        if (Objects.equals(shopOrder.getHandleStatus(),OrderWaitHandleType.NOTE_ORDER_OCCUPY_SHIPMENT_CREATED.value())){
            shipmentWiteLogic.updateShipmentNote(shopOrder,OrderWaitHandleType.NOTE_ORDER_OCCUPY_SHIPMENT_CANCELED.value());
        }
        log.info("end try to roll back shop order shopOrderId is {}", shopOrderId);

    }

    /**
     * 订单(包括整单和子单)取消失败,手工操作逻辑
     *
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/cancel/order", method = RequestMethod.PUT)
    @OperationLogType("人工取消订单")
    public void cancelShopOrder(@PathVariable("id") @PermissionCheckParam @OperationLogParam Long shopOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-CANCELSHOPORDER-START param: shopOrderId [{}]",shopOrderId);
        }
        //判断是整单取消还是子单取消
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //获取是否存在失败的sku记录
        String skuCodeCanceled = "";
        try {
            skuCodeCanceled = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.SKU_CODE_CANCELED, shopOrder);
        } catch (Exception e) {
            log.info("skuCode is not exist,because of not cancel sku order");
        }
        if (StringUtils.isNotEmpty(skuCodeCanceled)) {
            log.info("try to cancel sku order shopOrderId is {},skuCode is {}", shopOrderId, skuCodeCanceled);
            Response<Boolean> response = orderWriteLogic.cancelSkuOrder(shopOrderId, skuCodeCanceled);
            if (!response.isSuccess()) {
                throw new JsonResponseException("cancel.sku.order.failed");
            }
        } else {
            log.info("try to cancel shop order shopOrderId is {},skuCode is {}", shopOrderId, skuCodeCanceled);
            Response<Boolean> response = orderWriteLogic.cancelShopOrder(shopOrderId);
            if (!response.isSuccess()) {
                throw new JsonResponseException("cancel.shop.order.failed");
            }
        }
        if(log.isDebugEnabled()){
            log.debug("API-CANCELSHOPORDER-END param: shopOrderId [{}]",shopOrderId);
        }
    }

    /**
     * 整单取消,子单整单发货单状态变为已取消(自动)
     *
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/auto/cancel/shop/order", method = RequestMethod.PUT)
    public void autoCancelShopOrder(@PathVariable("id") @PermissionCheckParam Long shopOrderId) {
        log.info("try to auto cancel shop order shopOrderId is {},skuCode is {}", shopOrderId);
        orderWriteLogic.autoCancelShopOrder(shopOrderId);
        log.info("end to auto cancel shop order shopOrderId is {},skuCode is {}", shopOrderId);
    }


    /**
     * 电商确认收货,此时通知中台修改shopOrder中ecpOrderStatus状态为已完成
     *
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/confirm", method = RequestMethod.PUT)
    public void confirmOrders(@PathVariable("id") @PermissionCheckParam Long shopOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-CONFIRMORDERS-START param: shopOrderId [{}]",shopOrderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        orderWriteLogic.updateEcpOrderStatus(shopOrder, MiddleOrderEvent.CONFIRM.toOrderOperation());
        if(log.isDebugEnabled()){
            log.debug("API-CONFIRMORDERS-END param: shopOrderId [{}]",shopOrderId);
        }
    }

    /**
     * 修改skuCode 和skuId
     *
     * @param id      sku订单主键
     * @param skuCode 中台条码
     */
    @RequestMapping(value = "/api/sku/order/{id}/update/sku/code", method = RequestMethod.PUT)
    @OperationLogType("修改货品条码")
    public void updateSkuOrderCodeAndSkuId(@PathVariable("id") @OperationLogParam Long id, @RequestParam("skuCode") String skuCode) {
        if(log.isDebugEnabled()){
            log.debug("API-UPDATESKUORDERCODEANDSKUID-START param: id [{}] skuCode [{}]",id,skuCode);
        }
        //判断该订单是否生成过发货单
        Boolean result = orderReadLogic.isShipmentCreated(id);
        if (!result) {
            throw new JsonResponseException("shipment.exist.can.not.edit.sku.code");
        }
        SkuOrder skuOrder = (SkuOrder) orderReadLogic.findOrder(id, OrderLevel.SKU);
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(skuOrder.getOrderId());
        //获取其他子单的条码
        List<String> orderSkuCodes = skuOrders.stream().filter(skuOrder1 -> !Objects.equals(skuOrder1.getId(), id)).map(SkuOrder::getSkuCode).collect(Collectors.toList());
        //如果其他子单含有这个条码则抛出异常
        if (orderSkuCodes.contains(skuCode)) {
            throw new JsonResponseException("other.sku.orders.contains.this.sku.code");
        }
        List<String> skuCodes = Lists.newArrayList();
        skuCodes.add(skuCode);
        Response<List<SkuTemplate>> skuResponse = skuTemplateReadService.findBySkuCodes(skuCodes);
        if (!skuResponse.isSuccess() || skuResponse.getResult().size() == 0) {
            log.error("find sku template failed,skuCode is {}", skuCode);
            throw new JsonResponseException(("find.sku.template.failed"));
        }
        SkuTemplate skuTemplate = skuResponse.getResult().get(0);
        Response<Boolean> response = middleOrderWriteService.updateSkuInfo(skuTemplate, id);
        if (!response.isSuccess()) {
            log.error("update skuCode failed,skuCodeId is({})", id);
            throw new JsonResponseException(response.getError());
        }
        //todo 一旦存在修改skuCode
        if(log.isDebugEnabled()){
            log.debug("API-UPDATESKUORDERCODEANDSKUID-END param: id [{}] skuCode [{}]",id,skuCode);
        }
    }

    /**
     * 添加中台客服备注,各个状态均可添加
     *
     * @param id                  店铺订单主键
     * @param customerSerivceNote 客服备注
     */
    @ApiOperation("添加中台客服备注")
    @LogMe(description = "添加中台客服备注", ignore = true)
    @RequestMapping(value = "/api/order/{id}/add/customer/service/note", method = RequestMethod.PUT)
    public void addCustomerServiceNote(@PathVariable("id") @LogMeContext Long id, @RequestParam("customerSerivceNote") @LogMeContext String customerSerivceNote) {
        if(log.isDebugEnabled()){
            log.debug("API-ADDCUSTOMERSERVICENOTE-START param: id [{}] customerSerivceNote [{}]",id,customerSerivceNote);
        }
        orderWriteLogic.addCustomerServiceNote(id, customerSerivceNote);
        if(log.isDebugEnabled()){
            log.debug("API-ADDCUSTOMERSERVICENOTE-END param: id [{}] customerSerivceNote [{}]",id,customerSerivceNote);
        }
    }

    /**
     * 修改订单的收货信息
     *
     * @param id        店铺订单主键
     * @param data      收货信息实体
     * @param buyerNote 买家备注
     * @return true (更新成功)or false (更新失败)
     */
    @RequestMapping(value = "/api/order/{id}/edit/receiver/info", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @LogMe(description = "修改订单的收货信息",ignore = true)
    public void editReceiverInfos(
            @PathVariable("id") @LogMeContext Long id,
            @RequestParam("data") @LogMeContext String data,
            @RequestParam(value = "buyerNote", required = false) @LogMeContext String buyerNote) {
        if(log.isDebugEnabled()){
            log.debug("API-EDITRECEIVERINFOS-START param: id [{}] data [{}] buyerNote [{}]",id,data,buyerNote);
        }
        Boolean result = orderReadLogic.isShipmentCreatedForShopOrder(id);
        if (!result) {
            throw new JsonResponseException("shipment.exist.can.not.edit.sku.code");
        }
        Map<String, Object> receiverInfoMap = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, String.class, Object.class));
        if (receiverInfoMap == null) {
            log.error("failed to parse receiverInfoMap:{}", data);
            throw new JsonResponseException("receiver.info.map.invalid");
        }
        //更新收货信息以及更新订单表中的手机号字段（中台是使用outBuyerId作为手机号）
        Response<Boolean> response = middleOrderWriteService.updateReceiveInfos(id, receiverInfoMap, buyerNote);
        if (!response.isSuccess()) {
            log.error("failed to edit receiver info:{},shopOrderId is(={})", data, id);
            throw new JsonResponseException(response.getError());
        }
        //抛出一个事件用来修改手机号
        // ModifyMobileEvent event = new ModifyMobileEvent();`
        // event.setShopOrderId(id);
        // eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-EDITRECEIVERINFOS-END param: id [{}] data [{}] buyerNote [{}]",id,data,buyerNote);
        }
    }

    /**
     * 修改订单的发票信息
     *
     * @param id
     * @param data
     * @return
     */
    @RequestMapping(value = "/api/order/{id}/edit/invoice", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @LogMe(description = "修改订单的发票信息", ignore = true)
    public void editInvoiceInfos(@PathVariable("id") @LogMeContext Long id,
                                 @RequestParam("data") @LogMeContext String data,
                                 @RequestParam(value = "title", required = false) @LogMeContext String title) {
        if(log.isDebugEnabled()){
            log.debug("API-EDITINVOICEINFOS-START param: id [{}] data [{}] title [{}]",id,data,title);
        }
        Boolean result = orderReadLogic.isShipmentCreatedForShopOrder(id);
        if (!result) {
            throw new JsonResponseException("shipment.exist.can.not.edit.sku.code");
        }
        Map<String, String> invoiceMap = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, String.class, String.class));
        if (invoiceMap == null) {
            log.error("failed to parse invoiceMap:{}", data);
            throw new JsonResponseException("invoice.map.invalid");
        }
        Response<Boolean> response = middleOrderWriteService.updateInvoices(id, invoiceMap, title);
        if (!response.isSuccess()) {
            log.error("failed to edit invoiceMap:{}", data);
            throw new JsonResponseException(response.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EDITINVOICEINFOS-END param: id [{}] data [{}] title [{}]",id,data,title);
        }
    }

    private ExpressCode makeExpressNameByhkCode(String hkExpressCode) {
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        criteria.setHkCode(hkExpressCode);
        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (response.getResult().getData().size() == 0) {
            log.error("there is not any express info by hkCode:{}", hkExpressCode);
            throw new JsonResponseException("express.info.is.not.exist");
        }
        ExpressCode expressCode = response.getResult().getData().get(0);
        return expressCode;
    }

    /**
     * 单个订单生成发货单自动处理逻辑
     *
     * @param shopOrderId
     * @return
     */
    @RequestMapping(value = "/api/order/{id}/auto/handle", method = RequestMethod.PUT)
    @LogMe(description = "单个订单生成发货单", ignore = true)
    public Response<Boolean> autoHandleSingleShopOrder(@PathVariable("id") @LogMeContext Long shopOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-AUTOHANDLESINGLESHOPORDER-START param: shopOrderId [{}] ",shopOrderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Response<String> response = shipmentWiteLogic.autoHandleOrder(shopOrder);
        if (!response.isSuccess()) {
            log.error("auto handle shop order failed, order id is {}", shopOrderId);
            throw new JsonResponseException("生成发货单失败，原因:" + opMessageSources.get(response.getError()));
        }
        if(log.isDebugEnabled()){
            log.debug("API-AUTOHANDLESINGLESHOPORDER-END param: shopOrderId [{}] ",shopOrderId);
        }
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 批量订单生成发货单自动处理逻辑
     *
     * @param ids
     * @return
     */
    @ApiOperation("订单批量处理")
    @RequestMapping(value = "/api/order/batch/auto/handle", method = RequestMethod.PUT)
    @LogMe(description = "批量订单自动处理",ignore = true)
    public Response<Boolean> autoBatchHandleShopOrder(@RequestParam(value = "ids") @LogMeContext List<Long> ids) {
        if(log.isDebugEnabled()){
            log.debug("API-AUTOBATCHHANDLESHOPORDER-START param: ids [{}] ",ids);
        }
        if (Objects.isNull(ids) || ids.isEmpty()) {
            throw new JsonResponseException("shop.order.ids.can.not.be.null");
        }
        List<Long> successShopOrderIds = Lists.newArrayList();
        List<Long> failedShopOrderIds = Lists.newArrayList();
        for (Long shopOrderId : ids) {
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
            try {
                Response<String> response = shipmentWiteLogic.autoHandleOrder(shopOrder);
                if (!response.isSuccess()) {
                    log.error("auto handle shop order failed, order id is {},error:{}", shopOrderId, response.getError());
                    failedShopOrderIds.add(shopOrderId);
                    continue;
                }
            } catch (Exception e) {
                log.error("auto handle order failed,shop order id is {},cause by {}", shopOrder.getOrderCode(), e.getMessage());
                failedShopOrderIds.add(shopOrderId);
                continue;
            }
            successShopOrderIds.add(shopOrderId);
        }

        //订单派单结果校验，未处理成功的记录失败订单列表中
        if(!successShopOrderIds.isEmpty()) {
            Response<List<ShopOrder>> resp = shopOrderReadService.findByIds(successShopOrderIds);
            if (!resp.isSuccess()) {
                log.error("find shop order failed, order ids is {},error:{}", successShopOrderIds.toString(), resp.getError());
            } else {
                resp.getResult().forEach(shopOrder -> {
                    if (!Objects.equals(shopOrder.getHandleStatus(), OrderWaitHandleType.HANDLE_DONE.value())) {
                        failedShopOrderIds.add(shopOrder.getId());
                    }
                });
            }
        }
        List<String> failedShopOrderCodes = Lists.newArrayList();
        failedShopOrderIds.forEach(failedShopOrderId -> {
            failedShopOrderCodes.add("SAL"+failedShopOrderId);
        });

        if (!failedShopOrderIds.isEmpty()) {
            throw new JsonResponseException("订单自动派单完毕， 交易单号：" + failedShopOrderCodes + "自动派单失败，具体原因见订单详情");
        }
        if(log.isDebugEnabled()){
            log.debug("API-AUTOBATCHHANDLESHOPORDER-END param: ids [{}] ",ids);
        }
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 中台客服取消店铺订单
     *
     * @param id 店铺订单id
     * @return
     */
    @RequestMapping(value = "/api/order/shop/{id}/customer/service/cancel", method = RequestMethod.PUT)
    @OperationLogType("中台客服取消店铺订单")
    public Response<Boolean> customerServiceCancelShopOrder(@PathVariable("id") @OperationLogParam Long id, @RequestParam String shopOrderCancelReason) {
        if(log.isDebugEnabled()){
            log.debug("API-CUSTOMERSERVICECANCELSHOPORDER-START param: id [{}] shopOrderCancelReason [{}]",id,shopOrderCancelReason);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
        if (!Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.WAIT_HANDLE.getValue())) {
            throw new JsonResponseException("error.status.can.not.cancel");
        }
        //判断订单是否存在有效的发货单
        if (!orderReadLogic.isShipmentCreatedForShopOrder(shopOrder.getId())) {
            throw new JsonResponseException("shop.order.has.shipment");
        }
        Map<String, String> shopOrderExtra = shopOrder.getExtra();
        shopOrderExtra.put(TradeConstants.SHOP_ORDER_CANCEL_REASON, shopOrderCancelReason);
        try {
            orderWriteService.updateOrderExtra(id, OrderLevel.SHOP, shopOrderExtra);
        } catch (Exception e) {
            log.error("add shop order cancel reason failed,shop order id is {}", id);
        }
        //获取有效子单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrder.getId());
        List<Long> skuIds = skuOrders.stream().filter(Objects::nonNull).filter(skuOrder -> !Objects.equals(skuOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue())).map(SkuOrder::getId).collect(Collectors.toList());
        //更新订单状态
        Response<List<Long>> r = orderWriteService.batchSkuOrderStatusChanged(skuIds, MiddleOrderStatus.WAIT_HANDLE.getValue(), MiddleOrderStatus.CANCEL.getValue());
        if (!r.isSuccess()) {
            log.error("shop order cancel failed,skuOrderId is {},caused by {}", id, r.getError());
            throw new JsonResponseException("cancel.shop.order.failed");
        }
        //释放mpos占用的库存
        orderWriteLogic.releaseRejectShipmentOccupyStock(shopOrder.getId());

        if(log.isDebugEnabled()){
            log.debug("API-CUSTOMERSERVICECANCELSHOPORDER-END param: id [{}] shopOrderCancelReason [{}]",id,shopOrderCancelReason);
        }
        return Response.ok(Boolean.TRUE);


    }

    /**
     * 中台客服取消sku订单(即取消商品)
     *
     * @param id 店铺订单id
     * @return
     */
    @RequestMapping(value = "/api/order/sku/{id}/customer/service/cancel", method = RequestMethod.PUT)
    @OperationLogType("中台客服取消子订单")
    public Response<Boolean> customerServiceCancelSkuOrder(@PathVariable("id") @OperationLogParam Long id, @RequestParam String skuOrderCancelReason) {
        if(log.isDebugEnabled()){
            log.debug("API-CUSTOMERSERVICECANCELSHOPORDER-START param: id [{}] skuOrderCancelReason [{}]",id,skuOrderCancelReason);
        }
        SkuOrder skuOrder = (SkuOrder) orderReadLogic.findOrder(id, OrderLevel.SKU);
        if (!Objects.equals(skuOrder.getStatus(), MiddleOrderStatus.WAIT_HANDLE.getValue())) {
            throw new JsonResponseException("error.status.can.not.cancel");
        }
        Map<String, String> skuOrderExtra = skuOrder.getExtra();
        skuOrderExtra.put(TradeConstants.SKU_ORDER_CANCEL_REASON, skuOrderCancelReason);
        try {
            orderWriteService.updateOrderExtra(id, OrderLevel.SKU, skuOrderExtra);
        } catch (Exception e) {
            log.error("add sku order cancel reason failed,sku order id is {}", id);
        }
        Response<Boolean> r = orderWriteService.skuOrderStatusChanged(id, MiddleOrderStatus.WAIT_HANDLE.getValue(), MiddleOrderStatus.CANCEL.getValue());
        if (!r.isSuccess()) {
            log.error("sku order cancel failed,skuOrderId is {},caused by{}", id, r.getError());
            throw new JsonResponseException("cancel.sku.order.failed");
        }
        //释放mpos占用的库存
        orderWriteLogic.releaseRejectShipmentOccupyStock(skuOrder.getOrderId());

        if(log.isDebugEnabled()){
            log.debug("API-CUSTOMERSERVICECANCELSHOPORDER-END param: id [{}] skuOrderCancelReason [{}]",id,skuOrderCancelReason);
        }
        return r;
    }

    /**
     * 选择快递商
     *
     * @param hkExpressCode
     * @return
     */
    @ApiOperation("选择快递商")
    @LogMe(description = "选择快递商",ignore = true)
    @RequestMapping(value = "/api/order/choose/hk/express/code", method = RequestMethod.PUT)
    public Response<Boolean> chooseExpress(@RequestParam @LogMeContext String hkExpressCode,
                                           @RequestParam @LogMeContext String expressName,
                                           @RequestParam @LogMeContext Long shopOrderId) {
        if(log.isDebugEnabled()){
            log.debug("API-CHOOSEEXPRESS-START param: hkExpressCode [{}] expressName [{}] shopOrderId [{}]",hkExpressCode,expressName,shopOrderId);
        }
        //只有订单处于待处理状态且没有有效的发货单时才可以选择快递
        if (!orderReadLogic.isShipmentCreatedForShopOrder(shopOrderId)) {
            throw new JsonResponseException("shipment.exist.can.not.edit.express.code");
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Map<String, String> extraMap = shopOrder.getExtra();
        extraMap.put(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE, hkExpressCode);
        extraMap.put(TradeConstants.SHOP_ORDER_HK_EXPRESS_NAME, expressName);
        Response<Boolean> rltRes = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, extraMap);
        if (!rltRes.isSuccess()) {
            log.error("update shopOrder：{} extra map to:{} fail,error:{}", shopOrder.getId(), extraMap, rltRes.getError());
            throw new JsonResponseException("add.shop.express.code.fail");
        }
        if(log.isDebugEnabled()){
            log.debug("API-CHOOSEEXPRESS-END param: hkExpressCode [{}] expressName [{}] shopOrderId [{}]",hkExpressCode,expressName,shopOrderId);
        }
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 根据订单好查询售后单信息
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/api/order/{id}/refunds/info", method = RequestMethod.GET)
    public Response<List<Refund>> findRefundsByOrderId(@PathVariable("id") Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-FINDREFUNDSBYORDERID-START param: id [{}] ",id);
        }
        Response<List<Refund>> r = refundReadService.findByOrderIdAndOrderLevel(id, OrderLevel.SHOP);
        if(log.isDebugEnabled()){
            log.debug("API-FINDREFUNDSBYORDERID-END param: id [{}] ",id);
        }
        if (r.getResult().isEmpty()) {
            return r;
        } else {
            List<Refund> refunds = r.getResult().stream().filter(refund -> !Objects.equals(refund.getRefundType(), MiddleRefundType.ON_SALES_REFUND.value())).collect(Collectors.toList());
            return Response.ok(refunds);
        }

    }
    /**
     *
     * 正常订单占用库存发货单确认后同步mpos或者yyedi
     * @return
     */
    @RequestMapping(value = "/api/order/{orderId}/occupy/shipment/confirm",method = RequestMethod.PUT)
    @OperationLogType("备注订单占库发货单确认")
    public Response<Boolean> confirmOrderOccupyShipments(@PathVariable("orderId") @OperationLogParam Long orderId){
        if (log.isDebugEnabled()){
            log.debug("confirm order occupy shipments start,shopOrderId {}",orderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderId);
        List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(orderId);
        for (Shipment shipment:shipments){
            //修改发货单类型，并且同步订单派发中心或者mpos
            shipmentWiteLogic.updateOccupyShipmentTypeByShipmentId(shipment.getId(),ShipmentOccupyType.SALE_N.name());
            shipmentWiteLogic.syncExchangeShipment(shipment.getId());
        }
        //确认订单之后，将未处理状态修改已经处理
        shipmentWiteLogic.updateShipmentNote(shopOrder,OrderWaitHandleType.HANDLE_DONE.value());
        return Response.ok(Boolean.TRUE);
    }


    /**
     *
     * 正常订单占用库存发货单批量确认后同步mpos或者yyedi
     * @return
     */
    @ApiOperation("占库发货单批量确认")
    @RequestMapping(value = "/api/order/batch/occupy/shipment/confirm",method = RequestMethod.PUT)
    @LogMe(description = "占库发货单批量确认",ignore = true)
    public Response<Boolean> batchonfirmOrderOccupyShipments(@RequestParam(value = "ids") @LogMeContext List<Long> ids){
        if (log.isDebugEnabled()){
            log.debug("confirm order occupy shipments start,shopOrderIds {}",ids);
        }
        if (Objects.isNull(ids) || ids.isEmpty()) {
            throw new JsonResponseException("order.ids.can.not.be.null");
        }
        for (Long shopOrderId:ids){
            try {
                ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
                List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrderId);
                for (Shipment shipment:shipments){
                    //修改发货单类型，并且同步订单派发中心或者mpos
                    shipmentWiteLogic.updateOccupyShipmentTypeByShipmentId(shipment.getId(),ShipmentOccupyType.SALE_N.name());
                    shipmentWiteLogic.syncExchangeShipment(shipment.getId());
                }
                //确认订单之后，将未处理状态修改已经处理
                shipmentWiteLogic.updateShipmentNote(shopOrder,OrderWaitHandleType.HANDLE_DONE.value());
            }catch (Exception e){
                log.error("batch confirm order occupy shipments failed,shopOrderId {},caused by {}",
                        shopOrderId,Throwables.getStackTraceAsString(e));
            }
        }
        if (log.isDebugEnabled()){
            log.debug("confirm order occupy shipments end,shopOrderIds {}",ids);
        }
        return Response.ok(Boolean.TRUE);
    }


    /**
     * 创建订单
     *
     * @param openFullOrderInfo
     * @return
     */
    @RequestMapping(value = "/api/order/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
    @LogMe(description = "创建订单",ignore = true)
    public Response<Boolean> createMiddleOrder(@RequestBody @LogMeContext OpenFullOrderInfo openFullOrderInfo) {
        if(log.isDebugEnabled()){
                log.debug("API-CREATEMIDDLEORDER-START param: openFullOrderInfo [{}] ",openFullOrderInfo);
        }
        try {
            openFullOrderInfo.getOrder().setStatus(1);
            Response<Boolean> response = openClientOrderLogic.createOrder(openFullOrderInfo);
            if(log.isDebugEnabled()){
                log.debug("API-CREATEMIDDLEORDER-END param: openFullOrderInfo [{}] ,resp: [{}]",openFullOrderInfo,response);
            }
            return response;
        } catch (Exception e) {
            log.error("create middle orderr failed,caused by {}", Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 导入订单
     *
     * @param file
     * @return
     */
    @RequestMapping(value = "/api/order/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
    public Response<Boolean> importMiddleOrder(MultipartFile file) {
        try {
            Set<String> allowExts = Sets.newHashSet("xlsx");

            String fileExtName = getExtName(file);
            if (!allowExts.contains(fileExtName)) {
                throw new JsonResponseException("文件格式不正确, 请上传正确的文件");
            }
            //解析文件
            List<MiddleOrderInfo> orderInfos = HandlerFileUtil.getInstance().handlerExcelOrder(file.getInputStream());

            if (log.isDebugEnabled()){
                log.debug("OrderWriteLogic importMiddleOrder,orderInfos {}",orderInfos);
            }
            List<OpenFullOrderInfo> openFullOrderInfos = orderWriteLogic.groupByMiddleOrderInfo(orderInfos);

            return openClientOrderLogic.batchCreateOrder(openFullOrderInfos);
        } catch (Exception e) {
            log.error("create middle orderr failed,caused by {}", Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    @GetMapping("/api/export/order/template")
    public ResponseEntity<byte[]> exportTmp(HttpServletResponse response, HttpServletRequest request, @RequestParam(required = false, defaultValue = "excel") String fileType) {
        String fileName = filePath;
        fileName += "/订单规则模板";
        if (Objects.equals("csv", fileType)) {
            fileName = fileName + ".csv";
            HandlerFileUtil.getInstance().writerCsv(com.google.common.collect.Lists.newArrayList(makeMiddleOrderInfo(), makeMiddleOrderInfoRule()), fileName);
        } else {
            fileName = fileName + ".xlsx";
            HandlerFileUtil.getInstance().writerUserExcel(com.google.common.collect.Lists.newArrayList(makeMiddleOrderInfo(), makeMiddleOrderInfoRule()), fileName);
        }

        return downLoadFile(fileName);

    }

    private String getExtName(MultipartFile file) {
        log.info("file name ={}", file.getOriginalFilename());
        String fullName = file.getOriginalFilename().toLowerCase();
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private ResponseEntity<byte[]> downLoadFile(String fileName) {
        File file = new File(fileName);
        byte[] body = null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            body = new byte[is.available()];
            is.read(body);
            HttpHeaders headers = new HttpHeaders();
            String name = file.getAbsoluteFile().getName();
            headers.setContentDispositionFormData("attachment", name, Charset.forName("UTF-8"));
            HttpStatus statusCode = HttpStatus.OK;
            ResponseEntity<byte[]> entity = new ResponseEntity<byte[]>(body, headers, statusCode);
            return entity;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private MiddleOrderInfo makeMiddleOrderInfo() {
        MiddleOrderInfo middleOrderInfo = new MiddleOrderInfo();
        middleOrderInfo.setOutOrderId("外部订单号");
        middleOrderInfo.setChannel("订单来源渠道");
        middleOrderInfo.setBuyerName("买家名称");
        middleOrderInfo.setBuyerMobile("买家手机号");
        middleOrderInfo.setReceiveUserName("收货人姓名");
        middleOrderInfo.setMobile("收货人手机号");
        middleOrderInfo.setProvince("省份");
        middleOrderInfo.setCity("城市");
        middleOrderInfo.setRegion("区县");
        middleOrderInfo.setDetail("地址详情");
//        middleOrderInfo.setShopId("订单来源店铺id")
        middleOrderInfo.setShopName("订单来源店铺名称");
        middleOrderInfo.setOrderHkExpressName("订单发货物流公司名称");
//        middleOrderInfo.setOrderExpressCode("订单发货物流公司编码")
//        middleOrderInfo.setOriginShipFee("原始运费")
        middleOrderInfo.setShipFee("优惠后运费");
        middleOrderInfo.setFee("订单优惠后金额");
        middleOrderInfo.setOrderOriginFee("订单优惠前总价");
        middleOrderInfo.setDiscount("订单优惠");
        middleOrderInfo.setPayType("支付类型");
        middleOrderInfo.setPaymentChannelName("支付渠道名");
        middleOrderInfo.setPaymentSerialNo("支付流水号");
        middleOrderInfo.setCreatedAt("订单创建时间");
        middleOrderInfo.setInvoiceType("发票类型");
        middleOrderInfo.setTitleType("抬头类型");
        middleOrderInfo.setTaxRegisterNo("统一信用代码/税号");
        middleOrderInfo.setCompanyName("公司名称");
        middleOrderInfo.setRegisterPhone("收票人电话");
        middleOrderInfo.setRegisterAddress("注册地址");
        middleOrderInfo.setRegisterBank("开户行");
        middleOrderInfo.setBankAccount("开户行账户");
        middleOrderInfo.setEmail("邮箱");
        middleOrderInfo.setSellerRemark("客服备注");
        middleOrderInfo.setBuyerNote("买家备注");
        middleOrderInfo.setOutSkuOrderId("子订单号");
        middleOrderInfo.setSkuCode("货品条码");
        middleOrderInfo.setItemId("电商商品id");
        middleOrderInfo.setItemName("商品名称");
        middleOrderInfo.setQuantity("数量");
        middleOrderInfo.setOriginFee("销售价格");
        middleOrderInfo.setItemDiscount("商品优惠");
        return middleOrderInfo;
    }

    /**
     * 订单导入模板填写规则
     */
    private MiddleOrderInfo makeMiddleOrderInfoRule() {
        MiddleOrderInfo middleOrderInfo = new MiddleOrderInfo();
        middleOrderInfo.setOutOrderId("1 必填\n" +
                "2 不可重复。\n" +
                "3 如果一次导入文件中有多个相同(外部单号+来源渠道)则认为是一个订单 \n" +
                "4 数据出现不一致以导入订单相同订单号的第一条为准 \n" +
                "5 数据第一行为提示行，不计入导入数据 \n" +
                "6 单元格格式设置成文本格式、不要科学计数法 \n" +
                "7 支持文件为 xlsx文件");
        middleOrderInfo.setChannel("1 必填\n" +
                "2 系统包含的渠道：官网、天猫、京东、苏宁、分期乐、淘宝\n" +
                "3 以导入订单相同订单号的第一条为准");
        middleOrderInfo.setBuyerName("1 必填\n" +
                "2 以导入订单相同订单号的第一条为准");
        middleOrderInfo.setBuyerMobile("1 必填\n" +
                "2 以导入订单相同订单号的第一条为准");
        middleOrderInfo.setReceiveUserName("1 必填 收货人姓名");
        middleOrderInfo.setMobile("1 必填 收货人手机号");
        middleOrderInfo.setProvince("1 必填\n" +
                "2 系统存在的省份\n" +
                "3 以导入订单相同订单号的第一条为准");
        middleOrderInfo.setCity("1 必填\n" +
                "2 系统存在的市\n" +
                "3 以导入订单相同订单号的第一条为准");
        middleOrderInfo.setRegion("1 必填\n" +
                "2 系统存在的区\n" +
                "3 以导入订单相同订单号的第一条为准");
        middleOrderInfo.setDetail("1 必填\n" +
                "2 以导入订单相同订单号的第一条为准");
//        middleOrderInfo.setShopId("订单来源店铺id")
        middleOrderInfo.setShopName("1 必填\n" +
                "2 系统存在的店铺名称\n" +
                "3 以导入订单相同订单号的第一条为准\n" +
                "店铺必须是存在的");
        middleOrderInfo.setOrderHkExpressName("1 必填");
//        middleOrderInfo.setOrderExpressCode("订单发货物流公司编码")
//        middleOrderInfo.setOriginShipFee("1 必填 单位为分 如填写10010 代表100.10元")
        middleOrderInfo.setShipFee("1 必填 单位为元 如填写100.11 代表100.11元 \n" +
                "2 精度必须小于等于2位");
        middleOrderInfo.setFee("1 必填 单位为元 如填写100.11 代表100.11元 \n" +
                "2 订单实付金额为 订单原价+实际运费-订单总优惠 校验使用 \n" +
                "3 精度必须小于等于2位");
        middleOrderInfo.setOrderOriginFee("1 必填 单位为元 如填写100.11 代表100.11元 \n" +
                "2 订单原价为子订单商品原价之和 校验使用 \n" +
                "3 精度必须小于等于2位");
        middleOrderInfo.setDiscount("1 必填 单位为元 如填写100.11 代表100.11元 \n" +
                "2 订单优惠、最后 订单总优惠=订单优惠+子订单优惠" +
                "3 精度必须小于等于2位");
        middleOrderInfo.setPayType("1 选填\n" +
                "2 如果填写必须是系统存在的支付方式：在线支付、货到付款");
        middleOrderInfo.setPaymentChannelName("1 选填\n" +
                "2 如果填写必须是系统存在的支付渠道：支付宝、微信支付、京东支付、网银支付");
        middleOrderInfo.setPaymentSerialNo("1 选填");
        middleOrderInfo.setCreatedAt("1 必填\n" +
                "2 精确到日期、时分秒 格式：20180330172405");
        middleOrderInfo.setInvoiceType("1 选填，如果没有则表示不开发票\n" +
                "2 如果填写则必须是：普通发票、增值税发票、电子发票");
        middleOrderInfo.setTitleType("1 选填，如果填写了发票类型则必填\n" +
                "2 抬头类型：公司、个人");
        middleOrderInfo.setTaxRegisterNo("1 抬头类型为公司则必填");
        middleOrderInfo.setCompanyName("1 抬头类型为公司则必填");
        middleOrderInfo.setRegisterPhone("1 增值税发票则必填 \n" +
                "2电子发票抬头为必填");
        middleOrderInfo.setRegisterAddress("1 增值税发票则必填");
        middleOrderInfo.setRegisterBank("1 增值税发票则必填");
        middleOrderInfo.setBankAccount("1 增值税发票则必填");
        middleOrderInfo.setEmail("1 电子发票必填");
        middleOrderInfo.setSellerRemark("1 选填");
        middleOrderInfo.setBuyerNote("1 选填");
        middleOrderInfo.setOutSkuOrderId("1 必填，同一订单号内不可重复");
        middleOrderInfo.setSkuCode("1 必填，同一订单号内不可重复");
        middleOrderInfo.setItemId("1 选填，同一订单号内不可重复");
        middleOrderInfo.setItemName("1 选填");
        middleOrderInfo.setQuantity("1 必填");
        middleOrderInfo.setOriginFee("1 必填 单位为元 如填写100.11 代表100.11元 \n" +
                "2 单位为单个商品原价 \n " +
                "3 精度必须小于等于2位");
        middleOrderInfo.setItemDiscount("1 必填 单位为元 如填写100.11 代表100.11元 \n" +
                "3 精度必须小于等于2位");
        return middleOrderInfo;
    }

    /**
     * 修复订单数据
     * @param shopId
     * @return
     */
    @RequestMapping(value = "/api/order/{shopId}/update/amount",method = RequestMethod.GET)
    public void updateOrderAmount(@PathVariable("shopId") Long shopId){
        orderWriteLogic.updateOrderAmount(shopId);
    }

    /**
     * 修复订单数据,根据订单id
     * @param shopId
     * @return
     */
    @RequestMapping(value = "/api/order/{shopId}/update/amount/by/order/id",method = RequestMethod.GET)
    public void updateOrderAmountByOrderId(@PathVariable("shopId") Long shopId,@RequestParam("shopOrderId")Long shopOrderId){
        orderWriteLogic.updateOrderAmountByOrderId(shopId,shopOrderId,null);
    }

    /**
     * 京东云鼎修复订单数据
     * @param shopId
     * @return
     */
    @RequestMapping(value = "/api/jd/yunding/order/{shopId}/update/amount",method = RequestMethod.GET)
    public void updateJdYunDingOrderAmount(@PathVariable("shopId") Long shopId){
        orderWriteLogic.updateJdYunDingOrderAmount(shopId);
    }
    /**
     * 修复订单数据,根据订单id
     * @param shopId
     * @return
     */
    @RequestMapping(value = "/api/jd/yunding/order/{shopId}/update/amount/by/order/id",method = RequestMethod.GET)
    public void updateJdYundingOrderAmountByOrderId(@PathVariable("shopId") Long shopId,@RequestParam("shopOrderId")Long shopOrderId){
        orderWriteLogic.updateJdYundingOrderAmountByOrderId(shopId,shopOrderId);
    }
}
