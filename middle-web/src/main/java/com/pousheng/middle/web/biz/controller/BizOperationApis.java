/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.biz.controller;

import com.pousheng.middle.mq.component.CompensateBizLogic;
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
