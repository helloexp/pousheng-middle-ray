package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.mpos.dto.MposPaginationResponse;
import com.pousheng.middle.open.mpos.dto.MposResponse;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.ReceiverInfo;
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
    @Autowired
    private OpenShopReadService openShopReadService;


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
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, orderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }
        shipment = shipmentReadLogic.findShipmentById(shipment.getId());
        Shipment update = new Shipment();
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Map<String, String> extraMap = shipment.getExtra();
        try{
            //判断该发货单是全渠道订单的店发发货单还是普通的mpos发货单
            MposResponse res = null;
            //不是新的全渠道订单,走老的全渠道订单且是销售发货
            if (!orderReadLogic.isNewAllChannelOpenShop(shipment.getShopId()) && Objects.equals(shipment.getType(), ShipmentType.SALES_SHIP.value())){
                Map<String,Object> param = this.assembShipmentParam(shipment);
                if (orderReadLogic.isAllChannelOpenShop(shipment.getShopId())){
                    res = mapper.fromJson(syncMposApi.syncAllChannelShipmnetToMpos(param),MposResponse.class);
                }else{
                    res = mapper.fromJson(syncMposApi.syncShipmentToMpos(param),MposResponse.class);
                }
            }else{
                //新全渠道和所有的换货发货统一走新全渠道发货（换货发货商品条码可能会边，如果用旧全渠道可能会报子订单不存在错误）
                Map<String,Object> param = this.assembNewShipmentParam(shipment);
                res = mapper.fromJson(syncMposApi.syncNewAllChannelShipmnetToMpos(param),MposResponse.class);
            }

            log.info("sync mpos shipment result,res is {}",res);

            if(!res.isSuccess()){
                log.error("sync shipments:(id:{}) fail.error:{}",shipment.getId(),res.getError());
                // 同步失败
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
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
            Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            }
            return Response.fail("sync.shipment.mpos.fail");
        }
        // 同步成功
        log.info("sync shipment:（id:{}) to mpos success",shipment.getId());
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_MPOS_ACCEPT_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            return Response.fail(updateSyncStatusRes.getError());
        }
        //如果指定门店，状态流向待发货
        if(Objects.equals(shipmentExtra.getIsAppint(),"1")){
            Shipment shipment1 = shipmentReadLogic.findShipmentById(shipment.getId());
            OrderOperation syncOrderOperation1 = MiddleOrderEvent.MPOS_RECEIVE.toOrderOperation();
            Response<Boolean> updateSyncStatusRes1 = shipmentWiteLogic.updateStatusLocking(shipment1, syncOrderOperation1);
            if (!updateSyncStatusRes1.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes1.getError());
            }
            //如果是自提，直接扣库存
            if(Objects.equals(shipmentExtra.getTakeWay(),"2")){
                //扣减库存
                mposSkuStockLogic.decreaseStock(shipment);
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
            log.error("sync mpos shipment status fail,cause by {}", Throwables.getStackTraceAsString(e));
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

    public MposResponse revokeMposShipment(Shipment shipment){
        MposResponse res = new MposResponse();
        try {
            if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SYNC_HK_ACCEPT_FAILED.getValue())){
                log.warn("shipment(id:{}) status is:{} so skip sync parana cancel",shipment.getId(),shipment.getStatus());
                res.setSuccess(Boolean.TRUE);
                return res;
            }

            Map<String,Object> param = Maps.newHashMap();
            param.put("outerShipmentId",shipment.getId());
            res = mapper.fromJson(syncMposApi.revokeMposShipment(param),MposResponse.class);
            return res;
        }catch (Exception e){
            log.error("revoke mpos shipment failed,shipment id is {},caused by {}",shipment.getId(),Throwables.getStackTraceAsString(e));
            res.setResult(e.getMessage());
            res.setError(e.getMessage());
            res.setSuccess(false);
            return res;
        }
    }

    public MposResponse revokeNewMposShipment(Shipment shipment){
        MposResponse res = new MposResponse();
        try {
            if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SYNC_HK_ACCEPT_FAILED.getValue())){
               log.warn("shipment(id:{}) status is:{} so skip sync parana cancel",shipment.getId(),shipment.getStatus());
                res.setSuccess(Boolean.TRUE);
                return res;
            }

            Map<String,Object> param = Maps.newHashMap();
            param.put("outerShipmentId",shipment.getId());
            res = mapper.fromJson(syncMposApi.revokeNewMposShipment(param),MposResponse.class);
            return res;
        }catch (Exception e){
            log.error("revoke mpos shipment failed,shipment id is {},caused by {}",shipment.getId(),Throwables.getStackTraceAsString(e));
            res.setResult(e.getMessage());
            res.setError(e.getMessage());
            res.setSuccess(false);
            return res;
        }
    }

    /**
     * 组装发货单参数
     * @param shipment 发货单
     * @return
     */
    private Map<String,Object> assembNewShipmentParam(Shipment shipment){
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

        //查询接单店铺
        Response<Shop> shopResponse = shopReadService.findById(shipmentExtra.getWarehouseId());
        if(!shopResponse.isSuccess()){
            log.error("find shop by id:{} failed,cause:{}",shopResponse.getError());
            throw new ServiceException("find.shop.not.exists");
        }
        Shop shop = shopResponse.getResult();

        //查询下单店铺
        Response<OpenShop>  openShopResponse  = openShopReadService.findById(shipment.getShopId());
        if(!openShopResponse.isSuccess()){
            log.error("find open shop by id:{} failed,cause:{}",shopResponse.getError());
            throw new ServiceException("find.open.shop.not.exists");
        }
        OpenShop openShop = openShopResponse.getResult();

        //下单店铺的恒康店铺外码
        String orderShopCode;
        //下单店铺的恒康公司码
        String orderBusinessId;
        //mpos门店则直接取app_key
        if (orderReadLogic.isMposOpenShop(shipment.getShopId())){
            List<String> businessIdAndOutCode = Splitter.on("-").splitToList(openShop.getAppKey());
            orderBusinessId = businessIdAndOutCode.get(0);
            orderShopCode = businessIdAndOutCode.get(1);
        } else {
            orderShopCode = openShop.getExtra().get(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE);
            orderBusinessId = openShop.getExtra().get(TradeConstants.HK_COMPANY_CODE);
        }


        Map<String,Object> param = Maps.newHashMap();
        //接单店铺公司码
        param.put("shipShopBizId",shop.getBusinessId());
        //接单店铺外码
        param.put("shipShopCode",shop.getOuterId());
        //下单店铺公司码
        param.put("orderShopBizId",orderBusinessId);
        //下单店铺公司码
        param.put("orderShopCode",orderShopCode);
        //中台发货单号
        param.put("outerShipmentId",shipment.getId());
        //发货单商品信息
        param.put("skuInfo",mapper.toJson(shipmentReadLogic.getShipmentItems(shipment)));
        //外部订单号
        param.put("outOrderId",shopOrder.getOutId());
        //订单来源渠道
        param.put("channel", shopOrder.getOutFrom());
        //中台联系人信息
        ReceiverInfo receiverInfo =  orderReadLogic.findReceiverInfo(shopOrder.getId());
        param.put("receiverInfoJson",mapper.toJson(receiverInfo));
        //是否指定门店:1:指定门店,2.不指定门店
        param.put("isAssignShop",2);
        return param;
    }

    /**
     * mpos发货单确认收货
     * @param shipment
     * @return
     */
    public boolean omniShipmmentConfirm(Shipment shipment){
        try{
            log.info("omni shipment confirm start ,shipmentId {}",shipment);
            Map<String,Object> param = Maps.newHashMap();
            param.put("outerShipmentId",shipment.getId());
            param.put("confirmedAt",System.currentTimeMillis());
            MposResponse res = mapper.fromJson(syncMposApi.omniShipmmentConfirm(param),MposResponse.class);
            if (res.isSuccess()){
                return true;
            }else{
                log.info("omni shipment confirm shipped failed,shipment is {},caused by {}",shipment,res.getError());
                return false;
            }
        }catch (Exception e){
            log.info("omni shipment confirm shipped failed,shipment is {},caused by {}",shipment,Throwables.getStackTraceAsString(e));
            return false;
        }
    }
}
