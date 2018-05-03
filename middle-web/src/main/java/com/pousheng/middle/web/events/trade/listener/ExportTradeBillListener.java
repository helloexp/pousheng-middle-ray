package com.pousheng.middle.web.events.trade.listener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.web.events.trade.ExportTradeBillEvent;
import com.pousheng.middle.web.export.*;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.utils.export.ExportContext;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.model.Criteria;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by penghui on 2018/2/6
 * 异步处理导出任务
 */
@Slf4j
@Component
public class ExportTradeBillListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @RpcConsumer
    private PaymentReadService paymentReadService;
    @RpcConsumer
    private SpuReadService spuReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ExportService exportService;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @Autowired
    private PoushengSettlementPosReadService poushengSettlementPosReadService;

    private static JsonMapper jsonMapper=JsonMapper.JSON_NON_EMPTY_MAPPER;
    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onExportTradeBill(ExportTradeBillEvent exportTradeBillEvent){
        String type = exportTradeBillEvent.getType();
        Criteria criteria = exportTradeBillEvent.getCriteria();
        Long userId = exportTradeBillEvent.getUserId();
        switch (type){
            case TradeConstants.EXPORT_ORDER :
                MiddleOrderCriteria middleOrderCriteria = (MiddleOrderCriteria) criteria;
                this.orderExport(middleOrderCriteria,userId);
                break;
            case TradeConstants.EXPORT_REFUND :
                MiddleRefundCriteria middleRefundCriteria = (MiddleRefundCriteria)criteria;
                this.refundExport(middleRefundCriteria,userId);
                break;
            case TradeConstants.EXPORT_SHIPMENT :
                OrderShipmentCriteria orderShipmentCriteria = (OrderShipmentCriteria)criteria;
                this.shipmentExport(orderShipmentCriteria,userId);
                break;
            case TradeConstants.EXPORT_POS :
                PoushengSettlementPosCriteria poushengSettlementPosCriteria = (PoushengSettlementPosCriteria)criteria;
                this.exportSettlementPos(poushengSettlementPosCriteria,userId);
                break;
            default:
                break;

        }
    }

    private void orderExport(MiddleOrderCriteria middleOrderCriteria,Long userId){

        if (middleOrderCriteria.getOutCreatedEndAt() != null) {
            middleOrderCriteria.setOutCreatedEndAt(new DateTime(middleOrderCriteria.getOutCreatedEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        middleOrderCriteria.setPageSize(500);
        if (StringUtils.isNotEmpty(middleOrderCriteria.getMobile())){
            middleOrderCriteria.setOutBuyerId(middleOrderCriteria.getMobile());
        }
        List<OrderExportEntity> orderExportData = new ArrayList<>();

        int pageNO = 1;
        while (true) {

            middleOrderCriteria.setPageNo(pageNO++);
            //根据查询条件查询总单
            Response<Paging<ShopOrder>> pagingRes = middleOrderReadService.pagingShopOrder(middleOrderCriteria);
            if (!pagingRes.isSuccess()) {
                log.error("get order fail,error:{}", pagingRes.getError());
                throw new JsonResponseException(pagingRes.getError());
            }
            if (pagingRes.getResult().getTotal() == 0)
                throw new JsonResponseException("export.data.empty");

            if (pagingRes.getResult().isEmpty())
                break;


            List<Long> orderIds = pagingRes.getResult().getData().stream().map(ShopOrder::getId).collect(Collectors.toList());
            Response<List<SkuOrder>> skuOrderResponse = skuOrderReadService.findByShopOrderIds(orderIds);
            if (!skuOrderResponse.isSuccess()) {
                log.error("get sku order fail,error:{}", skuOrderResponse.getError());
                throw new JsonResponseException(skuOrderResponse.getError());
            }
            final Map<Long/*ShopOrderId*/, List<SkuOrder>> skuOrders = skuOrderResponse.getResult().stream().collect(Collectors.groupingBy(SkuOrder::getOrderId));


            for (ShopOrder shopOrder : pagingRes.getResult().getData()) {

                Long orderID = shopOrder.getId();

                if (!skuOrders.containsKey(orderID))
                    continue;
                Response<List<Shipment>> shipmentResponse = shipmentReadService.findByOrderIdAndOrderLevel(orderID, OrderLevel.SHOP);
                if (!shipmentResponse.isSuccess()) {
                    log.error("get shipment fail,error:{}", shipmentResponse.getError());
                    throw new JsonResponseException(shipmentResponse.getError());
                }
                Response<List<Payment>> paymentResponse = paymentReadService.findByOrderIdAndOrderLevel(orderID, OrderLevel.SHOP);
                if (!paymentResponse.isSuccess()) {
                    log.error("get payment fail,error:{}", paymentResponse.getError());
                    throw new JsonResponseException(paymentResponse.getError());
                }
                Response<List<ReceiverInfo>> receiverResponse = receiverInfoReadService.findByOrderId(orderID, OrderLevel.SHOP);
                if (!receiverResponse.isSuccess()) {
                    log.error("get receiver fail,error:{}", receiverResponse.getError());
                    throw new JsonResponseException(receiverResponse.getError());
                }

                Optional<Shipment> shipment = shipmentResponse.getResult().stream().filter(
                        s -> s.getStatus().equals(MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                                || s.getStatus().equals(MiddleShipmentsStatus.SHIPPED.getValue())).findFirst();


                Optional<Payment> payment = paymentResponse.getResult().stream().filter(p -> (p.getStatus().equals(3))).findAny();

                Optional<ReceiverInfo> receiverInfo = receiverResponse.getResult().stream().findFirst();


                Map<String, Spu> spus = new HashMap<>();
                List<String> skuCodes = skuOrders.get(orderID).stream().map(SkuOrder::getSkuCode).collect(Collectors.toList());
                if (!skuCodes.isEmpty()) {
                    Response<List<SkuTemplate>> skuTemplateResponse = skuTemplateReadService.findBySkuCodes(skuCodes);
                    if (!skuTemplateResponse.isSuccess()) {
                        log.error("get sku template fail,error:{}", skuTemplateResponse.getError());
                        throw new JsonResponseException(skuTemplateResponse.getError());
                    }
                    if (!skuTemplateResponse.getResult().isEmpty()) {
                        for (SkuTemplate skuTemplate : skuTemplateResponse.getResult()) {
                            Response<Spu> spuResponse = spuReadService.findById(skuTemplate.getSpuId());
                            if (!spuResponse.isSuccess()) {
                                log.error("get item fail,error:{}", spuResponse.getError());
                                throw new JsonResponseException(spuResponse.getError());
                            }
                            if (spuResponse.getResult() != null)
                                spus.put(skuTemplate.getSkuCode(), spuResponse.getResult());
                        }
                    }
                }

                ArrayList<String> querySkuCodes = Lists.newArrayList();
                skuOrders.get(orderID).forEach(skuOrder -> {
                    OrderExportEntity export = new OrderExportEntity();
                    export.setOrderID(skuOrder.getOrderId());
                    export.setShopName(skuOrder.getShopName());
                    shipment.ifPresent(s -> {
                        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(s);
                        export.setShipmentCorpName(shipmentExtra.getShipmentCorpName());
                        export.setCarrNo(shipmentExtra.getShipmentSerialNo());
                    });
                    receiverInfo.ifPresent(receiver -> {
                        String address = this.getAddress(receiver);
                        export.setReciverAddress(address);
                        export.setReciverName(receiver.getReceiveUserName());
                        export.setPhone(receiver.getMobile());
                    });
                    //TODO paytype enum
                    export.setPayType("在线支付");
                    //恒康的绩效店铺代码
                    if (shopOrder.getExtra()!=null){
                        export.setPerformanceShopCode(shopOrder.getExtra().get(TradeConstants.ERP_PERFORMANCE_SHOP_CODE)!=null?shopOrder.getExtra().get(TradeConstants.ERP_PERFORMANCE_SHOP_CODE):"");
                    }else{
                        export.setPerformanceShopCode("");
                    }
                    export.setOutId(shopOrder.getOutId());
                    export.setPaymentDate(shopOrder.getOutCreatedAt());
                    export.setOrderStatus(MiddleOrderStatus.fromInt(skuOrder.getStatus()).getName());
                    export.setOrderMemo(shopOrder.getBuyerNote());
                    export.setShipFee(null == shopOrder.getShipFee() ? null : new BigDecimal(shopOrder.getShipFee()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    //TODO 发票信息待完善
                    export.setInvoice("");
                    //TODO 货号可能是其他字段
                    export.setMaterialCode(getMaterialCode(skuOrder.getSkuCode(),querySkuCodes));
                    export.setItemNo(Optional.ofNullable(skuOrder.getSkuCode()).orElse("")); //货品条码
                    if (null != skuOrder.getSkuAttrs()) {
                        skuOrder.getSkuAttrs().forEach(attr -> {
                            switch (attr.getAttrKey()) {
                                case "颜色":
                                    export.setColor(attr.getAttrVal());
                                    break;
                                case "尺码":
                                    export.setSize(attr.getAttrVal());
                                    break;
                            }
                        });
                    }

                    if (spus.containsKey(skuOrder.getSkuCode()))
                        export.setBrandName(spus.get(skuOrder.getSkuCode()).getBrandName());
                    export.setSkuQuantity(skuOrder.getQuantity());
                    if (null != skuOrder.getFee())
                        export.setFee(new BigDecimal(skuOrder.getFee()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());//从分转换成元
                    orderExportData.add(export);
                });

            }
        }

        exportService.saveToDiskAndCloud(new ExportContext(orderExportData),userId);
    }

    @NotNull
    private String getAddress(ReceiverInfo receiver) {
        StringBuffer addressBuffer = new StringBuffer();
        if (StringUtils.isNotEmpty(receiver.getProvince())) {
            addressBuffer.append(receiver.getProvince());
        }
        if (StringUtils.isNotEmpty(receiver.getCity())) {
            addressBuffer.append(receiver.getCity());
        }
        if (StringUtils.isNotEmpty(receiver.getRegion())) {
            addressBuffer.append(receiver.getRegion());
        }
        if (StringUtils.isNotEmpty(receiver.getStreet())) {
            addressBuffer.append(receiver.getStreet());
        }
        if (StringUtils.isNotEmpty(receiver.getDetail())) {
            addressBuffer.append(receiver.getDetail());
        }
        return addressBuffer.toString();
    }

    private void refundExport(MiddleRefundCriteria criteria,Long userId) {

        criteria.setExcludeRefundType(MiddleRefundType.ON_SALES_REFUND.value());
        criteria.setSize(500);
        if (criteria.getRefundEndAt() != null) {
            criteria.setRefundEndAt(new DateTime(criteria.getRefundEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        List<RefundExportEntity> refundExportData = new ArrayList<>();

        int pageNO = 1;
        while (true) {
            criteria.setPageNo(pageNO++);
            Response<Paging<RefundPaging>> pagingResponse = refundReadLogic.refundPaging(criteria);
            if (!pagingResponse.isSuccess()) {
                throw new JsonResponseException(pagingResponse.getError());
            }
            if (pagingResponse.getResult().getTotal() == 0)
                throw new JsonResponseException("export.data.empty");

            if (pagingResponse.getResult().isEmpty()) {
                break;
            }
            pagingResponse.getResult().getData().forEach(refundInfo -> {

                boolean dirtyData = false;
                List<RefundItem> refundItems = null;
                try {
                    refundItems = refundReadLogic.findRefundItems(refundInfo.getRefund());
                } catch (JsonResponseException e) {
                    if (e.getMessage().equals("refund.exit.not.contain.item.info"))
                        dirtyData = true;
                }
                if (!dirtyData) {
                    if (null == refundInfo.getRefund())
                        throw new JsonResponseException("order.refund.find.fail");
                    if (null == refundInfo.getOrderRefund())
                        throw new JsonResponseException("order.refund.find.fail");

                    RefundExtra refundExtra = refundReadLogic.findRefundExtra(refundInfo.getRefund());

                    Map<String, Spu> spus = new HashMap<>();
                    List<String> skuCodes = refundItems.stream().map(RefundItem::getSkuCode).collect(Collectors.toList());
                    if (!skuCodes.isEmpty()) {
                        Response<List<SkuTemplate>> skuTemplateResponse = skuTemplateReadService.findBySkuCodes(skuCodes);
                        if (!skuTemplateResponse.isSuccess()) {
                            log.error("get sku template fail,error:{}", skuTemplateResponse.getError());
                            throw new JsonResponseException(skuTemplateResponse.getError());
                        }
                        if (!skuTemplateResponse.getResult().isEmpty()) {
                            for (SkuTemplate skuTemplate : skuTemplateResponse.getResult()) {
                                Response<Spu> spuResponse = spuReadService.findById(skuTemplate.getSpuId());
                                if (!spuResponse.isSuccess()) {
                                    log.error("get item fail,error:{}", spuResponse.getError());
                                    throw new JsonResponseException(spuResponse.getError());
                                }
                                if (spuResponse.getResult() != null)
                                    spus.put(skuTemplate.getSkuCode(), spuResponse.getResult());
                            }
                        }
                    }
                    ArrayList<String> querySkuCodes = Lists.newArrayList();

                    refundItems.forEach(item -> {
                        RefundExportEntity export = new RefundExportEntity();
                        export.setOrderID(refundInfo.getOrderRefund().getOrderId());
                        export.setShopName(refundInfo.getRefund().getShopName());
                        export.setMemo(refundInfo.getRefund().getBuyerNote());
                        export.setRefundType(MiddleRefundType.from(refundInfo.getRefund().getRefundType()).toString());
                        export.setStatus(MiddleRefundStatus.fromInt(refundInfo.getRefund().getStatus()).getName());

                        export.setAmt(item.getFee() == null ? null : new BigDecimal(item.getFee()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        export.setMaterialCode(getMaterialCode(item.getSkuCode(),querySkuCodes));
                        export.setItemNo(item.getSkuCode());
                        if (StringUtils.isNotBlank(item.getSkuCode()) && spus.containsKey(item.getSkuCode())) {
                            export.setBrand(spus.get(item.getSkuCode()).getBrandName());
                        }
                        if (null != item.getAttrs()) {
                            item.getAttrs().forEach(attr -> {
                                switch (attr.getAttrKey()) {
                                    case "颜色":
                                        export.setColor(attr.getAttrVal());
                                        break;
                                    case "尺码":
                                        export.setSize(attr.getAttrVal());
                                        break;
                                }
                            });
                        }
                        export.setApplyQuantity(item.getAlreadyHandleNumber());
                        if (null != refundExtra.getHkConfirmItemInfos()) {
                            refundExtra.getHkConfirmItemInfos().stream().filter(hkinfo -> hkinfo.getItemCode().equalsIgnoreCase(item.getSkuCode())).findAny().ifPresent(hkinfo -> {
                                export.setActualQuantity(hkinfo.getQuantity());
                            });
                        }
                        export.setWarehousingDate(refundExtra.getHkReturnDoneAt());

                        refundExportData.add(export);
                    });

                }

            });

        }

        exportService.saveToDiskAndCloud(new ExportContext(refundExportData),userId);
    }

    private String getMaterialCode(String skuCode,List<String> querySkuCodes){
        querySkuCodes.clear();
        if (StringUtils.isEmpty(skuCode)){
            return "";//skuCode为空的
        }
        querySkuCodes.add(skuCode);
        Response<List<SkuTemplate>> response = skuTemplateReadService.findBySkuCodes(querySkuCodes);
        if (!response.isSuccess()){
            log.error("get sku template bySkuCode fail ,skuCode={},error:{}",skuCode, response.getError());
            throw new JsonResponseException(response.getError());
        } else {
            return response.getResult().get(0).getExtra().getOrDefault("materialCode","");
        }


    }
    private void shipmentExport(OrderShipmentCriteria criteria,Long userId) {
        //判断查询的发货单类型
        if (Objects.equals(criteria.getType(), ShipmentType.EXCHANGE_SHIP.value())) {
            criteria.setAfterSaleOrderId(criteria.getOrderId());
            criteria.setOrderId(null);
        }

        List<ShipmentExportEntity> shipmentExportEntities = new ArrayList<>();

        int pageNo = 1;

        while (true) {

            criteria.setPageNo(pageNo++);

            Response<Paging<ShipmentPagingInfo>> response = orderShipmentReadService.findBy(criteria);
            if (!response.isSuccess()) {
                log.error("find shipment by criteria:{} fail,error:{}", criteria, response.getError());
                throw new JsonResponseException(response.getError());
            }
            if (response.getResult().getTotal() == 0)
                throw new JsonResponseException("export.data.empty");

            if (response.getResult().getData().isEmpty())
                break;


            response.getResult().getData().forEach(shipmentContext -> {

                Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(shipmentContext.getOrderShipment().getOrderId());
                if (!shopOrderResponse.isSuccess())
                    throw new JsonResponseException(shopOrderResponse.getError());

                Response<List<ReceiverInfo>> receiverResponse = receiverInfoReadService.findByOrderId(shipmentContext.getOrderShipment().getOrderId(), OrderLevel.SHOP);
                if (!receiverResponse.isSuccess()) {
                    log.error("get receiver fail,error:{}", receiverResponse.getError());
                    throw new JsonResponseException(receiverResponse.getError());
                }

                ArrayList<String> querySkuCodes = Lists.newArrayList();

                shipmentReadLogic.getShipmentItems(shipmentContext.getShipment()).forEach(item -> {
                    ShipmentExportEntity entity = new ShipmentExportEntity();

                    ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipmentContext.getShipment());
                   //发货方式
                    if(null!=shipmentContext.getShipment().getShipWay()){
                        String shipWay="";
                        if (shipmentContext.getShipment().getShipWay()==1){
                            shipWay="店发";
                        }
                        if (shipmentContext.getShipment().getShipWay()==2){
                            shipWay="仓发";
                        }
                        //发货方式
                        entity.setShipWay(shipWay);
                    }
                    //发货方
                    entity.setWarehouseName(shipmentExtra.getWarehouseName());
                    entity.setShopName(shipmentContext.getOrderShipment().getShopName());
                    entity.setOrderID(shipmentContext.getOrderShipment().getOrderId());
                    entity.setMaterialCode(getMaterialCode(item.getSkuCode(),querySkuCodes));
                    entity.setItemNo(item.getSkuCode());

                    entity.setShipmentCorpName(shipmentExtra.getShipmentCorpName());
                    entity.setCarrNo(shipmentExtra.getShipmentSerialNo());
                    if (!receiverResponse.getResult().isEmpty()) {
                        entity.setReciverName(receiverResponse.getResult().get(0).getReceiveUserName());
                        entity.setReciverAddress(this.getAddress(receiverResponse.getResult().get(0)));
                        entity.setPhone(receiverResponse.getResult().get(0).getMobile());
                    }
                    //TODO 销售类型
//                entity.setSaleType(shipmentContext.getOrderShipment().getType());
                    entity.setPayType("在线支付");
//                    entity.setInvoice("");
                    entity.setPaymentDate(shopOrderResponse.getResult().getOutCreatedAt());
                    entity.setSkuQuantity(item.getQuantity());
                    if (null == item.getCleanFee())
                        entity.setFee(0D);
                    else
                        entity.setFee(new BigDecimal(item.getCleanFee()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
//                    entity.setShipFee(null == shopOrderResponse.getResult().getShipFee() ? null : new BigDecimal(shopOrderResponse.getResult().getShipFee()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    entity.setOrderMemo(shopOrderResponse.getResult().getBuyerNote());
                    entity.setOrderStatus(MiddleOrderStatus.fromInt(shopOrderResponse.getResult().getStatus()).getName());
                    shipmentExportEntities.add(entity);

                });


            });
        }
        exportService.saveToDiskAndCloud(new ExportContext(shipmentExportEntities),userId);
    }

    private void exportSettlementPos(PoushengSettlementPosCriteria criteria,Long userId){
        List<SettlementPosEntity> posExportEntities = new ArrayList<>();

        criteria.setPageSize(500);
        int pageNo=1;
        while(true){
            criteria.setPageNo(pageNo++);
            Response<Paging<PoushengSettlementPos>> r = poushengSettlementPosReadService.paging(criteria);
            if (!r.isSuccess()) {
                log.error("find settlement pos by criteria:{} fail,error:{}", criteria, r.getError());
                throw new JsonResponseException(r.getError());
            }
            if (r.getResult().getTotal() == 0) {
                throw new JsonResponseException("export.data.empty");
            }

            if (r.getResult().getData().isEmpty()) {
                break;
            }
            r.getResult().getData().forEach(posContext->{
                SettlementPosEntity entity = new  SettlementPosEntity();
                entity.setPosSerialNo(posContext.getPosSerialNo());
                entity.setPosAmt(new BigDecimal(posContext.getPosAmt()==null?0:posContext.getPosAmt()).divide(new BigDecimal(100),2, RoundingMode.HALF_DOWN).toString());
                entity.setShopId(posContext.getShopId());
                entity.setShopName(posContext.getShopName());
                entity.setPosType(posContext.getPosType()==1?"正常销售":"售后");
                entity.setOrderId(posContext.getOrderId());
                entity.setPosCreatedAt(posContext.getPosCreatedAt());
                entity.setCreatedAt(posContext.getCreatedAt());
                posExportEntities.add(entity);
            });
        }
        exportService.saveToDiskAndCloud(new ExportContext(posExportEntities),userId);
    }
}
