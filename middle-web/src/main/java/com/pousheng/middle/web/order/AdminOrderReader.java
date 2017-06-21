package com.pousheng.middle.web.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@RestController
@Slf4j
public class AdminOrderReader {


    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;
    @Autowired
    private WarehouseReadService warehouseReadService;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    //订单分页
    @RequestMapping(value = "/api/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<ShopOrder>> findBy(MiddleOrderCriteria middleOrderCriteria) {

        return middleOrderReadService.pagingShopOrder(middleOrderCriteria);
    }


    //订单详情
    @RequestMapping(value = "/api/order/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<OrderDetail> detail(@PathVariable("id") Long id) {
        return orderReadLogic.orderDetail(id);
    }


    //订单待处理商品列表
    @RequestMapping(value = "/api/order/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<SkuOrder> waitHandleSku(@PathVariable("id") Long id) {
        return orderReadLogic.findSkuOrderByShopOrderIdAndStatus(id, MiddleOrderStatus.WAIT_HANDLE.getValue());
    }

    /**
     * 发货预览
     *
     * @param shopOrderId 店铺订单id
     * @param data skuOrderId及数量, 是List<SubmittedSku>的json表示形式
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

}