package com.pousheng.middle.web.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.ShopOrderWithReceiveInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;


    //订单分页
    @RequestMapping(value = "/api/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<ShopOrder>> findBy(MiddleOrderCriteria middleOrderCriteria) {

        return middleOrderReadService.pagingShopOrder(middleOrderCriteria);
    }


    //订单详情
    @RequestMapping(value = "/api/order/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OrderDetail> detail(@PathVariable("id") Long id) {
        return orderReadLogic.orderDetail(id);
    }


    //订单待处理商品列表
    @RequestMapping(value = "/api/order/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkuOrder> waitHandleSku(@PathVariable("id") Long id) {
        return orderReadLogic.findSkuOrderByShopOrderIdAndStatus(id, MiddleOrderStatus.WAIT_HANDLE.getValue());
    }


    //新建售后订单 for 展示订单信息
    @RequestMapping(value = "/api/order/{id}/for/after/sale", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ShopOrderWithReceiveInfo> afterSaleOrderInfo(@PathVariable("id") Long id) {

        ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
        Response<List<ReceiverInfo>> response = receiverInfoReadService.findByOrderId(id, OrderLevel.SHOP);
        if(!response.isSuccess()){
            log.error("find order receive info by order id:{} fial,error:{}",id,response.getError());
            return Response.fail(response.getError());
        }
        List<ReceiverInfo> receiverInfos = response.getResult();
        if(CollectionUtils.isEmpty(receiverInfos)){
            log.error("not find receive info by order id:{}",id);
            return Response.fail("order.receive.info.not.exist");
        }

        ShopOrderWithReceiveInfo withReceiveInfo = new ShopOrderWithReceiveInfo();
        withReceiveInfo.setShopOrder(shopOrder);
        withReceiveInfo.setReceiverInfo(receiverInfos.get(0));

        return Response.ok(withReceiveInfo);
    }


}