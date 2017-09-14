package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.OrderSource;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import com.taobao.api.domain.Shop;
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

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 恒康第一个发货单发货完成之后需要将ecpOrderstatus状态从初始的代发货修改为已发货
 * Created by tony on 2017/7/13.
 * pousheng-middle
 */
@Slf4j
@Component
public class EcpOrderListener {
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

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void updateEcpOrderInitialStatus(HkShipmentDoneEvent event) {

        Shipment shipment = event.getShipment();
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        long orderShopId = orderShipment.getOrderId();
        //获取店铺订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
        //获取ecpOrderStatus
        String status = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        //获取shipment的Extra信息
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        try {

            //判断ecpOrder的状态是否是初始的待发货状态,如果不是,跳过
            if (Objects.equals(Integer.valueOf(status), EcpOrderStatus.WAIT_SHIP.getValue())) {
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
            log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), e.getMessage());
        }catch (Exception e) {
            log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), e.getMessage());
        }

    }

    private void syncEcpShipmentInfos(Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //获取第一个返货单生成时冗余的发货单id
        String shipmentId = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_SHIPMENT_ID,shopOrder);

        List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrderId);

        List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(it->!Objects.equals(MiddleShipmentsStatus.CANCELED.getValue(),it.getStatus())).collect(Collectors.toList());
        //判断该订单下所有发货单的状态
        List<Integer> orderShipMentStatusList = Lists.transform(orderShipmentsFilter, new Function<OrderShipment, Integer>() {
            @Nullable
            @Override
            public Integer apply(@Nullable OrderShipment orderShipment) {
                return orderShipment.getStatus();
            }
        });
        //判断订单是否已经全部发货了
        int count=0;
        for (Integer status:orderShipMentStatusList){
            if (!Objects.equals(status,MiddleShipmentsStatus.SHIPPED.getValue())){
                count++;
            }
        }
        //如果已经全部发货了并且电商已经无任何发货单要生成了,则通知电商平台状态为已收货,将第一个发货的发货单的运单号之类的传给电商
        if (count==0&&shopOrder.getStatus()>= MiddleOrderStatus.WAIT_SHIP.getValue()){
            Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(shipmentId));
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
            //最后一个发货单发货完成之后需要将订单同步到电商
            String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);
            if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())){
                syncOrderToEcpLogic.syncOrderToECP(shopOrder, expressCompanyCode, shipment.getId());
            }else{
                syncOrderToEcpLogic.syncOrderToTaobao(shopOrder);
            }
        }
    }

}
