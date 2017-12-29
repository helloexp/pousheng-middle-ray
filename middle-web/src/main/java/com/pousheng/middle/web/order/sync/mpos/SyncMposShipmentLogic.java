package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;


/**
 * 同步Mpos发货单
 */
@Component
@Slf4j
public class SyncMposShipmentLogic{

    @Autowired
    private SyncMposApi syncMposApi;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     *  同步发货单至mpos
     * @param shipment
     * @return
     */
    public Response<Boolean> syncShipmentToMpos(Shipment shipment){
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_MPOS.toOrderOperation();
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
            Response.fail(updateStatusRes.getError());
        }
        Map<String,Serializable> param = this.assembShipmentParam(shipment);
        Response<Long> res = mapper.fromJson(syncMposApi.syncShipmentToMpos(param),Response.class);
        if(!res.isSuccess()){
            log.error("sync shipments:(id:{}) fail",shipment.getId());
            //同步失败
            OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            }
            Response.fail(res.getError());
        }
        //保存mpos发货单id
        Map<String, String> extraMap = shipment.getExtra();
        Shipment update = new Shipment();
        log.info("sync shipment:（id:{}) to mpos success",shipment.getId());
        Long outShipmentId = res.getResult();
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        shipmentExtra.setOutShipmentId(outShipmentId.toString());
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
        update.setId(shipment.getId());
        update.setExtra(extraMap);
        shipmentWiteLogic.update(update);
        //同步成功
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            Response.fail(updateSyncStatusRes.getError());
        }
        return Response.ok(true);
    }

    /**
     * 恒康发货通知mpos
     * @return
     */
    public Response<Boolean> syncShippedToMpos(Shipment shipment,ShopOrder shopOrder){
        //同步本地ecpstatus状态
        //更新状态为同步中
        try {
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);
            Map<String,Serializable> param = this.assembShipShipmentParam(shipment,shopOrder);
            Response<Boolean> resp = mapper.fromJson(syncMposApi.syncShipmentShippedToMpos(param),Response.class);
            if (!resp.isSuccess()) {
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            } else {
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail(resp.getError());
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), e.getMessage());
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
            return Response.fail("sync.ecp.failed");
        }
        return Response.ok(true);
    }


    /**
     * 组装发货单参数
     * @param shipment 发货单
     * @return
     */
    private Map<String,Serializable> assembShipmentParam(Shipment shipment){
        Map<String,Serializable> param = Maps.newHashMap();
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentById(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        param.put("orderId",shopOrder.getOutId());
        param.put("id",shipmentExtra.getWarehouseId());
        param.put("name",shipmentExtra.getWarehouseName());
        param.put("shipmentType",shipmentExtra.getShipmentWay());
        param.put("outShipmentId",shipment.getId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        List<String> skuCodes = Lists.transform(shipmentItems, new Function<ShipmentItem, String>() {
            @Nullable
            @Override
            public String apply(@Nullable ShipmentItem shipmentItem) {
                return shipmentItem.getOutSkuCode();
            }
        });
        param.put("outerSkuCodes",mapper.toJson(skuCodes));
        return param;
    }

    /**
     * 组装发货单发货参数
     * @param shipment
     * @param shopOrder
     * @return
     */
    private Map<String,Serializable> assembShipShipmentParam(Shipment shipment,ShopOrder shopOrder){
        Map<String,Serializable> param = Maps.newHashMap();
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        param.put("shipmentId",shipmentExtra.getOutShipmentId());
        param.put("shipmentCorpCode",shipmentExtra.getShipmentCorpCode());
        param.put("shipmentSerialNo",shipmentExtra.getShipmentSerialNo());
        param.put("shipmentDate",shipmentExtra.getShipmentDate());
        return param;
    }
}