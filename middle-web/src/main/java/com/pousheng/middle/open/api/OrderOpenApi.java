package com.pousheng.middle.open.api;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 订单open api
 * Created by songrenfei on 2017/6/15
 */
@OpenBean
@Slf4j
public class OrderOpenApi {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;


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
                                                 @NotEmpty(message = "hk.shipment.id.is.null") String hkShipmentId,
                                                 @NotEmpty(message = "shipment.corp.code.empty") String shipmentCorpCode,
                                                 @NotEmpty(message = "shipment.serial.no.empty") String shipmentSerialNo,
                                                 @NotEmpty(message = "shipment.date.empty") String shipmentDate){
        log.info("HK-SYNC-SHIPMENT-STATUS-START param shipmentId is:{} hkShipmentId is:{} shipmentCorpCode is:{} " +
                "shipmentSerialNo is:{} shipmentDate is:{}",shipmentId,hkShipmentId,shipmentCorpCode,shipmentSerialNo,shipmentDate);

        try {

            DateTime dt = DateTime.parse(shipmentDate, DFT);
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentById(shipmentId);

            //判断状态及获取接下来的状态
            Flow flow = flowPicker.pickShipments();
            OrderOperation orderOperation =  MiddleOrderEvent.SHIP.toOrderOperation();
            if (!flow.operationAllowed(orderShipment.getStatus(), orderOperation)) {
                log.error("shipment(id={})'s status({}) not fit for ship",
                        orderShipment.getId(), orderShipment.getStatus());
                throw new OPServerException("shipment.current.status.not.allow.ship");
            }
            Integer targetStatus = flow.target(orderShipment.getStatus(),orderOperation);
            Shipment shipment = shipmentReadLogic.findShipmentById(orderShipment.getShipmentId());
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);


            //封装更新信息
            Shipment update = new Shipment();
            update.setId(shipment.getId());
            Map<String,String> extraMap = shipment.getExtra();
            shipmentExtra.setErpOrderShopCode(hkShipmentId);
            shipmentExtra.setShipmentSerialNo(shipmentSerialNo);
            shipmentExtra.setShipmentCorpCode(shipmentCorpCode);
            //shipmentExtra.setShipmentCorpName();todo 转换为中文
            shipmentExtra.setShipmentDate(dt.toDate());
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO,mapper.toJson(shipmentExtra));
            update.setExtra(extraMap);

            //更新状态
            Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(),targetStatus);
            if(!updateStatusRes.isSuccess()){
                log.error("update shipment(id:{}) status to :{} fail,error:{}",shipment.getId(),targetStatus,updateStatusRes.getError());
                throw new OPServerException(updateStatusRes.getError());
            }

            //更新基本信息
            Response<Boolean> updateRes = shipmentWriteService.update(update);
            if(!updateRes.isSuccess()){
                log.error("update shipment(id:{}) extraMap to :{} fail,error:{}",shipment.getId(),extraMap,updateRes.getError());
                throw new OPServerException(updateStatusRes.getError());
            }

        }catch (JsonResponseException e){
            log.error("hk sync shipment(id:{}) to pousheng fail,error:{}",shipmentId,e.getMessage());
            throw new OPServerException(e.getMessage());
        }

        log.info("HK-SYNC-SHIPMENT-STATUS-END");
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
