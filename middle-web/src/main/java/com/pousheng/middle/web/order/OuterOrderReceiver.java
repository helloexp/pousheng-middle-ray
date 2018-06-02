package com.pousheng.middle.web.order;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.DispatchOrderEngine;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.web.events.trade.listener.AutoCreateShipmetsListener;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposApi;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.open.client.center.job.order.api.OrderReceiver;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.parana.component.ParanaOrderConverter;
import io.terminus.open.client.parana.dto.OrderInfo;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 外部订单接收器（供内网系统之间调用）
 * Created by songrenfei on 2017/12/18
 */
@Api(description = "外部订单接收器")
@RestController
@Slf4j
@RequestMapping("/api/outer/order")
public class OuterOrderReceiver {

    @Autowired
    private ParanaOrderConverter paranaOrderConverter;
    @RpcConsumer
    private ShopReadService shopReadService;
    @Autowired
    private OrderReceiver orderReceiver;
    @Autowired
    private DispatchOrderEngine dispatchOrderEngine;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private SyncMposApi syncMposApi;
    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private AutoCreateShipmetsListener autoCreateShipmetsListener;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final Integer BATCH_SIZE = 100;     // 批处理数量




    @ApiOperation("创建外部订单")
    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> createOuterShopOrder(@RequestBody OrderInfo orderInfo) {
        //目前就mpos系统调用，所以

        ShopOrder mposShopOrder = orderInfo.getShopOrder();

        //判断门店是否存在
        val rExist = shopReadService.findById(mposShopOrder.getShopId());
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",mposShopOrder.getShopId(),rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();
        ShopExtraInfo extraInfo = ShopExtraInfo.fromJson(exist.getExtra());
        //获取对应的open shop id
        Long openShopId = extraInfo.getOpenShopId();
        if(Arguments.isNull(openShopId)){
            log.error("create outer order fail,because shop(id:{}) not find open shop record",exist.getId());
            return Response.fail("not.find.open.shop");
        }
        //封装open shop info
        OpenClientShop openClientShop = new OpenClientShop();
        openClientShop.setChannel(ShopConstants.CHANNEL);
        openClientShop.setOpenShopId(openShopId);
        openClientShop.setShopName(exist.getName());

        OpenClientFullOrder openClientFullOrder = paranaOrderConverter.transform(orderInfo);
        openClientFullOrder.setExtra(orderInfo.getShopOrder().getExtra());

        //处理单据
        orderReceiver.receiveOrder(openClientShop, Lists.newArrayList(openClientFullOrder));

        return Response.ok(Boolean.TRUE);
    }


    @RequestMapping(value = "/dispatch/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DispatchOrderItemInfo createOuterShopOrder(@PathVariable(value = "id") Long orderId) {

        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(orderId);
        if(!shopOrderRes.isSuccess()){
            throw new JsonResponseException(shopOrderRes.getError());
        }
        Response<List<SkuOrder>> skuOrdersRes = skuOrderReadService.findByShopOrderId(orderId);
        if(!skuOrdersRes.isSuccess()){
            throw new JsonResponseException(skuOrdersRes.getError());
        }
        Response<List<ReceiverInfo>> receiveInfosRes = receiverInfoReadService.findByOrderId(orderId, OrderLevel.SHOP);
        if(!receiveInfosRes.isSuccess()){
            throw new JsonResponseException(receiveInfosRes.getError());
        }

        List<SkuOrder> skuOrders = skuOrdersRes.getResult();
        //获取skuCode,数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayListWithCapacity(skuOrders.size());
        skuOrders.forEach(skuOrder -> {
            SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
            skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
            skuCodeAndQuantity.setQuantity(skuOrder.getQuantity());
            skuCodeAndQuantities.add(skuCodeAndQuantity);
        });
        Response<DispatchOrderItemInfo> response = dispatchOrderEngine.toDispatchOrder(shopOrderRes.getResult(),receiveInfosRes.getResult().get(0),skuCodeAndQuantities);
        if(!response.isSuccess()){
            log.error("dispatch fail,error:{}",response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();

    }


    @RequestMapping(value = "/dispatch/shipment/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void createShipment(@PathVariable(value = "id") Long orderId) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderId);
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(orderId);
        //获取skuCode,数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayListWithCapacity(skuOrders.size());
        skuOrders.forEach(skuOrder -> {
            SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
            skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
            skuCodeAndQuantity.setQuantity(Integer.valueOf(orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.WAIT_HANDLE_NUMBER, skuOrder)));
            skuCodeAndQuantities.add(skuCodeAndQuantity);
        });
        shipmentWiteLogic.toDispatchOrder(shopOrder,null);
    }

    @RequestMapping(value = "/dispatch/shipment/sku/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void createSkuShipment(@PathVariable(value = "id") Long skuOrderId) {
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(Lists.newArrayList(skuOrderId));

        ShopOrder shopOrder = orderReadLogic.findShopOrderById(skuOrders.get(0).getOrderId());
        //获取skuCode,数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayListWithCapacity(skuOrders.size());
        skuOrders.forEach(skuOrder -> {
            SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
            skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
            skuCodeAndQuantity.setQuantity(Integer.valueOf(orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.WAIT_HANDLE_NUMBER, skuOrder)));
            skuCodeAndQuantities.add(skuCodeAndQuantity);
        });
        shipmentWiteLogic.toDispatchOrder(shopOrder,skuCodeAndQuantities);
    }



    @RequestMapping(value = "/auto/handle", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void autoHandle() {

        log.info("[AUTO-HANDLE-ORDER-DISPATCH-BEGIN] begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<ShopOrder> notDispatchOrders = Lists.newArrayList();

        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE,notDispatchOrders);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, BATCH_SIZE,notDispatchOrders);
        }

        log.info("TOTAL-WAIT-HANDLE number is:{}",notDispatchOrders.size());

        for (ShopOrder shopOrder : notDispatchOrders){
            log.info("START-AUTO-DISPATCH ORDER:{}",shopOrder.getOrderCode());
            OpenClientOrderSyncEvent event = new OpenClientOrderSyncEvent(shopOrder.getId());
            try {
                autoCreateShipmetsListener.onShipment(event);
            } catch (Exception e){
                log.info("fail to auto create shipment, shop order code is {} ",shopOrder.getOrderCode());
            }
            log.info("END-AUTO-DISPATCH ORDER:{}",shopOrder.getOrderCode());
        }

        stopwatch.stop();
        log.info("[AUTO-HANDLE-ORDER-DISPATCH-END] done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));


    }

    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size,List<ShopOrder> notDispatchOrders) {

        List<Integer> status = Lists.newArrayList(MiddleOrderStatus.WAIT_HANDLE.getValue());
        OrderCriteria criteria = new OrderCriteria();
        criteria.setStatus(status);
        Response<Paging<ShopOrder>> pagingRes = shopOrderReadService.findBy(pageNo, size, criteria);
        if (!pagingRes.isSuccess()){
            log.error("paging shop order fail,criteria:{},error:{}",criteria,pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<ShopOrder> paging = pagingRes.getResult();
        List<ShopOrder> shopOrders = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(shopOrders)) {
            return Boolean.FALSE;
        }
        notDispatchOrders.addAll(shopOrders);

        int current = shopOrders.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }




}
