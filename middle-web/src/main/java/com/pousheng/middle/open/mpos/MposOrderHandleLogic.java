package com.pousheng.middle.open.mpos;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by ph on 2018/1/10
 * 处理mpos订单状态改变
 */
@Slf4j
@Component
public class MposOrderHandleLogic {

    @Autowired
    private ShopOrderReadService shopOrderReadService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private EventBus eventBus;

    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    /**
     * 根据订单的extra,识别发货单状态
     * @param openClientFullOrders
     */
    public void specialHandleOrder(List<OpenClientFullOrder> openClientFullOrders){
        openClientFullOrders.forEach(openClientFullOrder -> {
            this.handleOrder(openClientFullOrder);
        });
    }

    private void handleOrder(OpenClientFullOrder openClientFullOrder){
        Response<Optional<ShopOrder>> findShopOrder = this.shopOrderReadService.findByOutIdAndOutFrom(openClientFullOrder.getOrderId(), ShopConstants.CHANNEL);
        ShopOrder shopOrder = null;
        if (!findShopOrder.isSuccess()) {
            log.error("fail to find shop order by outId={},outFrom={} when receive order,cause:{}", new Object[]{openClientFullOrder.getOrderId(), ShopConstants.CHANNEL, findShopOrder.getError()});
            return ;
        } else {
            Optional<ShopOrder> shopOrderOptional = (Optional) findShopOrder.getResult();
            if (!shopOrderOptional.isPresent()) {
                return ;
            }
            shopOrder = shopOrderOptional.get();
        }
//        Long shopOrderId = shopOrder.getId();
//        List<OpenClientOrderItem> list = openClientFullOrder.getItems();
//        Map<String,Integer> skuStatusMap = Maps.newHashMap();
//        list.forEach(openClientOrderItem -> {
//            skuStatusMap.put(openClientOrderItem.getSkuCode(),openClientOrderItem.getStatus().getValue());
//        });
//        OpenClientOrderItem openClientOrderItem = new OpenClientOrderItem();
        Map<String,String> extra = Maps.newHashMap();
        if(!extra.containsKey("shipments")){
            return ;
        }
        List<MposShipmentExtra> mposShipmentExtras = mapper.fromJson(extra.get("shipments"),List.class);
        for (MposShipmentExtra mposExtra: mposShipmentExtras) {
            Map<String,String> shipExtra = mposExtra.toExtraMap();
            Shipment shipment = shipmentReadLogic.findShipmentById(mposExtra.getShipmentId());
            this.deal(shipment,mposExtra.getStatus(),shipExtra);
        }
        /**
         * {
         *     shipments:[{
         *         {"id":"1","status":"wait_ship","mposReceiveStaff":"接单店员"}，
         *         {"id":"2","status":"shipped","shipmentSerialNo":"1232131","shipmentCorpCode":"SF","shipmentDate":"20180110143940"},
         *         {"id":"3","status":"reject","mposRejectReason":"不想接"}
         *     }]
         * }
         */
//        List<Shipment> shipments = Lists.newArrayList();//shipmentReadLogic.findByShopOrderId(shopOrderId);
//        for (Shipment shipment:shipments) {
//
//            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
//            //仓发不做处理
//            if (Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_WAREHOUSE_DELIVER))
//                continue;
//            boolean handled = false;
//            while(!handled){
//                List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
//                for (ShipmentItem shipmentItem:shipmentItems) {
//                    if(skuStatusMap.containsKey(shipmentItem.getSkuCode())){
//                        this.deal(shipment,skuStatusMap.get(shipmentItem.getSkuCode()).toString(),null);
//                        skuStatusMap.remove(shipmentItem.getSkuCode());
//                        handled = true;
//                        break;
//                    }
//                }
//            }
//        }
    }


    /**
     * 处理发货单状态更新
     * @param shipment      发货单
     * @param status        状态
     * @param extra         额外信息
     */
    private void deal(Shipment shipment,String status,Map<String,String> extra){
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Map<String, String> extraMap = shipment.getExtra();
        MiddleOrderEvent orderEvent = null;
        Shipment update = null;
        switch (status){
            case TradeConstants.MPOS_SHIPMENT_WAIT_SHIP:
                orderEvent = MiddleOrderEvent.MPOS_RECEIVE;
                shipmentExtra.setReceiveStaff(extra.get(TradeConstants.MPOS_RECEIVE_STAFF));
                break;
            case TradeConstants.MPOS_SHIPMENT_REJECT:
                orderEvent = MiddleOrderEvent.MPOS_REJECT;
                shipmentExtra.setRejectReason(extra.get(TradeConstants.MPOS_REJECT_REASON));
                break;
            case TradeConstants.MPOS_SHIPMENT_SHIPPED:
                orderEvent = MiddleOrderEvent.SHIP;
                update = new Shipment();
                update.setId(shipment.getId());
                //保存物流信息
                shipmentExtra.setShipmentSerialNo(extra.get(TradeConstants.SHIP_SERIALNO));
                shipmentExtra.setShipmentCorpCode(extra.get(TradeConstants.SHIP_CORP_CODE));
                ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(extra.get(TradeConstants.SHIP_CORP_CODE));
                shipmentExtra.setShipmentCorpName(expressCode.getName());
                DateTime dt = DateTime.parse(extra.get(TradeConstants.SHIP_DATE), DFT);
                shipmentExtra.setShipmentDate(dt.toDate());
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                update.setExtra(extraMap);
                break;
        }
        Response<Boolean> res = shipmentWiteLogic.updateStatus(shipment,orderEvent.toOrderOperation());
        if(!res.isSuccess()){
            log.error("sync shipment(id:{}) fail,cause:{}",shipment.getId(),res.getError());
            throw new JsonResponseException(res.getError());
        }
        if(Objects.nonNull(update))
            shipmentWiteLogic.update(update);
        eventBus.post(new MposShipmentUpdateEvent(shipment.getId(),orderEvent));
        log.info("sync shipment(id:{}) success",shipment.getId());
    }
}
