package com.pousheng.middle.open.mpos;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.RejectShipmentOccupy;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.StockRecordType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.events.warehouse.StockRecordEvent;
import com.pousheng.middle.web.order.component.*;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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
    private MposShipmentLogic mposShipmentLogic;

    @Autowired
    private ShipmentWriteManger shipmentWriteManger;

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;

    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private EventBus eventBus;

    @Autowired
    private ErpClient erpClient;

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
                log.info("shipExtra param is {}", shipExtra);
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
     * @param shipment      发货单middle-web/src/main/java/com/pousheng/middle/open/api/ExpressInfoOpenApi.java
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
                update.setShipmentSerialNo(extra.get(TradeConstants.SHIP_SERIALNO));
                //保存物流信息
                shipmentExtra.setShipmentSerialNo(extra.get(TradeConstants.SHIP_SERIALNO));

                if(Objects.nonNull(extra.get(TradeConstants.SHIP_CORP_CODE))){
                    try{

                        ExpressCode expressCode = orderReadLogic.makeExpressNameByMposCode(extra.get(TradeConstants.SHIP_CORP_CODE));
                        shipmentExtra.setShipmentCorpName(expressCode.getName());
                        shipmentExtra.setShipmentCorpCode(expressCode.getHkCode());
                        DateTime dt = DateTime.parse(extra.get(TradeConstants.SHIP_DATE), DFT);
                        shipmentExtra.setShipmentDate(dt.toDate());
                        update.setShipmentCorpCode(expressCode.getHkCode());
                        update.setShipmentCorpName(expressCode.getName());
                    }catch (Exception e){
                        log.error("query express(code:{}) failed,cause:{}",extra.get(TradeConstants.SHIP_CORP_CODE),Throwables.getStackTraceAsString(e));
                    }
                }
                // 圆通回传的快递单号
                if (!StringUtils.isEmpty(extra)
                        && !StringUtils.isEmpty(extra.get(TradeConstants.YTO_CALL_BACK_MAIL_NO))){
                    shipmentExtra.setCallbackMailNo(extra.get(TradeConstants.YTO_CALL_BACK_MAIL_NO));
                    update.setShipmentSerialNo(extra.get(TradeConstants.YTO_CALL_BACK_MAIL_NO));
                }
                // 物流单号
                if (!StringUtils.isEmpty(extra)
                        && !StringUtils.isEmpty(extra.get(TradeConstants.EXPRESS_ORDER_ID))){
                    shipmentExtra.setExpressOrderId(extra.get(TradeConstants.EXPRESS_ORDER_ID));
                }

                List<ShipmentItem> items = shipmentReadLogic.getShipmentItems(shipment);
                //店发默认全部发货
                for (ShipmentItem s : items) {
                    s.setShipQuantity(s.getQuantity());
                }
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, mapper.toJson(items));
                log.info("extraMap param is {}", extraMap);
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

            // 异步订阅 用于记录库存数量的日志
            eventBus.post(new StockRecordEvent(shipment.getId(), StockRecordType.MPOS_REFUSE_ORDER.toString()));

            //如果拒单，将据单原因更新到发货单扩展字段里
            shipmentWiteLogic.updateExtra(shipment.getId(), extraMap);

            //如果是换货发货门店拒单则需要更新对应售后单的状态并释放占用库存
            if (Objects.equals(shipment.getType(), ShipmentType.EXCHANGE_SHIP.value())
                    ||Objects.equals(shipment.getType(), 3)){
                handleExchangeShipReject(shipment);
                return;
            }
            //回滚发货单
            shipmentWriteManger.rollbackSkuOrderWaitHandleNumber(shipment);
        }
        if (!Objects.equals(orderEvent,MiddleOrderEvent.MPOS_RECEIVE)) {
            mposShipmentLogic.onUpdateMposShipment(new MposShipmentUpdateEvent(shipment.getId(), orderEvent));
        }
        //如果发货将发货单信息同步到esp
        if (Objects.equals(orderEvent,MiddleOrderEvent.SHIP)) {
            synExpressInfoToEsp(shipment.getShipmentCode(),shipmentExtra.getShipmentCorpCode(),shipmentExtra.getShipmentSerialNo());
        }
        log.info("sync shipment(id:{}) success",shipment.getId());
    }

    /**
     * 丢件补发或者是换货被拒单后处理逻辑
     * @param shipment 发货单
     */
    public void handleExchangeShipReject(Shipment shipment){
        //如果是换货发货门店拒单则需要更新对应售后单的状态并释放占用库存
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());

        Response<Boolean> updateRes = refundWriteLogic.updateStatus(refund,MiddleOrderEvent.MPOS_REJECT.toOrderOperation());
        if (!updateRes.isSuccess()){
            log.error("mpos reject so to update refund(id:{})  fail,error:{}",refund.getId(),updateRes.getError());
        }

        Map<String, Integer> skuCodeAndQuantity = Maps.newHashMap();

        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        for (ShipmentItem shipmentItem : shipmentItems){
            skuCodeAndQuantity.put(shipmentItem.getSkuCode(),0- shipmentItem.getQuantity());
        }
        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
            refundWriteLogic.updateSkuHandleNumber(orderShipment.getAfterSaleOrderId(), skuCodeAndQuantity);
        } else {
            refundWriteLogic.updateSkuHandleNumberForLost(orderShipment.getAfterSaleOrderId(), skuCodeAndQuantity);
        }
        //换货被拒单不立即释放库存，等到生成新的发货单之后才会释放库存
        //mposSkuStockLogic.unLockStock(shipment);

        //记录拒单的发货单号
        List<RejectShipmentOccupy> shipmentOccupies = refundReadLogic.getShipmentOccupies(refund);
        RejectShipmentOccupy rejectShipmentOccupy = new RejectShipmentOccupy();
        rejectShipmentOccupy.setShipmentId(shipment.getId());
        rejectShipmentOccupy.setStatus(RejectShipmentOccupy.ShipmentOccupyStatus.OCCUPY.name());
        shipmentOccupies.add(rejectShipmentOccupy);
        Refund newRefund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
        Map<String, String> refundExtra = newRefund.getExtra();
        refundExtra.put(TradeConstants.REJECT_SHIPMENT_OCCUPY_LIST,JsonMapper.nonDefaultMapper().toJson(shipmentOccupies));
        newRefund.setExtra(refundExtra);
        refundWriteLogic.update(newRefund);

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

    /**
     * 向esp同步mpos发货单信息
     * @param shipmentCode
     * @param shipmentCorpCode
     * @param shipmentSerialNo
     */
    public void synExpressInfoToEsp(String shipmentCode, String shipmentCorpCode, String shipmentSerialNo) {
        if (StringUtils.isEmpty(shipmentCode) || StringUtils.isEmpty(shipmentCorpCode)
                || StringUtils.isEmpty(shipmentSerialNo)){
            log.error("fail sync express info to esp shipmentCode:{}、 shipmentCorpCode:{}、 shipmentSerialNo:{} param invalid",
                    shipmentCode,shipmentCorpCode,shipmentSerialNo);
            return;
        }
        switch (shipmentCorpCode){
            case "shunfeng":
                shipmentCorpCode = "SF";
                break;
            case "yuantong":
                shipmentCorpCode = "YTO";
                break;
            case "SF":
                shipmentCorpCode = "SF";
                break;
            case "YTO":
                shipmentCorpCode = "YTO";
                break;
            default:
                log.error("fail sync express info to esp shipmentCode:{}、 shipmentCorpCode:{}、 shipmentSerialNo:{} shipmentCorpCode invalid",
                        shipmentCode,shipmentCorpCode,shipmentSerialNo);
                shipmentCorpCode = "";
        }
        if (Strings.isNullOrEmpty(shipmentCorpCode)){
            return;
        }
        //往esp推送信息
        Map<String,Object> requestBody = Maps.newHashMap();
        Map<String,Object> body = Maps.newHashMap();
        Map<String,String> params = Maps.newHashMap();
        params.put("BillNo",shipmentCode);
        params.put("ExpressCode",shipmentCorpCode);
        params.put("mailno",shipmentSerialNo);
        params.put("SourceId","中台");
        List<Map<String,String>> requestData = new ArrayList<>();
        requestData.add(params);
        body.put("requestData",requestData);
        requestBody.put("body",body);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestBody);
        log.info("synExpressInfoToEsp begin push shipments,shipmentCode:{},expressCode:{},mailno:{}",shipmentCode,shipmentCorpCode,shipmentSerialNo);
        String response = erpClient.postJson("common/esp/default/pushexpress",
                paramJson);
        log.info("synExpressInfoToEsp shipmentCode:{} end,the response:{}",shipmentCode,response);
    }
}
