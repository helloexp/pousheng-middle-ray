package com.pousheng.middle.web.order.sync.erp;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.yyedi.SyncYYEdiShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 1.同步发货单信息到erp
 * 2.将已经同步到erp的发货单取消
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/15
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncErpShipmentLogic {
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private SyncYYEdiShipmentLogic syncYYEdiShipmentLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    /**
     * 根据配置渠道确定将发货单同步到hk还是订单派发中心
     * @param shipment
     * @return
     */
    public Response<Boolean> syncShipment(Shipment shipment){
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopOrder.getShopId());
        if (!openShopResponse.isSuccess()){
            log.error("find open shop by openShopId {} failed,caused by {}",shopOrder.getShopId(),openShopResponse.getError());
            return Response.fail(openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra =  openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE)==null?"hk":openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        switch (erpSyncType){
            case "hk":
                return syncShipmentLogic.syncShipmentToHk(shipment);
            case "yyEdi":
                return syncYYEdiShipmentLogic.syncShipmentToYYEdi(shipment);
            default:
                log.error("can not find sync erp type,openShopId is {}",shopOrder.getShopId());
                return Response.fail("find.open.shop.extra.erp.sync.type.fail");
        }
    }

    /**
     * 将已经同步到erp的发货单取消
     * @param shipment 发货单
     * @param operationType  0 取消 1 删除 2 收货状态更新
     * @return
     */
    public Response<Boolean> syncShipmentCancel(Shipment shipment,Integer operationType){
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopOrder.getShopId());
        if (!openShopResponse.isSuccess()){
            log.error("find open shop by openShopId {} failed,caused by {}",shopOrder.getShopId(),openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra =  openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE)==null?"hk":openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        switch (erpSyncType){
            case "hk":
                return syncShipmentLogic.syncShipmentCancelToHk(shipment,operationType);
            case "yyEdi":
                return syncYYEdiShipmentLogic.syncShipmentCancelToYYEdi(shipment);
            default:
                log.error("can not find sync erp type,openShopId is {}",shopOrder.getShopId());
                return Response.fail("find.open.shop.extra.erp.sync.type.fail");
        }
    }

    /**
     * 自动同步发货单收货信息到erp
     * @param shipment 发货单
     * @param operationType 0 取消 1 删除 2 收货状态更新
     * @param syncOrderOperation 同步失败的动作(手动和自动略有不同)
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentDone(Shipment shipment,Integer operationType,OrderOperation syncOrderOperation){
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopOrder.getShopId());
        if (!openShopResponse.isSuccess()){
            log.error("find open shop by openShopId {} failed,caused by {}",shopOrder.getShopId(),openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra =  openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE)==null?"hk":openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        switch (erpSyncType){
            case "hk":
                return syncShipmentLogic.syncShipmentDoneToHk(shipment,operationType,syncOrderOperation);
            case "yyEdi":
                Response<Boolean> r = syncShipmentPosLogic.syncShipmentDoneToHk(shipment);
                if (r.isSuccess()){
                    OrderOperation operation = MiddleOrderEvent.HK_CONFIRMD_SUCCESS.toOrderOperation();
                    Response<Boolean> updateStatus = shipmentWiteLogic.updateStatus(shipment, operation);
                    if (!updateStatus.isSuccess()) {
                        log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), operation.getText(), updateStatus.getError());
                        return Response.fail(updateStatus.getError());
                    }
                }else{
                    log.error("shipment(id:{}) notify hk failed,cause:{}",r.getError());
                  /*  Map<String,Object> param = Maps.newHashMap();
                    param.put("shipmentId",shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param,TradeConstants.FAIL_SYNC_SHIPMENT_CONFIRM_TO_HK,r.getError());*/
                    updateShipmetDoneToHkFail(shipment,MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
                    return Response.fail("恒康返回信息:"+r.getError());
                }
                return Response.ok(Boolean.TRUE);
            default:
                return syncShipmentLogic.syncShipmentDoneToHk(shipment,operationType,syncOrderOperation);
        }
    }
    private void updateShipmetDoneToHkFail(Shipment shipment,OrderOperation syncOrderOperation){
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }

}
