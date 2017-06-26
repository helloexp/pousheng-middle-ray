package com.pousheng.middle.web.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentPagingInfo;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 发货单相关api （以 order shipment 为发货单）
 * Created by songrenfei on 2017/6/20
 */
@RestController
@Slf4j
public class Shipments {

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @RpcConsumer
    private WarehouseSkuReadService warehouseSkuReadService;
    @Autowired
    private MiddleOrderFlowPicker orderFlowPicker;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();



    //发货单分页
    @RequestMapping(value = "/api/shipment/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<ShipmentPagingInfo> findBy(OrderShipmentCriteria shipmentCriteria) {

        Response<Paging<ShipmentPagingInfo>> response =  orderShipmentReadService.findBy(shipmentCriteria);
        if(!response.isSuccess()){
            log.error("find shipment by criteria:{} fail,error:{}",shipmentCriteria,response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<ShipmentPagingInfo> shipmentDtos = response.getResult().getData();
        Flow flow = orderFlowPicker.pickShipments();
        shipmentDtos.forEach(shipmentDto ->shipmentDto.setShopOrderOperations(flow.availableOperations(shipmentDto.getOrderShipment().getStatus())));
        return response.getResult();
    }


    //发货单详情
    @RequestMapping(value = "/api/shipment/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShipmentDetail findDetail(@PathVariable(value = "id") Long orderShipmentId) {

        return shipmentReadLogic.orderDetail(orderShipmentId);
    }


    /**
     * 订单下的发货单
     * @param shopOrderId 店铺订单id
     * @return 发货单
     */
    @RequestMapping(value = "/api/order/{id}/shipments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<OrderShipment> shipments(@PathVariable("id") Long shopOrderId) {
        Response<List<OrderShipment>> response = orderShipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if(!response.isSuccess()){
            log.error("find order shipment by order id:{} level:{} fail,error:{}",shopOrderId,OrderLevel.SHOP.toString(),response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }


    /**
     * 发货预览
     *
     * @param shopOrderId 店铺订单id
     * @param data skuOrderId及数量 json格式
     * @param warehouseId          仓库id
     * @return 订单信息
     */
    @RequestMapping(value = "/api/order/{id}/ship/preview", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OrderDetail> shipPreview(@PathVariable("id") Long shopOrderId,
                                             @RequestParam("data") String data,
                                             @RequestParam(value = "warehouseId") Long warehouseId){
        Map<Long, Integer> skuOrderIdAndQuantity = analysisSkuOrderIdAndQuantity(data);

        Response<OrderDetail> orderDetailRes = orderReadLogic.orderDetail(shopOrderId);
        if(!orderDetailRes.isSuccess()){
            log.error("find order detail by order id:{} fail,error:{}",shopOrderId,orderDetailRes.getError());
            throw new JsonResponseException(orderDetailRes.getError());
        }
        OrderDetail orderDetail = orderDetailRes.getResult();
        List<SkuOrder> allSkuOrders = orderDetail.getSkuOrders();
        List<SkuOrder> currentSkuOrders = allSkuOrders.stream().filter(skuOrder -> skuOrderIdAndQuantity.containsKey(skuOrder.getId())).collect(Collectors.toList());
        currentSkuOrders.forEach(skuOrder -> skuOrder.setQuantity(skuOrderIdAndQuantity.get(skuOrder.getId())));
        orderDetail.setSkuOrders(currentSkuOrders);

        //发货仓库信息

        Warehouse warehouse = findWarehouseById(warehouseId);

        //塞入发货仓库信息
        ShopOrder shopOrder = orderDetail.getShopOrder();
        Map<String,String> extraMap = shopOrder.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            extraMap = Maps.newHashMap();
        }
        extraMap.put(TradeConstants.WAREHOUSE_ID,String.valueOf(warehouse.getId()));
        extraMap.put(TradeConstants.WAREHOUSE_NAME,warehouse.getName());
        shopOrder.setExtra(extraMap);

        return Response.ok(orderDetail);
    }


    private Map<Long, Integer> analysisSkuOrderIdAndQuantity(String data){
        Map<Long, Integer> skuOrderIdAndQuantity = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, Long.class, Integer.class));
        if(skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuOrderIdAndQuantity:{}",data);
            throw new JsonResponseException("sku.quantity.invalid");
        }

        return skuOrderIdAndQuantity;
    }

    private Warehouse findWarehouseById(Long warehouseId){
        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }

        return warehouseRes.getResult();
    }



    /**
     * 生成发货单
     * 发货成功：
     * 1. 更新子单的处理数量
     * 2. 更新子单的状态（如果子单全部为已处理则更新店铺订单为已处理）
     * @param shopOrderId 店铺订单id
     * @param data skuOrderId及数量 json格式
     * @param warehouseId          仓库id
     * @return 发货单id
     */
    @RequestMapping(value = "/api/order/{id}/ship", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createShipment(@PathVariable("id") Long shopOrderId,
                               @RequestParam("data") String data,
                               @RequestParam(value = "warehouseId") Long warehouseId) {
        Map<Long, Integer> skuOrderIdAndQuantity = analysisSkuOrderIdAndQuantity(data);

        //todo 封装商品信息到extra 下单店铺、绩效店铺
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);

        Map<String,Integer> skuCodeAndQuantityMap = skuOrders.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(SkuOrder::getSkuCode, it -> skuOrderIdAndQuantity.get(it.getId())));
        //检查库存是否充足
        checkStockIsEnough(warehouseId,skuCodeAndQuantityMap);


        Shipment shipment = new Shipment();
        shipment.setType(ShipmentType.SALES_SHIP.value());
        shipment.setSkuInfos(skuOrderIdAndQuantity);
        shipment.setReceiverInfos(findReceiverInfos(shopOrderId, OrderLevel.SHOP));

        //发货仓库信息

        Warehouse warehouse = findWarehouseById(warehouseId);
        Map<String,String> extraMap = Maps.newHashMap();
        extraMap.put(TradeConstants.WAREHOUSE_ID,String.valueOf(warehouse.getId()));
        extraMap.put(TradeConstants.WAREHOUSE_NAME,warehouse.getName());

        Response<Long> createResp = shipmentWriteService.create(shipment, Arrays.asList(shopOrderId), OrderLevel.SHOP);
        if (!createResp.isSuccess()) {
            log.error("fail to create shipment:{} for order(id={}),and level={},cause:{}",
                    shipment, shopOrderId, OrderLevel.SHOP.getValue(), createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }


        Long shipmentId = createResp.getResult();

        //eventBus.post(new OrderShipmentEvent(shipmentId)); todo 发货成功调用大度仓库接口减库存

        return shipmentId;

    }

    private String findReceiverInfos(Long orderId, OrderLevel orderLevel) {

        List<ReceiverInfo> receiverInfos = doFindReceiverInfos(orderId, orderLevel);

        if (CollectionUtils.isEmpty(receiverInfos)) {
            log.error("receiverInfo not found where orderId={}", orderId);
            throw new JsonResponseException("receiver.info.not.found");
        }

        ReceiverInfo receiverInfo = receiverInfos.get(0);

        try {
            return objectMapper.writeValueAsString(receiverInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<ReceiverInfo> doFindReceiverInfos(Long orderId, OrderLevel orderLevel) {
        Response<List<ReceiverInfo>> receiversResp = receiverInfoReadService.findByOrderId(orderId, orderLevel);
        if (!receiversResp.isSuccess()) {
            log.error("fail to find receiver info by order id={},and order level={},cause:{}",
                    orderId, orderLevel.getValue(), receiversResp.getError());
            throw new JsonResponseException(receiversResp.getError());
        }
        return receiversResp.getResult();
    }





    //获取指定仓库中指定商品的库存信息
    private Map<String, Integer> findStocksForSkus(Long warehouseId,List<String> skuCodes){
        Response<Map<String, Integer>> r = warehouseSkuReadService.findByWarehouseIdAndSkuCodes(warehouseId, skuCodes);
        if(!r.isSuccess()){
            log.error("failed to find stock in warehouse(id={}) for skuCodes:{}, error code:{}",
                    warehouseId, skuCodes, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    //检查库存是否充足
    private void checkStockIsEnough(Long warehouseId, Map<String,Integer> skuCodeAndQuantityMap){


        List<String> skuCodes = Lists.newArrayListWithCapacity(skuCodeAndQuantityMap.size());
        skuCodes.addAll(skuCodeAndQuantityMap.keySet());
        Map<String, Integer> warehouseStockInfo = findStocksForSkus(warehouseId,skuCodes);
        for (String skuCode : warehouseStockInfo.keySet()){
            if(warehouseStockInfo.get(skuCode)<skuCodeAndQuantityMap.get(skuCode)){
                log.error("sku code:{} warehouse stock:{} ship quantity:{} stock not enough",skuCode,warehouseStockInfo.get(skuCode),skuCodeAndQuantityMap.get(skuCode));
                throw new JsonResponseException(skuCode+".stock.not.enough");
            }
        }
    }
}
