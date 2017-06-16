package com.pousheng.middle.web.order;

import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.OrderGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@Controller
@Slf4j
public class AdminOrderReader {

    @Autowired
    private OrderReadLogic orderReadLogic;

    @RequestMapping(value = "/api/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<Paging<OrderGroup>> findBy(@RequestParam Map<String, String> orderCriteria) {
        return orderReadLogic.pagingOrder(orderCriteria);
    }


    @RequestMapping(value = "/api/order/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<OrderDetail> detail(@PathVariable("id") Long id) {
        return orderReadLogic.orderDetail(id);
    }
}