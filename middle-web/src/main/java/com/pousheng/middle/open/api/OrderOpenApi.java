package com.pousheng.middle.open.api;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.service.PaymentReadService;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;

/**
 * 订单open api
 * Created by songrenfei on 2017/6/15
 */
@OpenBean
@Slf4j
public class OrderOpenApi {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @RpcConsumer
    private PaymentReadService paymentReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");


    /**
     * 恒康同步发货完成状态到中台
     * @param shipmentId 中台发货单号
     * @param hkShipmentId 恒康发货单号
     * @param shipmentCorpCode 物流公司代码
     * @param shipmentSerialNo 物流单号
     * @param shipmentDate 发货时间
     * @return 是否同步成功
     */
    @OpenMethod(key = "hk.shipments.api", paramNames = {"shipmentId","hkShipmentId","shipmentCorpCode","shipmentSerialNo",
            "shipmentDate"}, httpMethods = RequestMethod.POST)
    public void syncHkShipmentStatus(@NotNull(message = "shipment.id.is.null") Long shipmentId,
                                                 @NotNull(message = "hk.shipment.id.is.null") Long hkShipmentId,
                                                 @NotEmpty(message = "shipment.corp.code.empty") String shipmentCorpCode,
                                                 @NotEmpty(message = "shipment.serial.no.empty") String shipmentSerialNo,
                                                 @NotEmpty(message = "shipment.date.empty") String shipmentDate){
        log.info("HK-SYNC-SHIPMENT-STATUS-START param shipmentId is:{} hkShipmentId is:{} shipmentCorpCode is:{} " +
                "shipmentSerialNo is:{} shipmentDate is:{}",shipmentId,hkShipmentId,shipmentCorpCode,shipmentSerialNo,shipmentDate);

        log.info("HK-SYNC-SHIPMENT-STATUS-END");
        throw new OPServerException("更新失败");
    }


    @OpenMethod(key = "hk.refund.confirm.received.api", paramNames = {"refundOrderId","hkRefundOrderId","receivedDate","itemInfo",
            "shipmentDate"}, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(@NotNull(message = "refund.order.id.is.null") Long refundOrderId,
                                     @NotNull(message = "hk.refund.order.id.is.null") Long hkRefundOrderId,
                                     @NotEmpty(message = "item.info.empty") String itemInfo,
                                     @NotEmpty(message = "received.date.empty") String receivedDate){
        log.info("HK-SYNC-REFUND-STATUS-START param refundOrderId is:{} hkRefundOrderId is:{} itemInfo is:{} " +
                "shipmentDate is:{}",refundOrderId,hkRefundOrderId,itemInfo,receivedDate);

        log.info("HK-SYNC-REFUND-STATUS-END");
        throw new OPServerException("更新失败");
    }


}
