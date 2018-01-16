package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.mpos.dto.MposPaginationResponse;
import com.pousheng.middle.open.mpos.dto.MposResponse;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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
    private ShopReadService shopReadService;

    @Autowired
    private EventBus eventBus;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

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
        Map<String,Object> param = this.assembShipmentParam(shipment);
        Shipment update = new Shipment();
        log.info("sync shipment:（id:{}) to mpos success",shipment.getId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Map<String, String> extraMap = shipment.getExtra();
        try{
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
            shipmentExtra.setOutShipmentId(res.getResult());
        }catch(Exception e){
            // 同步失败
            OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            }
            return Response.fail("sync.shipment.mpos.fail");
        }
        // 同步成功
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            return Response.fail(updateSyncStatusRes.getError());
        }
        //如果指定门店，状态流向待发货
        if(Objects.equals(shipmentExtra.getIsAppint(),"1")){
            Shipment shipment1 = shipmentReadLogic.findShipmentById(shipment.getId());
            OrderOperation syncOrderOperation1 = MiddleOrderEvent.MPOS_RECEIVE.toOrderOperation();
            Response<Boolean> updateSyncStatusRes1 = shipmentWiteLogic.updateStatus(shipment1, syncOrderOperation1);
            if (!updateSyncStatusRes1.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes1.getError());
            }
        }
        //如果门店自提，状态流向待收货
        if(Objects.equals(shipmentExtra.getTakeWay(),"2")){
            Shipment shipment1 = shipmentReadLogic.findShipmentById(shipment.getId());
            OrderOperation syncOrderOperation1 = MiddleOrderEvent.SHIP.toOrderOperation();
            Response<Boolean> updateSyncStatusRes1 = shipmentWiteLogic.updateStatus(shipment1, syncOrderOperation1);
            if (!updateSyncStatusRes1.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes1.getError());
            }
            shipmentExtra.setShipmentDate(new Date());
            eventBus.post(new MposShipmentUpdateEvent(shipment.getId(),MiddleOrderEvent.SHIP));
        }
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
        update.setId(shipment.getId());
        update.setExtra(extraMap);
        shipmentWiteLogic.update(update);
        return Response.ok(true);
    }

    /**
     * 恒康发货通知mpos
     * @param shipment  发货单
     * @return
     */
    public Response<Boolean> syncShippedToMpos(Shipment shipment){
        try {
            Map<String,Object> param = this.assembShipShipmentParam(shipment);
            MposResponse resp = mapper.fromJson(syncMposApi.syncShipmentShippedToMpos(param),MposResponse.class);
            if (!resp.isSuccess()) {
                return Response.fail(resp.getError());
            }
            eventBus.post(new MposShipmentUpdateEvent(shipment.getId(),MiddleOrderEvent.SHIP));
        }catch (Exception e) {
            log.error("sync shipment shipped failed,shipmentId is({}),cause by {}", shipment.getId(), e.getMessage());
            return Response.fail("sync.shipment.ship.failed");
        }
        return Response.ok(true);
    }

    /**
     * 拉取mpos发货单状态更新
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Paging<MposShipmentExtra> syncMposShimentStatus(Integer pageNo, Integer pageSize, Date startAt, Date endAt){
        try {
            Map<String,Object> param = Maps.newHashMap();
            param.put("pageNo",pageNo);
            param.put("pageSize",pageSize);
            param.put("startAt",DFT.print(new DateTime(startAt)));
            param.put("endAt",DFT.print(new DateTime(endAt)));
            MposPaginationResponse resp = mapper.fromJson(syncMposApi.syncShipmentStatus(param),MposPaginationResponse.class);
            if (!resp.getSuccess()) {
                log.error("sync mpos shipment status fail,cause:{}",resp.getError());
                return null;
            }
            return resp.getResult();
        }catch (Exception e) {
            log.error("sync mpos shipment status fail,cause by {}", e.getMessage());
            return null;
        }
    }

    /**
     * 组装发货单参数
     * @param shipment 发货单
     * @return
     */
    private Map<String,Object> assembShipmentParam(Shipment shipment){
        Map<String,Object> param = Maps.newHashMap();
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        param.put("orderId",shopOrder.getOutId());
        if(Objects.equals(shipmentExtra.getShipmentWay(),TradeConstants.MPOS_SHOP_DELIVER)){
            Response<Shop> shopResponse = shopReadService.findById(shipmentExtra.getWarehouseId());
            if(shopResponse.isSuccess()){
                Shop shop = shopResponse.getResult();
                param.put("shopOuterId",shop.getOuterId());
            }
        }
        param.put("shopName",shipmentExtra.getWarehouseName());
        param.put("outerShipmentId",shipment.getId());
        param.put("outOrderId",shopOrder.getId());
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
    private Map<String,Object> assembShipShipmentParam(Shipment shipment){
        Map<String,Object> param = Maps.newHashMap();
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        param.put("shipmentId",shipmentExtra.getOutShipmentId());
        param.put("shipmentCorpCode",shipmentExtra.getShipmentCorpCode());
        param.put("shipmentSerialNo",shipmentExtra.getShipmentSerialNo());
        param.put("shipmentDate",shipmentExtra.getShipmentDate());
        return param;
    }

}