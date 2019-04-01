package com.pousheng.middle.web.task;

import com.pousheng.middle.task.dto.TaskSearchCriteria;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskReadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.UserUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhaoxw
 * @date 2018/5/12
 */

@Slf4j
@RestController
@RequestMapping("/api/schedule/task")
@Api(description = "异步任务")
public class ScheduleTasks {

    @RpcConsumer
    @Setter
    ScheduleTaskReadService scheduleTaskReadService;

    @ApiOperation("根据条件查询任务记录")
    @GetMapping("/paging")
    public Paging<ScheduleTask> findBy(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                       @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                       @RequestParam(required = false, value = "businessId") Long businessId,
                                       @RequestParam(required = false, value = "businessType") Integer businessType,
                                       @RequestParam Integer type) {
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        criteria.setType(type);
        criteria.setBusinessId(businessId);
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setBusinessType(businessType);
        criteria.setUserId(UserUtil.getUserId());
        Response<Paging<ScheduleTask>> r = scheduleTaskReadService.paging(criteria);
        if (!r.isSuccess()) {
            log.error("failed to pagination schedule tasks, error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

}
