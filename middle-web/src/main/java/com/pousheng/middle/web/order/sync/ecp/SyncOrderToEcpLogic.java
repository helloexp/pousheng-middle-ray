package com.pousheng.middle.web.order.sync.ecp;

import com.google.common.base.Splitter;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.SyncTaobaoStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.order.dto.OpenClientOrderShipment;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 同步电商发货单逻辑
 * Created by tony on 2017/7/5.
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncOrderToEcpLogic {
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private OrderServiceCenter orderServiceCenter;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    /**
     * 同步发货单到电商--只需要传送第一个发货的发货单
     * @param shopOrder 店铺订单
     * @param expressCompayCode 物流公司好
     * @param shipmentId 发货单主键
     * @return
     */
    public Response<Boolean> syncOrderToECP(ShopOrder shopOrder,String expressCompayCode,Long shipmentId)
    {
        try {
            //获取ecpOrderStatus
            String status = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
            //只有待同步电商平台或者是同步电商失败的才可以同步到苏宁京东
            if (Objects.equals(status, String.valueOf(EcpOrderStatus.SHIPPED_WAIT_SYNC_ECP.getValue()))||Objects.equals(status, String.valueOf(EcpOrderStatus.SYNC_ECP_FAIL.getValue()))){
                Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);
                OpenClientOrderShipment orderShipment = new OpenClientOrderShipment();
                orderShipment.setOuterOrderId(shopOrder.getOutId());
                orderShipment.setLogisticsCompany(expressCompayCode);
                //填写运单号
                String shipmentSerialNo = StringUtils.isEmpty(shipmentExtra.getShipmentSerialNo())?"":Splitter.on(",").omitEmptyStrings().trimResults().splitToList(shipmentExtra.getShipmentSerialNo()).get(0);
                orderShipment.setWaybill(shipmentSerialNo);
                //目前苏宁需要传入商品编码
                List<String> outSkuCodes = Lists.newArrayList();
                if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.SUNING.getValue())){
                    for (ShipmentItem shipmentItem:shipmentItems){
                        outSkuCodes.add(shipmentItem.getOutSkuCode());
                    }
                    orderShipment.setOuterSkuCodes(outSkuCodes);
                }
                Response<Boolean> response = orderServiceCenter.ship(shopOrder.getShopId(), orderShipment);
                if (response.isSuccess()) {
                    //同步成功
                    OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                    orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
                } else {
                    //同步失败
                    OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                    orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                    return Response.fail(response.getError());
                }
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), e.getMessage());
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
            return Response.fail("sync.ecp.failed");
        }

        return Response.ok(Boolean.TRUE);
    }



    /**
     * 同步发货单到电商，需要上传所有发货单到电商，天猫是子单发货，如果一个发货单中只有赠品，该发货单不会被同步到电商平台，就算已经同步成功
     * @param shopOrder 店铺订单
     * @return
     */
    public Response<Boolean> syncShipmentsToEcp(ShopOrder shopOrder)
    {

        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);

            List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrder.getId());
            List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                    .filter(it->!Objects.equals(MiddleShipmentsStatus.CANCELED.getValue(),it.getStatus()) && !Objects.equals(MiddleShipmentsStatus.REJECTED.getValue(),it.getStatus())).collect(Collectors.toList());
            int count = 0;//判断是否存在同步淘宝失败的发货单
            for (OrderShipment orderShipment:orderShipmentsFilter){
                Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(orderShipment.getShipmentId()));
                //如果发货单的发货状态不是已发货的，跳过
                if (!Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.SHIPPED.getValue())){
                    continue;
                }
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                try{
                    //获取当前同步淘宝的状态
                    Integer syncTaobaoStatus = shipmentExtra.getSyncTaobaoStatus()==null? SyncTaobaoStatus.WAIT_SYNC_TAOBAO.getValue()
                            :shipmentExtra.getSyncTaobaoStatus();
                    //如果存在已经同步淘宝成功的发货单,则跳过
                    if (Objects.equals(syncTaobaoStatus,SyncTaobaoStatus.SYNC_TAOBAO_SUCCESS.getValue())){
                        continue;
                    }
                    shipmentExtra.setSyncTaobaoStatus(syncTaobaoStatus);
                    //获取快递信息
                    ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
                    String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);

                    OpenClientOrderShipment openClientOrderShipment = new OpenClientOrderShipment();
                    openClientOrderShipment.setOuterOrderId(shopOrder.getOutId());
                    openClientOrderShipment.setLogisticsCompany(expressCompanyCode);
                    List<String> outerItemOrderIds = Lists.newArrayList();
                    List<String> outerSkuCodes = Lists.newArrayList();
                    //添加外部子订单的id
                    for (ShipmentItem shipmentItem:shipmentItems){
                        //淘宝会用到外部sku订单号
                        if (!StringUtils.isEmpty(shipmentItem.getSkuOutId())){
                            outerItemOrderIds.add(shipmentItem.getSkuOutId());
                        }
                        //官网会用到skuCode
                        outerSkuCodes.add(shipmentItem.getSkuCode());
                    }
                    openClientOrderShipment.setOuterItemOrderIds(outerItemOrderIds);
                    openClientOrderShipment.setOuterSkuCodes(outerSkuCodes);
                    //填写运单号
                    String shipmentSerialNo = StringUtils.isEmpty(shipmentExtra.getShipmentSerialNo())?"":Splitter.on(",").omitEmptyStrings().trimResults().splitToList(shipmentExtra.getShipmentSerialNo()).get(0);
                    openClientOrderShipment.setWaybill(shipmentSerialNo);
                    log.info("ship to ecp,shopOrderId is {},openClientOrderShipment is {}",shopOrder.getId(),openClientOrderShipment);
                    Response<Boolean> response = null;
                    if (!isTaobaoGiftShipmentOnly(shopOrder,shipmentItems)){
                        response = orderServiceCenter.ship(shopOrder.getShopId(), openClientOrderShipment);
                    }else{
                        response = Response.ok(Boolean.TRUE);
                    }

                    Map<String,String> extraMap = shipment.getExtra();
                    extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
                    shipment.setExtra(extraMap);

                    if (response.isSuccess()){
                        shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment,MiddleOrderEvent.SYNC_TAOBAO_SUCCESS.toOrderOperation());
                    }else{
                        count++;
                        shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment,MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                    }
                }catch (Exception e){
                    log.error("sync shipment to taobao failed,shipmentId is {},caused by {}",shipment.getId(),e.getMessage());
                    shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment,MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                    throw new ServiceException(e.getMessage());
                }
            }

            if (count==0) {
                //同步成功
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            } else {
                //同步失败
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail("sync.ecp.fail");
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), e.getMessage());
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
        }

        return Response.ok();
    }

    /**
     * 判断是否是淘宝的赠品发货单(该发货单中只有淘宝赠品)
     * @param shopOrder
     * @param shipmentItems
     * @return 不是淘宝赠品发货单false,
     */
    public boolean isTaobaoGiftShipmentOnly(ShopOrder shopOrder,List<ShipmentItem> shipmentItems)
    {
        if (Objects.equals(shopOrder.getOutFrom(),MiddleChannel.TAOBAO.getValue())&&shipmentItems.size()==1){
           for (ShipmentItem shipmentItem:shipmentItems){
               if (StringUtils.isEmpty(shipmentItem.getSkuOutId())){
                   return true;
               }
           }
        }
        return false;
    }
}
