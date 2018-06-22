package com.pousheng.middle.web.operationlog;

import com.pousheng.middle.order.dto.OperationLogCriteria;
import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.service.OperationLogReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Created by sunbo@terminus.io on 2017/8/2.
 */
@RestController
@RequestMapping("api/operationlog/")
@Slf4j
public class OperationLogController {


    @Autowired
    private OperationLogReadService operationLogReadService;


    @GetMapping("paging")
    public Response<Paging<OperationLog>> findBy(OperationLogCriteria criteria) {
        if(log.isDebugEnabled()){
            log.debug("API-OPERATIONLOG-PAGING-START param: criteria [{}] ",JsonMapper.nonEmptyMapper().toJson(criteria));
        }
        Response<Paging<OperationLog>> response = operationLogReadService.paging(criteria);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATIONLOG-PAGING-END param: criteria [{}] ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(criteria),JsonMapper.nonEmptyMapper().toJson(response));
        }
        return response;
    }

    @GetMapping("{id}")
    public Response<OperationLog> detail(@PathVariable Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-OPERATIONLOG-DETAIL-START param: id [{}] ",id);
        }
        Response<OperationLog> response = operationLogReadService.findById(id);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATIONLOG-DETAIL-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(response));
        }
        return response;
    }

}

