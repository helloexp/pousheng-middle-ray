package com.pousheng.middle.web.order.component;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 *发货单发货完成并且订单状态改变之后同步发货信息到电商
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/29
 * pousheng-middle
 */
@Slf4j
@Component
public class EcpOrderLogic {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private SyncOrderToEcpLogic syncOrderToEcpLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    public void shipToEcp(Long shipmentId) {

        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        long orderShopId = orderShipment.getOrderId();
        //获取店铺订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
        //获取ecpOrderStatus
        String status = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        //获取shipment的Extra信息
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        try {
            log.info("try to notify ecp to ship ,shipemntId is{}",shipmentId);
            //判断ecpOrder的状态是否是初始的待发货状态,如果不是,跳过
            if (Objects.equals(Integer.valueOf(status), EcpOrderStatus.WAIT_SHIP.getValue())) {
                log.info("try to notify ecp to ship step one ,shipemntId is{}",shipmentId);
                Response<Boolean> response = orderWriteLogic.updateEcpOrderStatus(shopOrder, MiddleOrderEvent.SHIP.toOrderOperation());
                if (!response.isSuccess()) {
                    log.error("update shopOrder(id:{}) failed, error:{}", orderShopId, response.getError());
                    throw new ServiceException(response.getError());
                }
                //冗余shipmentId到extra中
                Map<String, String> extraMap1 = shopOrder.getExtra();
                extraMap1.put(TradeConstants.ECP_SHIPMENT_ID, String.valueOf(shipment.getId()));
                Response<Boolean> response1 = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, extraMap1);
                if (!response1.isSuccess()) {
                    log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), response1.getError());
                    throw new ServiceException(response1.getError());
                }
            }
            //同步订单信息到电商平台
            this.syncEcpShipmentInfos(shopOrder.getId());
        } catch (ServiceException e) {
            log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), Throwables.getStackTraceAsString(e));
        }catch (Exception e) {
            log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), Throwables.getStackTraceAsString(e));
        }

    }


    private void syncEcpShipmentInfos(Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //获取第一个返货单生成时冗余的发货单id
        String shipmentId = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_SHIPMENT_ID,shopOrder);

        Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(shipmentId));
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
        //最后一个发货单发货完成之后需要将订单同步到电商
        String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);
        if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())&&!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.OFFICIAL.getValue())&&!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.SUNINGSALE.getValue())){
            if( Objects.equals(shopOrder.getOutFrom(), MiddleChannel.YJ.getValue())){
                //同步到云聚
                syncOrderToEcpLogic.syncToYunJu(shopOrder);

            }else{
                syncOrderToEcpLogic.syncOrderToECP(shopOrder, expressCompanyCode, shipment.getId());
            }
        }else{
            syncOrderToEcpLogic.syncShipmentsToEcp(shopOrder);
        }

    }

}
