package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.mpos.dto.MposPaginationResponse;
import com.pousheng.middle.open.mpos.dto.MposResponse;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.exception.ServiceException;
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
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

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
        Shipment update = new Shipment();
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Map<String, String> extraMap = shipment.getExtra();
        try{
            Map<String,Object> param = this.assembShipmentParam(shipment);
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
            shipmentExtra.setMposShipmentId(res.getResult());
        }catch(Exception e){
            // 同步失败
            log.error("sync shipment(id:{}) to mpos failed,cause:{}",shipment.getId(), Throwables.getStackTraceAsString(e));
            OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            }
            return Response.fail("sync.shipment.mpos.fail");
        }
        // 同步成功
        log.info("sync shipment:（id:{}) to mpos success",shipment.getId());
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
            //如果是自提，直接扣库存
            if(Objects.equals(shipmentExtra.getTakeWay(),"2")){
                //扣减库存
                DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
                mposSkuStockLogic.decreaseStock(dispatchOrderItemInfo);
                // 发货推送pos信息给恒康
                Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                if(!response.isSuccess()){
                    Map<String,Object> param1 = Maps.newHashMap();
                    param1.put("shipmentId",shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param1,TradeConstants.FAIL_SYNC_POS_TO_HK,response.getError());
                }
            }
        }
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
        update.setId(shipment.getId());
        update.setExtra(extraMap);
        shipmentWiteLogic.update(update);
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
                throw new ServiceException(resp.getError());
            }
            return resp.getResult();
        }catch (Exception e) {
            log.error("sync mpos shipment status fail,cause by {}", e.getMessage());
            return Paging.empty(MposShipmentExtra.class);
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
        Response<Shop> shopResponse = shopReadService.findById(shipmentExtra.getWarehouseId());
        if(!shopResponse.isSuccess()){
            log.error("find shop by id:{} failed,cause:{}",shopResponse.getError());
            throw new ServiceException("find.shop.not.exists");
        }
        Shop shop = shopResponse.getResult();
        param.put("shopOuterId",shop.getOuterId());
        param.put("businessId",shop.getBusinessId());
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


}