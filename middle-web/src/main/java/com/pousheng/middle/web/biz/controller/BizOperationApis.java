/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.biz.controller;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.swagger.annotations.Api;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author:  songrenfeiu
 * Date: 2018-11-14
 */
@Api(description = "bizAPI")
@Slf4j
@RestController
@RequestMapping("/api/biz")
public class BizOperationApis {
    // Biz type constants
    private static String THIRD_ORDER_PICKUP_WAIT_HANDLE = "THIRD_ORDER_PICKUP_WAIT_HANDLE";
    private static String THIRD_ORDER_CREATE_SHIP = "THIRD_ORDER_CREATE_SHIP";
    private static String THIRD_ORDER_CREATE_SHIP_WAIT_HANDLE = "THIRD_ORDER_CREATE_SHIP_WAIT_HANDLE";

    @Autowired
    private PoushengCompensateBizWriteService compensateBizWriteService;

    @Autowired
    private CompensateBizLogic compensateBizLogic;


    /**
     * 更新bi状态
     * @param id id
     * @param currentStatus 当前状态
     * @param newStatus 新状态
     * @return 更新结果
     */
    @RequestMapping(value = "/update/status", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> updateStatus(@RequestParam Long id,@RequestParam String currentStatus, @RequestParam String newStatus) {
        if (log.isDebugEnabled()){
            log.debug("API-UPDATE-BIZ-STATUS param: id {} current status:{} new status:{}",id,currentStatus,newStatus);
        }
        //乐观锁控制更新为处理中
        return compensateBizWriteService.updateStatus(id,currentStatus, newStatus);
    }

    /**
     * 更新bi状态, 通过context（一般为orderid等其他id信息）
     * 
     * @param context       context
     * @param currentStatus 当前状态
     * @param newStatus     新状态
     * @return 更新结果
     */
    @RequestMapping(value = "/update/third_order_create_shipment_biz_status", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> updateThirdOrderCreateShipmentStatus(@RequestParam Long context,
            @RequestParam String currentStatus,
            @RequestParam String newStatus) {
        if (log.isDebugEnabled()) {
            log.debug("API-UPDATE-BIZ-STATUS param: id {} current status:{} new status:{}", context, currentStatus,
                    newStatus);
        }

        return compensateBizWriteService.updateStatusByContextInTwoHours(String.valueOf(context), currentStatus,
                newStatus, PoushengCompensateBizType.THIRD_ORDER_CREATE_SHIP.toString());
    }

    /**
     * 更新Biz type
     * 
     * @param context
     * @param currentBizType
     * @param newBizType
     * @return
     */
    @RequestMapping(value = "/update/biztype", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<String> updateBizType(@RequestParam Long context, @RequestParam String currentBizType,
            @RequestParam String newBizType) {
        if (log.isDebugEnabled()) {
            log.debug("API-UPDATE-BIZ-TYPE param: context {} current biz_type:{} new biz_type:{}", context,
                    currentBizType, newBizType);
        }
        if (context == null || currentBizType == null || newBizType == null) {
            return Response.fail("update.biz.type.params.invalid");
        }
        // 更新状态api最初针对京东自提单给出， 京东渠道的订单创建后会写一个THIRD_ORDER_CREATE_SHIP_WAIT_HANDLE
        // type的biz任务，待通过api确认订单是否是自提单后再调用此api更新biz
        // type，非自提单改为正常type走正常派单流程，自提单改为自提单独有type
        if (!(THIRD_ORDER_PICKUP_WAIT_HANDLE.equals(newBizType) || THIRD_ORDER_CREATE_SHIP.equals(newBizType))) {
            return Response.fail("update.biz.type.newtype.invalid");
        }
        if (!THIRD_ORDER_CREATE_SHIP_WAIT_HANDLE.equals(currentBizType)) {
            return Response.fail("update.biz.type.currenttype.invalid");
        }
        return compensateBizWriteService.updateBizTypeByContextOnlyIfOfWaitHandleStatus(context, currentBizType,
                newBizType);
    }


    /**
     * 更新失败原因和次数
     * @return 是否成功
     */
    @RequestMapping(value = "/update/fail/reason", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> updateFailReason(@RequestParam Long id,@RequestParam String failReason,@RequestParam Integer count) {

        if (log.isDebugEnabled()){
            log.debug("API-UPDATE-BIZ-FAIL-REASON param: id {} fail reason :{} count:{}",id,failReason,count);
        }
        return compensateBizWriteService.updateLastFailedReason(id,failReason,count);
    }


    /**
     * 消费Biz
     * @param id id
     * @return 更新结果
     */
    @RequestMapping(value = "/consume", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void consume(@RequestParam String id) {
        log.debug("API-CONSUME-BIZ param: id {} ",id);
        compensateBizLogic.consumeMqMessage(id);
    }
}
