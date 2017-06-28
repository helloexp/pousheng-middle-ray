package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.MiddleRefundDetail;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Created by songrenfei on 2017/6/26
 */
@RestController
@Slf4j
public class Refunds {

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    //逆向单分页
    @RequestMapping(value = "/api/refund/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<RefundPaging> findBy(RefundCriteria criteria) {

        Response<Paging<RefundPaging>> pagingRes = refundReadLogic.refundPaging(criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging refund by criteria:{} fail,error:{}",criteria,pagingRes.getError());
            throw new JsonResponseException(pagingRes.getError());
        }

        return pagingRes.getResult();
    }


    //逆向单详情
    @RequestMapping(value = "/api/refund/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public MiddleRefundDetail detail(@PathVariable(value = "id") Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        MiddleRefundDetail refundDetail = new MiddleRefundDetail();
        refundDetail.setOrderRefund(orderRefund);
        refundDetail.setRefund(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        refundDetail.setRefundItems(refundReadLogic.findRefundItems(refund));
        refundDetail.setRefundExtra(refundExtra);

        //如果为换货,则封装发货信息
        if(Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())&& Arguments.notNull(refundExtra.getShipmentId())){
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentById(refundExtra.getShipmentId());
            Shipment shipment = shipmentReadLogic.findShipmentById(orderShipment.getShipmentId());
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            refundDetail.setShipmentItems(shipmentItems);
            refundDetail.setOrderShipments(Lists.newArrayList(orderShipment));
        }

        return refundDetail;
    }

}
