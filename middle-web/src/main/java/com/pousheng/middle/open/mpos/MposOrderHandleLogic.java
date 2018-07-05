package com.pousheng.middle.open.mpos;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.component.*;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by ph on 2018/1/10
 * 处理mpos发货单状态改变
 */
@Slf4j
@Component
public class MposOrderHandleLogic {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private ExpressCodeReadService expressCodeReadService;

    @Autowired
    private MposShipmentLogic mposShipmentLogic;

    @Autowired
    private ShipmentWriteManger shipmentWriteManger;

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    /**
     * 拉取发货单状态的更新
     * @param mposShipmentExtras
     */
    public void handleOrder(List<MposShipmentExtra> mposShipmentExtras){
        for(MposShipmentExtra mposShipmentExtra:mposShipmentExtras){
            try{
                log.info("begin to handle order,mposShipmentExtra is {}",mposShipmentExtra);
                Map<String,String> shipExtra = mposShipmentExtra.transToMap();
                Shipment shipment = shipmentReadLogic.findShipmentById(mposShipmentExtra.getOuterShipmentId());
                if(shipment != null && idempotencyValidate(shipment.getStatus(),mposShipmentExtra.getStatus().toString())){
                    this.deal(shipment,mposShipmentExtra.getStatus().toString(),shipExtra);
                }
            }catch (Exception e){
                log.error("handle shipment status fail,cause:{}",Throwables.getStackTraceAsString(e));
            }
        }
    }

    /**
     * 处理发货单状态更新
     * @param shipment      发货单
     * @param status        状态
     * @param extra         额外信息
     */
    private void deal(Shipment shipment,String status,Map<String,String> extra){
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Map<String, String> extraMap = shipment.getExtra();
        MiddleOrderEvent orderEvent = null;
        Shipment update = null;
        switch (status){
            case TradeConstants.MPOS_SHIPMENT_WAIT_SHIP:
                orderEvent = MiddleOrderEvent.MPOS_RECEIVE;
                shipmentExtra.setReceiveStaff(extra.get(TradeConstants.MPOS_RECEIVE_STAFF));
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                break;
            case TradeConstants.MPOS_SHIPMENT_CALL_SHIP:
                orderEvent = MiddleOrderEvent.MPOS_RECEIVE;
                break;
            case TradeConstants.MPOS_SHIPMENT_REJECT:
                orderEvent = MiddleOrderEvent.MPOS_REJECT;
                shipmentExtra.setRejectReason(extra.get(TradeConstants.MPOS_REJECT_REASON));
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                break;
            case TradeConstants.MPOS_SHIPMENT_SHIPPED:
                orderEvent = MiddleOrderEvent.SHIP;
                update = new Shipment();
                update.setId(shipment.getId());
                //保存物流信息
                shipmentExtra.setShipmentSerialNo(extra.get(TradeConstants.SHIP_SERIALNO));
                if(Objects.nonNull(extra.get(TradeConstants.SHIP_CORP_CODE))){
                    try{

                        ExpressCode expressCode = orderReadLogic.makeExpressNameByMposCode(extra.get(TradeConstants.SHIP_CORP_CODE));
                        shipmentExtra.setShipmentCorpName(expressCode.getName());
                        shipmentExtra.setShipmentCorpCode(expressCode.getHkCode());
                        DateTime dt = DateTime.parse(extra.get(TradeConstants.SHIP_DATE), DFT);
                        shipmentExtra.setShipmentDate(dt.toDate());

                    }catch (Exception e){
                        log.error("query express(code:{}) failed,cause:{}",extra.get(TradeConstants.SHIP_CORP_CODE),Throwables.getStackTraceAsString(e));
                    }
                }
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                update.setExtra(extraMap);
                break;
            default:
                log.info("error status");
        }
        if(Objects.isNull(orderEvent)) {
            return;
        }
        Response<Boolean> res = shipmentWiteLogic.updateStatusLocking(shipment,orderEvent.toOrderOperation());
        if(!res.isSuccess()){
            log.error("sync shipment(id:{}) fail,cause:{}",shipment.getId(),res.getError());
            return ;
        }
        if(Objects.nonNull(update)) {
            shipmentWiteLogic.update(update);
        }

        if (Objects.equals(orderEvent,MiddleOrderEvent.MPOS_REJECT)){
            //如果拒单，将据单原因更新到发货单扩展字段里
            shipmentWiteLogic.updateExtra(shipment.getId(), extraMap);
            //回滚发货单
            Shipment shipment1 = shipmentReadLogic.findShipmentById(shipment.getId());

            //如果是换货发货门店拒单则需要更新对应售后单的状态
            if (Objects.equals(shipment1.getType(), ShipmentType.EXCHANGE_SHIP.value())){

                OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment1.getId());
                Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());

                Response<Boolean> updateRes = refundWriteLogic.updateStatus(refund,MiddleOrderEvent.MPOS_REJECT.toOrderOperation());
                if (!updateRes.isSuccess()){
                    log.error("mpos reject so to update refund(id:{})  fail,error:{}",refund.getId(),updateRes.getError());
                }

                Map<String, Integer> skuCodeAndQuantity = Maps.newHashMap();

                List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment1);
                for (ShipmentItem shipmentItem : shipmentItems){
                    skuCodeAndQuantity.put(shipmentItem.getSkuCode(),0- shipmentItem.getQuantity());
                }

                refundWriteLogic.updateSkuHandleNumber(refund.getId(),skuCodeAndQuantity);

                return;
            }
            shipmentWriteManger.rollbackSkuOrderWaitHandleNumber(shipment1);
        }
        if (!Objects.equals(orderEvent,MiddleOrderEvent.MPOS_RECEIVE)) {
            mposShipmentLogic.onUpdateMposShipment(new MposShipmentUpdateEvent(shipment.getId(), orderEvent));
        }
        log.info("sync shipment(id:{}) success",shipment.getId());
    }

    /**
     * 确保幂等性
     * @param status     中台发货单状态
     * @param shipStatus mpos发货单状态
     * @return
     */
    private Boolean idempotencyValidate(Integer status,String shipStatus){
        switch (shipStatus){
            case TradeConstants.MPOS_SHIPMENT_WAIT_SHIP:
                if(Objects.equals(status, MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                    return false;
                }
                break;
            case TradeConstants.MPOS_SHIPMENT_CALL_SHIP:
                if(Objects.equals(status, MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                    return false;
                }
                break;
            case TradeConstants.MPOS_SHIPMENT_REJECT:
                if(Objects.equals(status, MiddleShipmentsStatus.REJECTED.getValue())) {
                    return false;
                }
                break;
            case TradeConstants.MPOS_SHIPMENT_SHIPPED:
                if(Objects.equals(status, MiddleShipmentsStatus.SHIPPED.getValue())) {
                    return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
