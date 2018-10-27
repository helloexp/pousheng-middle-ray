package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenClientOrderConsignee;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.order.impl.dao.OrderReceiverInfoDao;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 预售订单后续处理流程
 *
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.web.biz.impl
 * 2018/10/22 15:14
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.STEP_ORDER_NOTIFY_ERP)
@Service
@Slf4j
public class StepOrderNotifyErpService implements CompensateBizService {

    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReceiverInfoDao orderReceiverInfoDao;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (log.isDebugEnabled()) {
            log.debug("START-PROCESSING-STEP-ORDER-NOTFIY-ERP,poushengCompensateBiz {}", poushengCompensateBiz);
        }
        if (Objects.isNull(poushengCompensateBiz)) {
            log.error("poushengCompensateBiz is null");
            return;
        }
        if (!Objects.equals(poushengCompensateBiz.getBizType(), PoushengCompensateBizType.STEP_ORDER_NOTIFY_ERP.name())) {
            log.error("poushengCompensateBiz type error,id {},currentType {}", poushengCompensateBiz.getId(), poushengCompensateBiz.getBizType());
            return;
        }

        OpenClientFullOrder openClientFullOrder = JsonMapper.nonEmptyMapper().fromJson(poushengCompensateBiz.getContext(), OpenClientFullOrder.class);

        Long shopOrderId = Long.valueOf(poushengCompensateBiz.getBizId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Map<String, String> extraMap = shopOrder.getExtra();
        extraMap.put(TradeConstants.STEP_ORDER_STATUS, String.valueOf(OpenClientStepOrderStatus.PAID.getValue()));
        Response<Boolean> r = orderWriteService.updateOrderExtra(shopOrderId, OrderLevel.SHOP, extraMap);
        if (!r.isSuccess()) {
            log.error("update shopOrder extra failed, shopOrder id is {},caused by {}", shopOrderId, r.getError());
            return;
        }

        //更新订单金额
        this.updateShopOrderFee(shopOrder, openClientFullOrder);
        //更新收货人地址
        this.updateRecevicerInfo(shopOrder,openClientFullOrder);
        //更新发货单金额
        this.updateShipmentFee(shopOrder);

        //同步发货单到恒康
        List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrderId);
        //获取待同步恒康的发货单
        List<OrderShipment> orderShipmentFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(orderShipment -> Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.WAIT_SYNC_HK.getValue()))
                .collect(Collectors.toList());


        //将待同步恒康的发货单发到恒康
        for (OrderShipment orderShipment : orderShipmentFilter) {
            try {
                Shipment shipment = shipmentReadLogic.findShipmentById(orderShipment.getShipmentId());
                //更新发货单金额

                if (!Objects.equals(shipment.getShipWay(), TradeConstants.MPOS_SHOP_DELIVER)) {
                    //仓库
                    shipmentWiteLogic.handleSyncShipment(shipment, 1, shopOrder);
                } else {
                    //门店
                    shipmentWiteLogic.handleSyncShipment(shipment, 2, shopOrder);
                }
            } catch (Exception e) {
                log.error("sync shipment(id:{}) to hk fail,error:{}", orderShipment.getShipmentId(), Throwables.getStackTraceAsString(e));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("END-PROCESSING-STEP-ORDER-NOTFIY-ERP,poushengCompensateBiz {}", poushengCompensateBiz);
        }
    }

    /**
     * 预售订单更新订单金额
     *
     * @param shopOrder
     */
    private void updateShopOrderFee(ShopOrder shopOrder, OpenClientFullOrder openClientFullOrder) {
        orderWriteLogic.updateOrderAmountByOrderId(shopOrder.getShopId(), shopOrder.getId(), openClientFullOrder);
    }

    /**
     * 更新发货单金额
     *
     * @param shopOrder
     */
    private void updateShipmentFee(ShopOrder shopOrder) {
        shipmentWiteLogic.updateShipmentFee(shopOrder.getId());
    }

    /**
     * 京东渠道更新收货人地址
     * @param shopOrder
     * @param openClientFullOrder
     */
    public void updateRecevicerInfo(ShopOrder shopOrder,OpenClientFullOrder openClientFullOrder){
        if (Objects.equals(shopOrder.getOutFrom(),MiddleChannel.JD.getValue())){
            OpenClientOrderConsignee openClientOrderConsignee =  openClientFullOrder.getConsignee();
            List<OrderReceiverInfo> receiverInfos = orderReceiverInfoDao.findByOrderIdAndOrderLevel(shopOrder.getId(), OrderLevel.SHOP);
            OrderReceiverInfo orderReceiverInfo = receiverInfos.get(0);
            ReceiverInfo receiverInfo = orderReceiverInfo.getReceiverInfo();
            receiverInfo.setReceiveUserName(openClientOrderConsignee.getName());
            orderReceiverInfo.setReceiverInfo(receiverInfo);
            boolean recevierInfoResult = orderReceiverInfoDao.update(orderReceiverInfo);
            if (!recevierInfoResult){
                log.error("failed to update orderReceiveInfo failed,(shopOrderId={}))",shopOrder.getId());
            }
        }
    }
}
