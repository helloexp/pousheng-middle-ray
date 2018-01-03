package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.mpos.dto.MposResponse;
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
 * Created by penghui on 2018/1/2
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
     * 同步发货单至mpos
     * @param shipment 发货单
     * @return
     */
    public Response<Boolean> syncShipmentToMpos(Shipment shipment){
        // 更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_MPOS.toOrderOperation();
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }
        shipment = shipmentReadLogic.findShipmentById(shipment.getId());
        Map<String,Serializable> param = this.assembShipmentParam(shipment);
        MposResponse res = mapper.fromJson(syncMposApi.syncShipmentToMpos(param),MposResponse.class);
        if(!res.isSuccess()){
            log.error("sync shipments:(id:{}) fail.error:{}",shipment.getId(),res.getError());
            // 同步失败
            OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            }
            return Response.fail(res.getError());
        }
        Map<String, String> extraMap = shipment.getExtra();
        Shipment update = new Shipment();
        log.info("sync shipment:（id:{}) to mpos success",shipment.getId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        shipmentExtra.setOutShipmentId(res.getResult());
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
        update.setId(shipment.getId());
        update.setExtra(extraMap);
        shipmentWiteLogic.update(update);
        // 同步成功
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            return Response.fail(updateSyncStatusRes.getError());
        }
        return Response.ok(true);
    }

    /**
     * 恒康发货通知mpos
     * @param shipment  发货单
     * @param shopOrder 订单
     * @return
     */
    public Response<Boolean> syncShippedToMpos(Shipment shipment,ShopOrder shopOrder){
        try {
            // 更新本地ECP状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);
            Map<String,Serializable> param = this.assembShipShipmentParam(shipment);
            MposResponse resp = mapper.fromJson(syncMposApi.syncShipmentShippedToMpos(param),MposResponse.class);
            if (!resp.isSuccess()) {
                // 同步失败
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail(resp.getError());
            }
            // 同步成功
            OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
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
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
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
                return shipmentItem.getSkuCode();
            }
        });
        param.put("outerSkuCodes",mapper.toJson(skuCodes));
        return param;
    }

    /**
     * 组装发货单发货参数
     * @param shipment   发货单
     * @return
     */
    private Map<String,Serializable> assembShipShipmentParam(Shipment shipment){
        Map<String,Serializable> param = Maps.newHashMap();
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        param.put("shipmentId",shipmentExtra.getOutShipmentId());
        param.put("shipmentCorpCode",shipmentExtra.getShipmentCorpCode());
        param.put("shipmentSerialNo",shipmentExtra.getShipmentSerialNo());
        param.put("shipmentDate",shipmentExtra.getShipmentDate());
        return param;
    }

}