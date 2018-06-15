/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: NotifyHkOrderDoneLogic
 * Author:   xiehong
 * Date:     2018/5/30 下午3:43
 * Description: 电商状态为已确认，通知恒康发单收货时间
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.open.component;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 〈电商状态为已确认，通知恒康发单收货时间〉
 *
 * @author xiehong
 * @create 2018/5/30 下午3:43
 */
@Slf4j
@Component
public class NotifyHkOrderDoneLogic {

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    /**
     * 创建通知恒康发货单收货时间任务
     *
     * @param shopOrderId
     * @return:
     * @Author:xiehong
     * @Date: 2018/5/30 下午4:37
     */
    public Response<Long> ctreateNotifyHkOrderDoneTask(Long shopOrderId) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.NOTIFY_HK_ORDER_DOWN.toString());
        biz.setContext(String.valueOf(shopOrderId));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);
    }

    /**
     * 创建同步通知mpos发货单确认收货的任务
     * @param shopOrderId
     * @return
     */
    public Response<Boolean> createNotifyMposDoneTask(Long shopOrderId){
        try{
            List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrderId);
            //获取已发货的发货单并且是店发的发货单
            List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                    .filter(orderShipment -> Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue()))
                    .filter(orderShipment -> Objects.equals(orderShipment.getShipWay(),1))
                    .collect(Collectors.toList());
            for (OrderShipment orderShipment:orderShipmentsFilter){
                PoushengCompensateBiz biz = new PoushengCompensateBiz();
                biz.setBizType(PoushengCompensateBizType.SYNC_MPOS_CONFIRM_DONE.name());
                biz.setBizId(String.valueOf(orderShipment.getShipmentId()));
                biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
                poushengCompensateBizWriteService.create(biz);
            }
            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("create notify mpos shipment done task failed,shopOrderId is {},caused by{}",shopOrderId, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }
}