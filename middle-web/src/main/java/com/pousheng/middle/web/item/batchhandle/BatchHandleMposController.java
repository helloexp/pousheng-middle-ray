package com.pousheng.middle.web.item.batchhandle;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@Slf4j
public class BatchHandleMposController {

    @Autowired
    private BatchHandleMposLogic batchHandleMposLogic;

    /**
     * mpos导入文件记录
     * @return
     */
    @RequestMapping(value = "/api/sku-template/batch/import/records",method = RequestMethod.GET)
    public List<BatchHandleRecord> importRecord(){
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-IMPORT-RECORDS-START noparam: ");
        }
        Response<List<BatchHandleRecord>> response = batchHandleMposLogic.getImportFileRecord();
        if(!response.isSuccess()){
            log.error("fail to find import record,cause:{}",response.getError());
            throw new JsonResponseException(response.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-IMPORT-RECORDS-START noparam: ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(response.getResult()));
        }
        return response.getResult();
    }

    /**
     * mpos导出文件记录
     * @return
     */
    @RequestMapping(value = "/api/sku-template/batch/export/records",method = RequestMethod.GET)
    public List<BatchHandleRecord> exportRecord(){
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-EXPORT-RECORDS-START noparam: ");
        }
        Response<List<BatchHandleRecord>> response = batchHandleMposLogic.getExportFileRecord();
        if(!response.isSuccess()){
            log.error("fail to find import record,cause:{}",response.getError());
            throw new JsonResponseException(response.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-EXPORT-RECORDS-END noparam: ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(response.getResult()));
        }
        return response.getResult();
    }

    /**
     * 打标，取消打标记录
     * @return
     */
    @RequestMapping(value = "/api/sku-template/batch/handle/mpos/flag/records",method = RequestMethod.GET)
    public List<BatchHandleRecord> batchMposFlagRecord(){
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-HANDLE-MPOS-FLAG-RECORDS-START noparam: ");
        }
        Response<List<BatchHandleRecord>> response = batchHandleMposLogic.getMposFlagRecord();
        if(!response.isSuccess()){
            log.error("fail to get mpos falg record cause:{}",response.getError());
            throw new JsonResponseException(response.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-HANDLE-MPOS-FLAG-RECORDS-END noparam: ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(response.getResult()));
        }
        return response.getResult();
    }

}
