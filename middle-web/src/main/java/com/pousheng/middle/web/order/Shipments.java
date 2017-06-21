package com.pousheng.middle.web.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.ShipmentDto;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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



    //发货单分页
    @RequestMapping(value = "/api/shipment/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<ShipmentDto> findBy(@RequestParam Map<String, String> shipmentCriteria) {

        OrderShipmentCriteria criteria = objectMapper.convertValue(shipmentCriteria, OrderShipmentCriteria.class);

        Response<Paging<ShipmentDto>> response =  orderShipmentReadService.findBy(criteria);
        if(!response.isSuccess()){
            log.error("find shipment by criteria:{} fail,error:{}",criteria,response.getError());
            throw new JsonResponseException(response.getError());
        }

        return response.getResult();
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



}
