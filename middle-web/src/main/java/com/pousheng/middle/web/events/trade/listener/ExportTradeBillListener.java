package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.convert.ReverseHeadlessInfoConvert;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessDto;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.search.dto.StockSendCriteria;
import com.pousheng.middle.search.refund.RefundSearchComponent;
import com.pousheng.middle.web.events.trade.ExportTradeBillEvent;
import com.pousheng.middle.web.events.trade.listener.export.StockSendExport;
import com.pousheng.middle.web.export.*;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ReverseHeadlessInfoLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import com.pousheng.middle.web.utils.export.ExportContext;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientPaymentInfo;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.model.Criteria;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
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
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by penghui on 2018/2/6 异步处理导出任务
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
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundSearchComponent refundSearchComponent;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ExportService exportService;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @Autowired
    private PoushengSettlementPosReadService poushengSettlementPosReadService;

    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private ReverseHeadlessInfoLogic reverseHeadlessInfoLogic;
    @Autowired
    private StockSendExport stockSendExport;

    private static final int SKU_TEMPLATES_AVALIABLE_STATUS = 1;

    @Autowired
    private ShopCacher shopCacher;
    
    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onExportTradeBill(ExportTradeBillEvent exportTradeBillEvent) {
        String type = exportTradeBillEvent.getType();
        Criteria criteria = exportTradeBillEvent.getCriteria();
        Long userId = exportTradeBillEvent.getUserId();
        switch (type) {
            case TradeConstants.EXPORT_ORDER:
                MiddleOrderCriteria middleOrderCriteria = (MiddleOrderCriteria)criteria;
                this.orderExport(middleOrderCriteria, userId);
                break;
            case TradeConstants.EXPORT_REFUND:
                MiddleRefundCriteria middleRefundCriteria = (MiddleRefundCriteria)criteria;
                this.refundExport(middleRefundCriteria, userId);
                break;
            case TradeConstants.EXPORT_SHIPMENT:
                OrderShipmentCriteria orderShipmentCriteria = (OrderShipmentCriteria)criteria;
                this.shipmentExport(orderShipmentCriteria, userId);
                break;
            case TradeConstants.EXPORT_POS:
                PoushengSettlementPosCriteria poushengSettlementPosCriteria = (PoushengSettlementPosCriteria)criteria;
                this.exportSettlementPos(poushengSettlementPosCriteria, userId);
                break;
            case TradeConstants.EXPORT_REVERSE_HEADLESS:
                ReverseHeadlessCriteria reverseHeadlessCriteria = (ReverseHeadlessCriteria)criteria;
                this.exportReverseHeadless(reverseHeadlessCriteria, userId);
                break;
            case TradeConstants.EXPORT_STOCK_SEND:
                StockSendCriteria c = (StockSendCriteria) criteria;
                this.stockSendExport.export(c, userId);
                break;
            default:
                break;
        }
    }

    private void orderExport(MiddleOrderCriteria middleOrderCriteria, Long userId) {

        if (middleOrderCriteria.getOutCreatedEndAt() != null) {
            middleOrderCriteria.setOutCreatedEndAt(new DateTime(middleOrderCriteria.getOutCreatedEndAt().getTime())
                .plusDays(1).minusSeconds(1).toDate());
        }
        middleOrderCriteria.setPageSize(500);
        if (StringUtils.isNotEmpty(middleOrderCriteria.getMobile())) {
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
            if (pagingRes.getResult().getTotal() == 0) {
                throw new JsonResponseException("export.data.empty");
            }

            if (pagingRes.getResult().isEmpty()) {
                break;
            }

            List<Long> orderIds = pagingRes.getResult().getData().stream().map(ShopOrder::getId).collect(
                Collectors.toList());
            Response<List<SkuOrder>> skuOrderResponse = skuOrderReadService.findByShopOrderIds(orderIds);
            if (!skuOrderResponse.isSuccess()) {
                log.error("get sku order fail,error:{}", skuOrderResponse.getError());
                throw new JsonResponseException(skuOrderResponse.getError());
            }
            final Map<Long/*ShopOrderId*/, List<SkuOrder>> skuOrders = skuOrderResponse.getResult().stream().collect(
                Collectors.groupingBy(SkuOrder::getOrderId));

            for (ShopOrder shopOrder : pagingRes.getResult().getData()) {

                Long orderID = shopOrder.getId();

                if (!skuOrders.containsKey(orderID)) {
                    continue;
                }
                Response<List<Shipment>> shipmentResponse = shipmentReadService.findByOrderIdAndOrderLevel(orderID,
                    OrderLevel.SHOP);
                if (!shipmentResponse.isSuccess()) {
                    log.error("get shipment fail,error:{}", shipmentResponse.getError());
                    throw new JsonResponseException(shipmentResponse.getError());
                }
                Response<List<Payment>> paymentResponse = paymentReadService.findByOrderIdAndOrderLevel(orderID,
                    OrderLevel.SHOP);
                if (!paymentResponse.isSuccess()) {
                    log.error("get payment fail,error:{}", paymentResponse.getError());
                    throw new JsonResponseException(paymentResponse.getError());
                }
                Response<List<ReceiverInfo>> receiverResponse = receiverInfoReadService.findByOrderId(orderID,
                    OrderLevel.SHOP);
                if (!receiverResponse.isSuccess()) {
                    log.error("get receiver fail,error:{}", receiverResponse.getError());
                    throw new JsonResponseException(receiverResponse.getError());
                }

                Optional<Shipment> shipment = shipmentResponse.getResult().stream().filter(
                    s -> s.getStatus().equals(MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                        || s.getStatus().equals(MiddleShipmentsStatus.SHIPPED.getValue())).findFirst();

                Optional<Payment> payment = paymentResponse.getResult().stream().filter(p -> (p.getStatus().equals(3)))
                    .findAny();

                Optional<ReceiverInfo> receiverInfo = receiverResponse.getResult().stream().findFirst();

                Map<String, Spu> spus = new HashMap<>();
                List<String> skuCodes = skuOrders.get(orderID).stream().map(SkuOrder::getSkuCode).collect(
                    Collectors.toList());
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
                            if (spuResponse.getResult() != null) {
                                spus.put(skuTemplate.getSkuCode(), spuResponse.getResult());
                            }
                        }
                    }
                }

                ArrayList<String> querySkuCodes = Lists.newArrayList();
                skuOrders.get(orderID).forEach(skuOrder -> {
                    OrderExportEntity export = new OrderExportEntity();
                    export.setOrderCode(shopOrder.getOrderCode());
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
                    export.setPerformanceShopCode(getPerformanceShopCode(shopOrder.getShopId()));

                    export.setOutId(shopOrder.getOutId());
                    export.setPaymentdate(shopOrder.getOutCreatedAt());
                    export.setOrderStatus(MiddleOrderStatus.fromInt(skuOrder.getStatus()).getName());
                    export.setOrderMemo(shopOrder.getBuyerNote());
                    export.setShipFee(null == shopOrder.getShipFee() ? null : new BigDecimal(shopOrder.getShipFee())
                        .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    //TODO 发票信息待完善
                    export.setInvoice("");
                    //TODO 货号可能是其他字段
                    export.setMaterialCode(getMaterialCode(skuOrder.getSkuCode(), querySkuCodes));
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

                    if (spus.containsKey(skuOrder.getSkuCode())) {
                        export.setBrandName(spus.get(skuOrder.getSkuCode()).getBrandName());
                    }
                    export.setSkuQuantity(skuOrder.getQuantity());
                    if (null != skuOrder.getFee()) {
                        export.setFee(new BigDecimal(skuOrder.getFee())
                            .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());//从分转换成元
                    }
                    orderExportData.add(export);
                });

            }
        }

        exportService.saveToDiskAndCloud(new ExportContext(orderExportData), userId);
    }

    @NotNull
    private String getAddress(ReceiverInfo receiver) {
        StringBuilder addressBuffer = new StringBuilder();
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

    private void refundExport(MiddleRefundCriteria criteria, Long userId) {

        criteria.setExcludeRefundType(MiddleRefundType.ON_SALES_REFUND.value());
        criteria.setPageSize(500);
        if (criteria.getRefundEndAt() != null) {
            criteria.setRefundEndAt(new DateTime(criteria.getRefundEndAt().getTime()).plusDays(1).minusSeconds(1)
                .toDate());
        }
        List<RefundExportEntity> refundExportData = new ArrayList<>();

        int pageNO = 1;
        while (true) {
            criteria.setPageNo(pageNO);
            pageNO += 1;

            Paging<RefundPaging> paging = refundSearchComponent.search(criteria);
            if (paging.isEmpty()) {
                break;
            }
            paging.getData().forEach(refundInfo -> {

                boolean dirtyData = false;
                List<RefundItem> refundItems = null;
                try {

                    if (Objects.equals(refundInfo.getRefund().getRefundType(),
                        MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
                        refundItems = refundReadLogic.findRefundLostItems(refundInfo.getRefund());
                    } else {
                        refundItems = refundReadLogic.findRefundItems(refundInfo.getRefund());
                    }
                } catch (JsonResponseException e) {
                    if (e.getMessage().equals("refund.exit.not.contain.item.info")) {
                        dirtyData = true;
                    }
                }
                if (!dirtyData) {
                    if (null == refundInfo.getRefund()) {
                        throw new JsonResponseException("order.refund.find.fail");
                    }
                    if (null == refundInfo.getOrderRefund()) {
                        throw new JsonResponseException("order.refund.find.fail");
                    }

                    RefundExtra refundExtra = refundReadLogic.findRefundExtra(refundInfo.getRefund());

                    Map<String, Spu> spus = new HashMap<>();
                    List<String> skuCodes = refundItems.stream().map(RefundItem::getSkuCode).collect(
                        Collectors.toList());
                    if (!skuCodes.isEmpty()) {
                        Response<List<SkuTemplate>> skuTemplateResponse = skuTemplateReadService.findBySkuCodes(
                            skuCodes);
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
                                if (spuResponse.getResult() != null) {
                                    spus.put(skuTemplate.getSkuCode(), spuResponse.getResult());
                                }
                            }
                        }
                    }
                    //查询关联的交易订单
                    ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(
                        refundInfo.getOrderRefund().getOrderCode());
                    //订单支付信息
                    OpenClientPaymentInfo paymentInfo = orderReadLogic.getOpenClientPaymentInfo(shopOrder);
                    //查询关联的pos单
                    PoushengSettlementPos posSettleMent = null;
                    Response<PoushengSettlementPos> posResponse = poushengSettlementPosReadService
                        .findByRefundCodeAndPosType(refundInfo.getRefund().getRefundCode(), 2);
                    if (posResponse.isSuccess()) {
                        posSettleMent = posResponse.getResult();
                    }

                    ArrayList<String> querySkuCodes = Lists.newArrayList();

                    PoushengSettlementPos finalPosSettleMent = posSettleMent;
                    Refund refund = refundInfo.getRefund();
                    refundItems.forEach(item -> {
                        RefundExportEntity export = new RefundExportEntity();
                        export.setOrderCode(refundInfo.getOrderRefund().getOrderCode());
                        export.setRefundCode(refund.getRefundCode());
                        //export.setRefundSubCode(item.getSkuCode());
                        export.setShopName(refund.getShopName());
                        export.setMemo(refund.getBuyerNote());
                        export.setRefundType(MiddleRefundType.from(refund.getRefundType()).toString());
                        export.setStatus(MiddleRefundStatus.fromInt(refund.getStatus()).getName());
                        export.setAmt(refund.getFee() == null ? null : new BigDecimal(refund.getFee())
                            .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        export.setMaterialCode(getMaterialCode(item.getSkuCode(), querySkuCodes));
                        if (item.getCleanPrice() != null && item.getApplyQuantity() != null) {
                            export.setTotalPrice(new BigDecimal(item.getCleanPrice()).multiply(
                                new BigDecimal(item.getApplyQuantity()))
                                .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        } else {
                            export.setTotalPrice(null);
                        }
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

                        List<YYEdiRefundConfirmItem> confirmItems = refundReadLogic.findRefundYYEdiConfirmItems(
                            refundInfo.getRefund());
                        if (!Objects.isNull(confirmItems)) {
                            confirmItems.stream().filter(
                                confirmItem -> confirmItem.getItemCode().equalsIgnoreCase(item.getSkuCode())).findAny()
                                .ifPresent(confirmItem -> {
                                    //实际数量
                                    export.setActualQuantity(Integer.parseInt(confirmItem.getQuantity()));
                                });
                        }
                        export.setWarehousingDate(refundExtra.getHkReturnDoneAt());
                        //申请数量
                        export.setApplyQuantity(item.getApplyQuantity());
                        //实际数量
                        //export.setActualQuantity(item.getApplyQuantity());
                        export.setOrderCode(refund.getReleOrderCode());
                        export.setShipCode(refundExtra.getShipmentId());
                        export.setOutCode(shopOrder.getOutId());
                        export.setPayOrderCreateDate(shopOrder.getCreatedAt());
                        if (finalPosSettleMent != null) {
                            export.setPosCode(finalPosSettleMent.getPosSerialNo());
                        }
                        if (!Objects.isNull(paymentInfo)) {
                            export.setPayOrderPayDate(paymentInfo.getPaidAt());
                        }
                        export.setAfterSaleCreateDate(refund.getCreatedAt());
                        export.setAfterSaleRefundDate(refund.getUpdatedAt());
                        export.setAfterSaleExpressCompany(refundExtra.getShipmentCorpName());
                        export.setAfterSaleExpressNo(refundExtra.getShipmentSerialNo());

                        refundExportData.add(export);
                    });

                }

            });

        }

        if (refundExportData.isEmpty()) {
            throw new JsonResponseException("export.data.empty");
        }
        exportService.saveToDiskAndCloud(new ExportContext(refundExportData), userId);
    }

    private String getMaterialCode(String skuCode, List<String> querySkuCodes) {
        querySkuCodes.clear();
        if (StringUtils.isEmpty(skuCode)) {
            return "";//skuCode为空的
        }
        querySkuCodes.add(skuCode);
        Response<List<SkuTemplate>> response = skuTemplateReadService.findBySkuCodes(querySkuCodes);
        if (!response.isSuccess()) {
            log.error("get sku template bySkuCode fail ,skuCode={},error:{}", skuCode, response.getError());
            throw new JsonResponseException(response.getError());
        } else {
            Optional<SkuTemplate> tmpSkuTemplate = response.getResult().stream().filter(
                s -> s.getStatus().equals(SKU_TEMPLATES_AVALIABLE_STATUS)).findFirst();
            String materialCode = "";
            if (tmpSkuTemplate.isPresent() && tmpSkuTemplate.get().getExtra() != null) {
                materialCode = tmpSkuTemplate.get().getExtra().getOrDefault("materialCode", "");
            }
            return materialCode;
        }

    }

    private void shipmentExport(OrderShipmentCriteria criteria, Long userId) {
        //判断查询的发货单类型
        if (Objects.equals(criteria.getType(), ShipmentType.EXCHANGE_SHIP.value())) {
            criteria.setAfterSaleOrderId(criteria.getOrderId());
            criteria.setOrderId(null);
        }

        List<ShipmentExportEntity> shipmentExportEntities = new ArrayList<>();

        int pageNo = 1;
        criteria.setPageSize(100);

        while (true) {

            criteria.setPageNo(pageNo++);

            Response<Paging<ShipmentPagingInfo>> response = orderShipmentReadService.findBy(criteria);
            if (!response.isSuccess()) {
                log.error("find shipment by criteria:{} fail,error:{}", criteria, response.getError());
                throw new JsonResponseException(response.getError());
            }
            if (response.getResult().getTotal() == 0) {
                throw new JsonResponseException("export.data.empty");
            }

            if (CollectionUtils.isEmpty(response.getResult().getData())) {
                break;
            }

            //批量查询联系人
            List<Long> orderIds = Lists.newArrayListWithCapacity(response.getResult().getData().size());
            response.getResult().getData().stream().forEach(val -> orderIds.add(val.getOrderShipment().getOrderId()));
            Map<Long, ReceiverInfo> receiverInfoMap = findReceiverInfos(orderIds);

            for (ShipmentPagingInfo shipmentContext : response.getResult().getData()) {

                ShopOrder shopOrder = shipmentContext.getShopOrder();
                if (Objects.isNull(shopOrder)) {
                    log.error("shop.order.not.found.orderId:{}", shipmentContext.getOrderShipment().getOrderId());
                    break;
                }

                ArrayList<String> querySkuCodes = Lists.newArrayList();

                shipmentReadLogic.getShipmentItems(shipmentContext.getShipment()).forEach(item -> {
                    ShipmentExportEntity entity = new ShipmentExportEntity();

                    ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipmentContext.getShipment());
                    //发货方式
                    if (null != shipmentContext.getShipment().getShipWay()) {
                        String shipWay = "";
                        if (shipmentContext.getShipment().getShipWay() == 1) {
                            shipWay = "店发";
                            Shop shop = findShop(shipmentExtra.getWarehouseId());
                            if (!Objects.isNull(shop)) {
                                entity.setShipArea(shop.getZoneName());
                            }
                        }
                        if (shipmentContext.getShipment().getShipWay() == 2) {
                            shipWay = "仓发";
                        }
                        //发货方式
                        entity.setShipWay(shipWay);
                    }
                    //发货方
                    entity.setWarehouseName(shipmentExtra.getWarehouseName());
                    //发货方外码
                    entity.setWarehouseOutCode(shipmentExtra.getWarehouseOutCode());
                    //派单类型
                    if (null != shipmentContext.getShipment().getDispatchType()) {
                        String dispatchType = "";
                        if (shipmentContext.getShipment().getDispatchType() == 1) {
                            dispatchType = "自动派单";
                        }
                        if (shipmentContext.getShipment().getDispatchType() == 2) {
                            dispatchType = "手动派单";
                        }
                        entity.setDispatchType(dispatchType);
                    }
                    entity.setShopName(shipmentContext.getOrderShipment().getShopName());
                    OpenShop serverShop = openShopCacher.findById(shipmentContext.getOrderShipment().getShopId());
                    if (serverShop.getShopName().startsWith("mpos")) {
                        entity.setServerArea(serverShop.getExtra().get("zoneName"));
                    }
                    entity.setOrderCode(shipmentContext.getOrderShipment().getOrderCode());
                    entity.setMaterialCode(getMaterialCode(item.getSkuCode(), querySkuCodes));
                    entity.setShipmenCode(shipmentContext.getShipment().getShipmentCode());
                    entity.setItemNo(item.getSkuCode());
                    entity.setShipmentCorpName(shipmentExtra.getShipmentCorpName());
                    entity.setCarrNo(shipmentExtra.getShipmentSerialNo());
                    entity.setCallbackMailNo(shipmentExtra.getCallbackMailNo());
                    entity.setExpressOrderId(shipmentExtra.getExpressOrderId());
                    ReceiverInfo receiverInfo = receiverInfoMap.get(shipmentContext.getOrderShipment().getOrderId());
                    if (!Objects.isNull(receiverInfo)) {
                        entity.setReciverName(receiverInfo.getReceiveUserName());
                        entity.setReciverAddress(this.getAddress(receiverInfo));
                        entity.setPhone(receiverInfo.getMobile());
                    }
                    //TODO 销售类型
                    //                entity.setSaleType(shipmentContext.getOrderShipment().getType());
                    entity.setPayType("在线支付");
                    //                    entity.setInvoice("");
                    entity.setOutCreatedDate(shopOrder.getOutCreatedAt());
                    entity.setPaymentdate(shopOrder.getOutCreatedAt());
                    entity.setOutId(shopOrder.getOutId());
                    if (CollectionUtils.isEmpty(shopOrder.getExtra())) {
                        entity.setOrderType("");
                    } else {
                        String shopCustomerPickUp = shopOrder.getExtra().get("is_customer_pick_up");
                        String stepOrder = shopOrder.getExtra().get("isStepOrder");
                        if (StringUtils.isNotBlank(shopCustomerPickUp) && Objects.equals(shopCustomerPickUp, "true")) {
                            entity.setShopCustomerPickUp("是");
                        } else {
                            entity.setShopCustomerPickUp("否");
                        }
                        if (StringUtils.isNotBlank(stepOrder) && Objects.equals(stepOrder, "true")) {
                            entity.setOrderType("预售订单");
                        } else {
                            entity.setOrderType("普通订单");
                        }
                    }

                    entity.setSkuQuantity(item.getQuantity());
                    if (null == item.getCleanFee()) {
                        entity.setFee(0D);
                    } else {
                        entity.setFee(new BigDecimal(item.getCleanFee())
                            .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    }
                    //                    entity.setShipFee(null == shopOrderResponse.getResult().getShipFee() ? null
                    // : new BigDecimal(shopOrderResponse.getResult().getShipFee()).divide(new BigDecimal(100), 2,
                    // BigDecimal.ROUND_HALF_UP).doubleValue());
                    entity.setOrderMemo(shopOrder.getBuyerNote());
                    String shipmentStatus = convertStatus(shipmentContext.getShipment().getStatus(),
                        shipmentContext.getShipment().getShipWay());
                    entity.setShipmentStatus(shipmentStatus);
                    if (Objects.equals(shipmentContext.getShipment().getStatus(),
                        MiddleShipmentsStatus.REJECTED.getValue())) {
                        entity.setRejectReason(shipmentExtra.getRejectReason());
                    }
                    //添加 系列时间
                    entity.setCreatedAtDate(shipmentContext.getShipment().getCreatedAt());
                    if (!Objects.isNull(shipmentExtra.getShipmentDate())) {
                    	entity.setShipmentDate(shipmentExtra.getShipmentDate());
                    }
                    //店铺接单时间 backend
                    MposShipmentExtra MposShipment = syncMposShipmentLogic.syncMposShipmentInfo(shipmentContext.getShipment());
                    if (!org.springframework.util.StringUtils.isEmpty(MposShipment) && 
                    		!org.springframework.util.StringUtils.isEmpty(MposShipment.getExtra()) &&
                    		MposShipment.getExtra().containsKey("shipmentAcceptTime")) {
                    	entity.setShipmentAcceptDate(convertDate(MposShipment.getExtra().get("shipmentAcceptTime")));
                    }
                    shipmentExportEntities.add(entity);

                });

            }
        }
        exportService.saveToDiskAndCloud(new ExportContext(shipmentExportEntities), userId);
    }

    private String convertStatus(Integer status, Integer shipWay) {
        String shipmentStatus = null;
        switch (status) {
            case 1:
                if (Objects.equals(shipWay, 1)) {
                    shipmentStatus = "待同步MPOS";
                } else {
                    shipmentStatus = "待同步订单派发中心";
                }
                break;
            case 2:
                if (Objects.equals(shipWay, 1)) {
                    shipmentStatus = "同步MPOS中";
                } else {
                    shipmentStatus = "同步订单派发中心中";
                }
                break;
            case -1:
                if (Objects.equals(shipWay, 1)) {
                    shipmentStatus = "MPOS受理失败";
                } else {
                    shipmentStatus = "订单派发中心受理失败";
                }
                break;
            case -2:
                if (Objects.equals(shipWay, 1)) {
                    shipmentStatus = "同步MPOS失败";
                } else {
                    shipmentStatus = "同步订单派发中心失败";
                }
                break;
            default:
                shipmentStatus = MiddleShipmentsStatus.fromInt(status).getName();
        }
        return shipmentStatus;
    }

    private String getPerformanceShopCode(long shopId) {

        try {

            OpenShop openShop = openShopCacher.findById(shopId);
            if (Arguments.isNull(openShop)) {
                return "";
            }
            Map<String, String> extra = openShop.getExtra();
            if (CollectionUtils.isEmpty(extra)) {
                return "";
            }
            return extra.getOrDefault("hkPerformanceShopOutCode", "");

        } catch (Exception e) {
            log.error("find OpenShop by id:{}fail,cause:{}", shopId, Throwables.getStackTraceAsString(e));
            return "";
        }

    }

    private void exportSettlementPos(PoushengSettlementPosCriteria criteria, Long userId) {
        List<SettlementPosEntity> posExportEntities = new ArrayList<>();

        criteria.setPageSize(500);
        int pageNo = 1;
        while (true) {
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
            r.getResult().getData().forEach(posContext -> {
                SettlementPosEntity entity = new SettlementPosEntity();
                entity.setPosSerialNo(posContext.getPosSerialNo());
                entity.setPosAmt(new BigDecimal(posContext.getPosAmt() == null ? 0 : posContext.getPosAmt())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN).toString());
                entity.setShopId(posContext.getShopId());
                entity.setShopName(posContext.getShopName());
                entity.setPosType(posContext.getPosType() == 1 ? "正常销售" : "售后");
                entity.setOrderId(posContext.getOrderId());
                entity.setPosCreatedAt(posContext.getPosCreatedAt());
                entity.setCreatedAt(posContext.getCreatedAt());
                posExportEntities.add(entity);
            });
        }
        exportService.saveToDiskAndCloud(new ExportContext(posExportEntities), userId);
    }

    private void exportReverseHeadless(ReverseHeadlessCriteria criteria, Long userId) {

        List<ReverseHeadExportEntity> reverseHeadExportEntityList = new ArrayList<>();

        criteria.setPageSize(10);
        int pageNo = 1;
        while (true) {
            criteria.setPageNo(pageNo++);
            log.info("export.reverse.headless.criteria={}", JsonMapper.nonEmptyMapper().toJson(criteria));
            Paging<ReverseHeadlessDto> paging = reverseHeadlessInfoLogic.paging(criteria);
            log.info("export.reverse.headless.response={}", JsonMapper.nonEmptyMapper().toJson(paging));
            if (paging.getTotal() == 0) {
                break;
            }

            if (Objects.isNull(paging.getData())) {
                break;
            }
            if (paging.getData().isEmpty()) {
                break;
            }
            paging.getData().forEach(reverseHeadlessDto -> {
                reverseHeadExportEntityList.add(HeadlessInfoConvert.convertHeadLessDtoToExport(reverseHeadlessDto));
            });
        }
        exportService.saveToDiskAndCloud(new ExportContext(reverseHeadExportEntityList), userId);
    }

    /**
     * 批量查询收货人信息
     *
     * @param ids 订单ids
     * @return
     */
    private Map<Long, ReceiverInfo> findReceiverInfos(List<Long> ids) {
        Map<Long, ReceiverInfo> result = Maps.newHashMapWithExpectedSize(ids.size());
        if (CollectionUtils.isEmpty(ids)) {
            return result;
        }

        Response<Map<Long, ReceiverInfo>> response = receiverInfoReadService.findByOrderIds(ids, OrderLevel.SHOP);

        if (!response.isSuccess()) {
            log.warn("get receiver fail,orderIds:{},error:{}", ids, response.getError());
            return result;
        }
        if (!Objects.isNull(response.getResult())) {
            result = response.getResult();
        }
        return result;
    }

    /**
     * 查询店铺信息
     *
     * @param id
     * @return
     */
    private Shop findShop(Long id) {
        try {
            return shopCacher.findShopById(id);
        } catch (Exception e) {
            log.warn("failed to find shop by shopId {}", id);
        }
        return null;
    }

    /**
     * 转换data
     * @param line
     * @return
     */
    private static Date convertDate(String line) {
    	Date date = null;
        SimpleDateFormat sDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
			 date = sDateFormat.parse(line);
		} catch (ParseException e) {
            log.error("convert date fail,error:{}", e);
            throw new JsonResponseException("convert.date.fail");
		}
        return date;
    }

}
