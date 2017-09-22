package com.pousheng.middle.web.export;

import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.utils.export.ExportContext;
import com.pousheng.middle.web.utils.export.ExportUtil;
import com.pousheng.middle.web.utils.export.FileRecord;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.impl.jam.mutable.MPackage;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Slf4j
@RestController
@RequestMapping("api/")
public class ExportController {

    private static final int REFUND_EXPORT_MAX_SUPPORT_RESULT_SIZE = 4000;//退货单最大支持导出结果集大小
    private static final int ORDER_EXPORT_MAX_SUPPORT_RESULT_SIZE = 4000;//订单最大支持导出结果集大小

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
    private ExportService exportService;


    /**
     * 导出订单
     *
     * @param middleOrderCriteria 查询参数
     * @return
     */
    @GetMapping("order/export")
    public void orderExport(MiddleOrderCriteria middleOrderCriteria) {

        if (middleOrderCriteria.getOutCreatedEndAt() != null) {
            middleOrderCriteria.setOutCreatedEndAt(new DateTime(middleOrderCriteria.getOutCreatedEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        middleOrderCriteria.setPageSize(500);

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
//            if (pagingRes.getResult().getTotal() > ORDER_EXPORT_MAX_SUPPORT_RESULT_SIZE)
//                throw new InvalidException("export.data.too.many", ORDER_EXPORT_MAX_SUPPORT_RESULT_SIZE, pagingRes.getResult().getTotal());
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


            pagingRes.getResult().getData().forEach(shopOrder -> {
                Long orderID = shopOrder.getId();
//                Response<List<SkuOrder>> skuOrderResponse = skuOrderReadService.findByShopOrderId(orderID);
//                if (!skuOrderResponse.isSuccess()) {
//                    log.error("get sku order fail,error:{}", skuOrderResponse.getError());
//                    throw new JsonResponseException(skuOrderResponse.getError());
//                }
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
                        s -> s.getStatus().equals(MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())).findFirst();

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

                skuOrders.get(orderID).forEach(skuOrder -> {
                    OrderExportEntity export = new OrderExportEntity();
                    export.setOrderID(skuOrder.getOrderId());
                    export.setShopName(skuOrder.getShopName());
                    shipment.ifPresent(s -> {
                        export.setShipmentCorpName(s.getShipmentCorpName());
                        export.setCarrNo(s.getShipmentSerialNo());
                        //TODO 收货人信息待完善
                        receiverInfo.ifPresent(receiver -> {
                            export.setReciverAddress(receiver.getDetail());
                            export.setReciverName(receiver.getReceiveUserName());
                            export.setPhone(receiver.getPhone());
                        });
                        //                    export.setReciverName(s.getExtra().get(""));
                        //                    export.setReciverAddress(s.getExtra().get(""));
                        //                    export.setPhone(s.getExtra().get(""));
                    });

                    //TODO paytype enum
                    export.setPayType("在线支付");
                    payment.ifPresent(p -> export.setPaymentDate(p.getPaidAt()));
                    export.setOrderStatus(MiddleOrderStatus.fromInt(skuOrder.getStatus()).getName());
                    export.setOrderMemo(shopOrder.getBuyerNote());
                    export.setShipFee(skuOrder.getShipFee());
                    //TODO 发票信息待完善
                    export.setInvoice("");
                    //TODO 货号可能是其他字段
                    export.setItemID(skuOrder.getItemId());

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

//                    if (StringUtils.isNotBlank(skuOrder.getSkuCode())) {
//                        Response<List<SkuTemplate>> skuTemplateResponse = skuTemplateReadService.findBySkuCodes(Collections.singletonList(skuOrder.getSkuCode()));
//                        if (!skuTemplateResponse.isSuccess()) {
//                            log.error("get sku template fail,error:{}", skuTemplateResponse.getError());
//                            throw new JsonResponseException(skuTemplateResponse.getError());
//                        }
//                        if (!skuTemplateResponse.getResult().isEmpty()) {
//                            Response<Spu> spuResponse = spuReadService.findById(skuTemplateResponse.getResult().get(0).getSpuId());
//                            if (!spuResponse.isSuccess()) {
//                                log.error("get item fail,error:{}", spuResponse.getError());
//                                throw new JsonResponseException(spuResponse.getError());
//                            }
//                            export.setBrandName(spuResponse.getResult().getBrandName());
//                        }
//                    }
                    if (spus.containsKey(skuOrder.getSkuCode()))
                        export.setBrandName(spus.get(skuOrder.getSkuCode()).getBrandName());
                    export.setSkuQuantity(skuOrder.getQuantity());
                    export.setFee(skuOrder.getFee());
                    orderExportData.add(export);
                });

            });
        }

        exportService.saveToDiskAndCloud(new ExportContext(orderExportData));
    }

    @GetMapping("refund/export")
    public void refundExport(MiddleRefundCriteria criteria) {
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
//            if (pagingResponse.getResult().getTotal() > REFUND_EXPORT_MAX_SUPPORT_RESULT_SIZE)
//                throw new InvalidException("export.data.too.many", REFUND_EXPORT_MAX_SUPPORT_RESULT_SIZE, pagingResponse.getResult().getTotal());
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

                    refundItems.forEach(item -> {
                        RefundExportEntity export = new RefundExportEntity();
                        export.setOrderID(refundInfo.getOrderRefund().getOrderId());
                        export.setShopName(refundInfo.getRefund().getShopName());
                        export.setMemo(refundInfo.getRefund().getBuyerNote());
                        export.setRefundType(MiddleRefundType.from(refundInfo.getRefund().getRefundType()).toString());
                        export.setStatus(MiddleRefundStatus.fromInt(refundInfo.getRefund().getStatus()).getName());

                        export.setAmt(item.getFee());

                        if (StringUtils.isNotBlank(item.getSkuCode()) && spus.containsKey(item.getSkuCode())) {
                            //TODO 货号
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
                        export.setWarehousingDate(refundExtra.getConfirmReceivedAt());

                        refundExportData.add(export);
                    });

                }

            });

        }

        exportService.saveToDiskAndCloud(new ExportContext(refundExportData));
    }


    @GetMapping("export/files")
    public Response<Paging<FileRecord>> exportFiles() {

        List<FileRecord> files = exportService.getExportFiles();

        return Response.ok(new Paging<FileRecord>((long) files.size(), files));
    }


}
