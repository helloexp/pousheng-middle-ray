package com.pousheng.middle.web.export;

import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.settle.enums.PayType;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Slf4j
@RestController
@RequestMapping("api/**")
public class ExportController {


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
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;


    /**
     * 导出订单
     *
     * @param middleOrderCriteria 查询参数
     * @return
     */
    @GetMapping(value = "order/export", produces = "application/vnd.ms-excel")
    public void orderExport(MiddleOrderCriteria middleOrderCriteria, HttpServletResponse response) {

        if (middleOrderCriteria.getOutCreatedEndAt() != null) {
            middleOrderCriteria.setOutCreatedEndAt(new DateTime(middleOrderCriteria.getOutCreatedEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        middleOrderCriteria.setPageSize(200);

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

            if (pagingRes.getResult().isEmpty())
                break;

            pagingRes.getResult().getData().forEach(shopOrder -> {
                Long orderID = shopOrder.getId();
                Response<List<SkuOrder>> skuOrderResponse = skuOrderReadService.findByShopOrderId(orderID);
                if (!skuOrderResponse.isSuccess()) {
                    log.error("get sku order fail,error:{}", skuOrderResponse.getError());
                    throw new JsonResponseException(skuOrderResponse.getError());
                }
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
                        s -> s.getStatus().equals(MiddleShipmentsStatus.SHIPPED.getValue())).findFirst();

                Optional<Payment> payment = paymentResponse.getResult().stream().filter(p -> (p.getStatus().equals(3))).findAny();

                Optional<ReceiverInfo> receiverInfo = receiverResponse.getResult().stream().findFirst();

                skuOrderResponse.getResult().forEach(skuOrder -> {

                    Response<Spu> spuResponse = spuReadService.findById(skuOrder.getItemId());
                    if (!spuResponse.isSuccess()) {
                        log.error("get item fail,error:{}", spuResponse.getError());
                        throw new JsonResponseException(spuResponse.getError());
                    }


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
                    export.setOrderStatus(MiddleOrderStatus.fromInt(skuOrder.getStatus()).name());
                    export.setOrderMemo(shopOrder.getBuyerNote());
                    export.setShipFee(skuOrder.getShipFee());
                    //TODO 发票信息待完善
                    export.setInvoice("");
                    //TODO 货号可能是其他字段
                    export.setItemID(skuOrder.getItemId());

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

                    export.setBrandName(spuResponse.getResult().getBrandName());
                    export.setSkuQuantity(skuOrder.getQuantity());
                    export.setFee(skuOrder.getFee());
                    orderExportData.add(export);
                });

            });
        }


        ExportContext exportContext = new ExportContext(orderExportData);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportUtil.export(exportContext, out);

    }

    @GetMapping("refund/export")
    public byte[] refundExport(RefundCriteria criteria, HttpServletResponse response) {

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

            if (pagingResponse.getResult().isEmpty())
                break;

            pagingResponse.getResult().getData().forEach(refundInfo -> {

                List<RefundItem> refundItems = refundReadLogic.findRefundItems(refundInfo.getRefund());
                RefundExtra refundExtra = refundReadLogic.findRefundExtra(refundInfo.getRefund());

                refundItems.forEach(item -> {
                    Response<Spu> spuResponse = spuReadService.findById(item.getSkuOrderId());
                    if (!spuResponse.isSuccess()) {
                        log.error("get item fail,error:{}", spuResponse.getError());
                        throw new JsonResponseException(spuResponse.getError());
                    }

                    RefundExportEntity export = new RefundExportEntity();
                    export.setOrderID(refundInfo.getOrderRefund().getOrderId());
                    export.setShopName(refundInfo.getRefund().getShopName());
                    export.setMemo(refundInfo.getRefund().getBuyerNote());
                    export.setRefundType(MiddleRefundType.from(refundInfo.getRefund().getRefundType()).toString());
                    export.setStatus(MiddleRefundStatus.fromInt(refundInfo.getRefund().getStatus()).name());

                    export.setAmt(item.getFee());
                    //TODO 货号
                    export.setBrand(spuResponse.getResult().getBrandName());
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

                    export.setApplyQuantity(item.getAlreadyHandleNumber());
                    refundExtra.getHkConfirmItemInfos().stream().filter(hkinfo -> hkinfo.getItemCode().equalsIgnoreCase(item.getSkuCode())).findAny().ifPresent(hkinfo -> {
                        export.setActualQuantity(hkinfo.getQuantity());
                    });
                    export.setWarehousingDate(refundExtra.getConfirmReceivedAt());

                    refundExportData.add(export);
                });
            });

        }

        ExportContext context = new ExportContext(refundExportData);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportUtil.export(context, out);

        response.setHeader("Content-Disposition", "attachment;filename=export.xls");
        return out.toByteArray();
    }
}
