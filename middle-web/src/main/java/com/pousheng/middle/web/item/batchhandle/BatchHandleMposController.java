package com.pousheng.middle.web.item.batchhandle;

import com.pousheng.middle.web.utils.export.FileRecord;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
        Response<List<BatchHandleRecord>> response = batchHandleMposLogic.getImportFileRecord();
        if(!response.isSuccess()){
            log.error("fail to find import record,cause:{}",response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * mpos导出文件记录
     * @return
     */
    @RequestMapping(value = "/api/sku-template/batch/export/records",method = RequestMethod.GET)
    public List<FileRecord> exportRecord(){
        Response<List<FileRecord>> response = batchHandleMposLogic.getExportFileRecord();
        if(!response.isSuccess()){
            log.error("fail to find import record,cause:{}",response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 打标，取消打标记录
     * @return
     */
    @RequestMapping(value = "/api/sku-template/batch/handle/mpos/flag/records",method = RequestMethod.GET)
    public List<BatchHandleRecord> batchMposFlagRecord(){
        Response<List<BatchHandleRecord>> response = batchHandleMposLogic.getMposFlagRecord();
        if(!response.isSuccess()){
            log.error("fail to get mpos falg record cause:{}",response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 查看导入文件异常记录
     * @param taskId
     * @return
     */
    @RequestMapping(value = "/api/sku-template/batch/import/abnormal/records")
    public Paging<AbnormalRecord> importFileAbnormalRecords(String taskId, @RequestParam(required = false,defaultValue = "1") Integer pageNo,@RequestParam(required = false,defaultValue = "10") Integer pageSize){
        Response<Paging<AbnormalRecord>> response = batchHandleMposLogic.getMposAbnormalRecord(taskId,pageNo,pageSize,"import");
        if(!response.isSuccess()){
            log.error("fail to get import file abnormal records,id={}",taskId);
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 查看打标，取消打标异常记录
     * @param taskId
     * @return
     */
    @RequestMapping(value = "/api/sku-template/batch/handle/mpos/flag/abnormal/records")
    public Paging<AbnormalRecord> mposAbnormalRecords(@RequestParam String taskId, @RequestParam(required = false,defaultValue = "1") Integer pageNo,@RequestParam(required = false,defaultValue = "10") Integer pageSize){
        Response<Paging<AbnormalRecord>> response = batchHandleMposLogic.getMposAbnormalRecord(taskId,pageNo,pageSize,"flag");
        if(!response.isSuccess()){
            log.error("fail to find batch handle mpos flage by taskId={},cause:{}",taskId,response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

}
