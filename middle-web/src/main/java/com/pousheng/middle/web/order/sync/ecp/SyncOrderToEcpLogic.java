package com.pousheng.middle.web.order.sync.ecp;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.component.SycYunJuShipmentOrderApi;
import com.pousheng.middle.hksyc.component.SyncYunJuJitShipmentApi;
import com.pousheng.middle.hksyc.dto.LogisticsInfo;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.hksyc.dto.YJSyncShipmentRequest;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.order.dto.OpenClientOrderShipment;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 同步电商发货单逻辑
 * Created by tony on 2017/7/5.
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncOrderToEcpLogic {
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private OrderServiceCenter orderServiceCenter;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private SkuOrderReadService skuOrderReadService;
    @Autowired
    private SycYunJuShipmentOrderApi sycYunJuShipmentOrderApi;

    @Autowired
    private SyncYunJuJitShipmentApi syncYunJuJitShipmentApi;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    private static String ERROR_10300009 = "errorCode:10300009";

    /**
     * 同步发货单到电商--只需要传送第一个发货的发货单
     *
     * @param shopOrder         店铺订单
     * @param expressCompayCode 物流公司好
     * @param shipmentId        发货单主键
     * @return
     */
    public Response<Boolean> syncOrderToECP(ShopOrder shopOrder, String expressCompayCode, Long shipmentId) {
        try {
            //获取ecpOrderStatus
            String status = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
            //只有待同步电商平台或者是同步电商失败的才可以同步到苏宁京东
            if (Objects.equals(status, String.valueOf(EcpOrderStatus.SHIPPED_WAIT_SYNC_ECP.getValue())) || Objects.equals(status, String.valueOf(EcpOrderStatus.SYNC_ECP_FAIL.getValue()))) {
                Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);
                OpenClientOrderShipment orderShipment = new OpenClientOrderShipment();
                orderShipment.setOuterOrderId(shopOrder.getOutId());
                orderShipment.setLogisticsCompany(expressCompayCode);
                //填写运单号
                String shipmentSerialNo = StringUtils.isEmpty(shipmentExtra.getShipmentSerialNo())?"":Splitter.on(",").omitEmptyStrings().trimResults().splitToList(shipmentExtra.getShipmentSerialNo()).get(0);
                orderShipment.setWaybill(shipmentSerialNo);
                //目前苏宁需要传入商品编码
                List<String> outSkuCodes = Lists.newArrayList();
                if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.SUNING.getValue())) {
                    for (ShipmentItem shipmentItem : shipmentItems) {
                        outSkuCodes.add(shipmentItem.getOutSkuCode());
                    }
                    orderShipment.setOuterSkuCodes(outSkuCodes);
                }
                Response<Boolean> response = orderServiceCenter.ship(shopOrder.getShopId(), orderShipment);
                if (response.isSuccess()) {
                    //同步成功
                    OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                    orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
                } else {
                    //同步失败
                    OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                    orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                    if (response.getError().contains(ERROR_10300009) && Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())) {
                        response.setError("快递单号:" + shipmentSerialNo + "没有回传至JD青龙系统，需要联系物流商");
                    }
                    return Response.fail(response.getError());
                }
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), Throwables.getStackTraceAsString(e));
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
            return Response.fail(e.getMessage());
        }

        return Response.ok(Boolean.TRUE);
    }


    /**
     * 同步发货单到电商，需要上传所有发货单到电商，天猫是子单发货，如果一个发货单中只有赠品，该发货单不会被同步到电商平台，就算已经同步成功
     * @param shopOrder 店铺订单
     * @return
     */
    public Response<Boolean> syncShipmentsToEcp(ShopOrder shopOrder) {

        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);

            List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrder.getId());
            List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                    .filter(it -> !Objects.equals(MiddleShipmentsStatus.CANCELED.getValue(), it.getStatus()) && !Objects.equals(MiddleShipmentsStatus.REJECTED.getValue(), it.getStatus())).collect(Collectors.toList());
            int count = 0;//判断是否存在同步淘宝失败的发货单
            for (OrderShipment orderShipment : orderShipmentsFilter) {
                Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(orderShipment.getShipmentId()));
                //如果发货单的发货状态不是已发货的，跳过
                if (!Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue())) {
                    continue;
                }
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                try {
                    //获取当前同步淘宝的状态
                    Integer syncTaobaoStatus = shipmentExtra.getSyncTaobaoStatus() == null ? SyncTaobaoStatus.WAIT_SYNC_TAOBAO.getValue()
                            : shipmentExtra.getSyncTaobaoStatus();
                    //如果存在已经同步淘宝成功的发货单,则跳过
                    if (Objects.equals(syncTaobaoStatus, SyncTaobaoStatus.SYNC_TAOBAO_SUCCESS.getValue())) {
                        continue;
                    }
                    shipmentExtra.setSyncTaobaoStatus(syncTaobaoStatus);
                    //获取快递信息
                    ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
                    String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);

                    OpenClientOrderShipment openClientOrderShipment = new OpenClientOrderShipment();
                    openClientOrderShipment.setOuterOrderId(shopOrder.getOutId());
                    openClientOrderShipment.setLogisticsCompany(expressCompanyCode);
                    List<String> outerItemOrderIds = Lists.newArrayList();
                    List<String> outerSkuCodes = Lists.newArrayList();
                    //添加外部子订单的id
                    for (ShipmentItem shipmentItem : shipmentItems) {
                        //淘宝会用到外部sku订单号
                        if (!StringUtils.isEmpty(shipmentItem.getSkuOutId())) {
                            outerItemOrderIds.add(shipmentItem.getSkuOutId());
                        }
                        //官网会用到skuCode
                        outerSkuCodes.add(shipmentItem.getSkuCode());
                    }
                    
                    if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.SUNING.getValue())) {
                        for (ShipmentItem shipmentItem : shipmentItems) {
                            outerSkuCodes.add(shipmentItem.getOutSkuCode());
                        }
                    }
                    
                    openClientOrderShipment.setOuterItemOrderIds(outerItemOrderIds);
                    openClientOrderShipment.setOuterSkuCodes(outerSkuCodes);
                    //填写运单号
                    String shipmentSerialNo = StringUtils.isEmpty(shipmentExtra.getShipmentSerialNo())?"":Splitter.on(",").omitEmptyStrings().trimResults().splitToList(shipmentExtra.getShipmentSerialNo()).get(0);
                    openClientOrderShipment.setWaybill(shipmentSerialNo);
                    log.info("ship to ecp,shopOrderId is {},openClientOrderShipment is {}",shopOrder.getId(),openClientOrderShipment);
                    Response<Boolean> response = null;
                    if (!isTaobaoGiftShipmentOnly(shopOrder, shipmentItems)) {
                        log.info("try ship to ecp,shopOrderId is {},openClientOrderShipment is {}",shopOrder.getId(),openClientOrderShipment);
                        response = orderServiceCenter.ship(shopOrder.getShopId(), openClientOrderShipment);
                    } else {
                        response = Response.ok(Boolean.TRUE);
                    }

                    Map<String, String> extraMap = shipment.getExtra();
                    extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
                    shipment.setExtra(extraMap);

                    if (response.isSuccess()) {
                        shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment, MiddleOrderEvent.SYNC_TAOBAO_SUCCESS.toOrderOperation());
                    } else {
                        count++;
                        shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment, MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                    }
                }catch (Exception e){
                    log.error("sync shipment to taobao failed,shipmentId is {},caused by {}",shipment.getId(),Throwables.getStackTraceAsString(e));
                    shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment,MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                    throw new ServiceException(e.getMessage());
                }
            }

            if (count == 0) {
                //同步成功
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            } else {
                //同步失败
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail("sync.ecp.fail");
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), Throwables.getStackTraceAsString(e));
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
            return Response.fail(e.getMessage());
        }

        return Response.ok(Boolean.TRUE);
    }


    /**
     * 判断是否是淘宝的赠品发货单(该发货单中只有淘宝赠品)
     * @param shopOrder
     * @param shipmentItems
     * @return 不是淘宝赠品发货单false,
     */
    public boolean isTaobaoGiftShipmentOnly(ShopOrder shopOrder, List<ShipmentItem> shipmentItems) {
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue()) && shipmentItems.size() == 1) {
            for (ShipmentItem shipmentItem : shipmentItems) {
                if (StringUtils.isEmpty(shipmentItem.getSkuOutId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Response<Boolean> syncToYunJu(ShopOrder shopOrder) {

        try {
            boolean fromJit = MiddleChannel.YUNJUJIT.getValue().equals(shopOrder.getOutFrom());
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);

            List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrder.getId());
            List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                    .filter(it -> !Objects.equals(MiddleShipmentsStatus.CANCELED.getValue(), it.getStatus()) && !Objects.equals(MiddleShipmentsStatus.REJECTED.getValue(), it.getStatus())).collect(Collectors.toList());
            int count = 0;//判断是否存在同步云聚失败的发货单
            // 云聚是order和shipment是一对一

            for (OrderShipment orderShipment : orderShipmentsFilter) {
                Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(orderShipment.getShipmentId()));
                //如果发货单的发货状态不是已发货的，跳过
                if (!Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue())) {
                    continue;
                }
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

                try {
                    //获取当前同步渠道的状态
                    Integer syncStatus = shipmentExtra.getSyncChannelStatus() == null ? SyncChannelStatus.WAIT_SYNC_CHANNEL.getValue()
                            : shipmentExtra.getSyncChannelStatus();
                    //如果存在已经同步渠道成功的发货单,则跳过
                    if (Objects.equals(syncStatus, SyncChannelStatus.SYNC_CHANNE_SUCCESS.getValue())) {
                        continue;
                    }
                    shipmentExtra.setSyncChannelStatus(syncStatus);
                    //获取快递信息
                    ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
                    String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);
                    Map<String,String> expressMapping= Maps.newHashMap();
                    expressMapping.put(shipmentExtra.getShipmentCorpCode(),expressCompanyCode);

                    List<LogisticsInfo> logisiticis = Lists.newArrayList();
                    boolean itemFailed = false;
                    //组装参数
                    for (ShipmentItem shipmentItem : shipmentItems) {

                        LogisticsInfo logisticsInfo = new LogisticsInfo();
                            Response<SkuOrder> skuOrderResponse = skuOrderReadService.findById(shipmentItem.getSkuOrderId());
                        if (!skuOrderResponse.isSuccess()) {
                            itemFailed = true;
                            break;
                        }else{
                            logisticsInfo.setOrder_product_id(skuOrderResponse.getResult().getOutId());//ERP的商品订单Id

                        }
                        // sku_order获取out_id
                        logisticsInfo.setBar_code(shipmentItem.getSkuCode());//sku条码
                        //兼容物流公司取值
                        expressCompanyCode=buildExpressCompanyCode(fromJit,shipmentExtra,shipmentItem,expressMapping,shopOrder.getShopId());
                        logisticsInfo.setLogistics_company_code(expressCompanyCode); //发货公司code

                        //兼容物流单号取值
                        String shipmentSerialNo = buildShipmentSerialNo(fromJit,shipmentExtra,shipmentItem);

                        logisticsInfo.setLogistics_order(shipmentSerialNo);//发货的快递公司单号

                        logisticsInfo.setDelivery_name(shipmentExtra.getWarehouseName());//发货人 经讨论是可以是发货仓
                        logisticsInfo.setDelivery_time(DateFormatUtils.format(shipmentExtra.getShipmentDate(),"yyyy-MM-dd HH:mm:ss"));
                        logisticsInfo.setAmount(shipmentItem.getShipQuantity()==null?shipmentItem.getQuantity():shipmentItem.getShipQuantity()); //实际发货数量

                        //增加jit新增字段
                        logisticsInfo.setArrival_time(convertDateFormat(shipmentExtra.getExpectDate()));
                        logisticsInfo.setDelivery_method(shipmentExtra.getTransportMethodCode());
                        if (!CollectionUtils.isEmpty(shipmentExtra.getBoxNoMap())) {
                            logisticsInfo.setBox_no(shipmentExtra.getBoxNoMap().get(shipmentItem.getSkuCode()));
                        }

                        logisiticis.add(logisticsInfo);
                    }
                    if (itemFailed) {
                        count++;
                        shipmentWiteLogic.updateShipmentSyncChannelStatus(shipment, MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                        break;
                    }


                    YJSyncShipmentRequest syncShipmentRequest = new YJSyncShipmentRequest();

                    syncShipmentRequest.setOrder_sn(shopOrder.getOutId());//订单号 外部订单号
                    syncShipmentRequest.setLogistics_info(logisiticis);

                    Map<String, String> extraMap = shipment.getExtra();
                    extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
                    shipment.setExtra(extraMap);
                    YJRespone yjRespone;
                    if (MiddleChannel.YUNJUJIT.getValue().equals(shopOrder.getOutFrom())) {
                        yjRespone = syncYunJuJitShipmentApi.doSyncShipmentOrder(syncShipmentRequest);
                    } else {
                        yjRespone = sycYunJuShipmentOrderApi.doSyncShipmentOrder(syncShipmentRequest);
                    }
                        // 去调用云聚接口
                        if (yjRespone != null&&0==yjRespone.getError()) { //成功
                            shipmentWiteLogic.updateShipmentSyncChannelStatus(shipment, MiddleOrderEvent.SYNC_TAOBAO_SUCCESS.toOrderOperation());
                        } else {
                            count++;
                            shipmentWiteLogic.updateShipmentSyncChannelStatus(shipment, MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                        }
                } catch (Exception e) {
                    log.error("sync shipment to yunju failed,shipmentId is {}", shipment.getId(), e);
                    shipmentWiteLogic.updateShipmentSyncChannelStatus(shipment, MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                    throw new ServiceException(e.getMessage());
                }
            }
            if (count == 0) {
                //同步成功
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            } else {
                //同步失败
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail("sync.ecp.fail");
            }
        } catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), Throwables.getStackTraceAsString(e));
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
            return Response.fail("sync.ecp.fail");
        }
        return Response.ok(Boolean.TRUE);

    }

    private static String convertDateFormat(String date){
        if (Strings.isNullOrEmpty(date)){
            return "";
        }
        DateTime dateTime=DateTime.parse(date, DateTimeFormat.forPattern("yyyyMMddHHmmss"));
        return dateTime.toString("yyyy-MM-dd HH:mm:ss");
    }

    private String buildShipmentSerialNo(boolean fromJit,ShipmentExtra shipmentExtra,ShipmentItem shipmentItem){
        if (fromJit && shipmentExtra.getShipmentSerialNoMap() != null) {
            return shipmentExtra.getShipmentSerialNoMap().get(shipmentItem.getSkuCode());
        } else {
            return StringUtils.isEmpty(shipmentExtra.getShipmentSerialNo()) ? ""
                : Splitter.on(",").omitEmptyStrings().trimResults().splitToList(
                    shipmentExtra.getShipmentSerialNo()).get(0);
        }
    }

    private String buildExpressCompanyCode(boolean fromJit,ShipmentExtra shipmentExtra,ShipmentItem shipmentItem,Map<String,String> expressMapping,Long shopId){
        //非JIT逻辑
        if (!fromJit) {
            return expressMapping.get(shipmentExtra.getShipmentCorpCode());
        }
        String shipmentCorpCode = shipmentExtra.getShipmentCorpCodeMap().get(shipmentItem.getSkuCode());
        if (org.apache.commons.lang3.StringUtils.isBlank(shipmentCorpCode)) {
            return null;
        }
        if (expressMapping.containsKey(shipmentCorpCode)) {
            return expressMapping.get(shipmentCorpCode);
        }
        //获取快递信息
        ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentCorpCode);
        String expressCompanyCode = orderReadLogic.getExpressCode(shopId, expressCode);
        expressMapping.put(shipmentCorpCode, expressCompanyCode);
        return expressCompanyCode;
    }
}
