package com.pousheng.middle.web.operationlog;

import com.pousheng.middle.order.dto.OperationLogCriteria;
import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.service.OperationLogReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Created by sunbo@terminus.io on 2017/8/2.
 */
@RestController
@RequestMapping("api/operationlog/")
public class OperationLogController {


    @Autowired
    private OperationLogReadService operationLogReadService;


    @GetMapping("paging")
    public Response<Paging<OperationLog>> findBy(OperationLogCriteria criteria) {

        return operationLogReadService.paging(criteria);
    }

}

