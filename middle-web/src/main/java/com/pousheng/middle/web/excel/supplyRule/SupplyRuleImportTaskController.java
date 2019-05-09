package com.pousheng.middle.web.excel.supplyRule;

import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.task.api.CreateTaskRequest;
import com.pousheng.middle.task.api.PagingTaskRequest;
import com.pousheng.middle.task.api.QuerySingleTaskByIdRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.impl.converter.CommonConverter;
import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.excel.MQTaskProducer;
import com.pousheng.middle.web.excel.supplyRule.dto.ProcessDetailDTO;
import com.pousheng.middle.web.excel.supplyRule.dto.SupplyRuleImportTaskDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 10:32<br/>
 */
@Api(tags = "发货限制规则导入接口")
@Slf4j
@RestController
@RequestMapping("/api/item-supply/v2")
public class SupplyRuleImportTaskController {
    private final TaskWriteFacade taskWriteFacade;
    private final TaskReadFacade taskReadFacade;
    private final MQTaskProducer mqTaskProducer;

    public SupplyRuleImportTaskController(TaskWriteFacade taskWriteFacade,
                                          TaskReadFacade taskReadFacade,
                                          MQTaskProducer mqTaskProducer) {
        this.taskWriteFacade = taskWriteFacade;
        this.taskReadFacade = taskReadFacade;
        this.mqTaskProducer = mqTaskProducer;
    }

    /**
     * 导入模板
     */
    @PostMapping("/import")
    public long importExcel(@RequestBody SkuStockRuleImportInfo info) {
        if (StringUtils.isEmpty(info.getFilePath())) {
            throw new JsonResponseException("import.file.path.empty");
        }

        log.info("about to create import task: {}", info);
        CreateTaskRequest request = new CreateTaskRequest();
        request.setStatus(TaskStatusEnum.INIT.name());
        request.setType(TaskTypeEnum.SUPPLY_RULE_IMPORT.name());

        SupplyRuleImportTaskDTO taskDTO = new SupplyRuleImportTaskDTO();
        taskDTO.setFileName(info.getFileName());
        taskDTO.setFilePath(info.getFilePath());
        taskDTO.setDelta(Objects.equals(info.getDelta(), Boolean.TRUE));
        taskDTO.setStatus(request.getStatus());
        try {
            request.setDetail(BeanMapper.convertObjectToMap(taskDTO));
        } catch (Exception e) {
            // ignore
        }
        request.setContent(Collections.emptyMap());
        Response<Long> r = taskWriteFacade.createTask(request);
        if (!r.isSuccess()) {
            log.error("failed to create import task {}, cause: {}", request, r.getError());
            throw new JsonResponseException(r.getError());
        }

        Response<TaskDTO> found = taskReadFacade.querySingleTaskById(new QuerySingleTaskByIdRequest(r.getResult()));
        if (!found.isSuccess()) {
            log.error("failed to query task by id: {}, cause: {}", r.getResult(), found.getError());
            throw new JsonResponseException(found.getError());
        }
        log.info("about to submit import task: {}", found.getResult());
        mqTaskProducer.sendStartMessage(found.getResult());
        return found.getResult().getId();
    }

    @ApiOperation("停止指定任务")
    @GetMapping("/stop")
    public boolean stop(@ApiParam("任务 ID")
                        @RequestParam("id") Long taskId,
                        @ApiParam("超时时间，默认10秒，0代表等待直到任务退出")
                        @RequestParam(value = "timeout", defaultValue = "30", required = false) Long timeout) {
        mqTaskProducer.sendStopMessage(taskId, timeout);
        return true;
    }

    @ApiOperation("分页查询")
    @GetMapping("/paging")
    public Paging<SupplyRuleImportTaskDTO> paging(@RequestParam(required = false, defaultValue = "SUPPLY_RULE_IMPORT") String type,
                                                  @RequestParam(required = false, value = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(required = false, value = "pageSize", defaultValue = "20") Integer pageSize) {
        PagingTaskRequest request = new PagingTaskRequest();
        request.setPageSize(pageSize);
        request.setPageNo(pageNo);
        request.setType(type);
        Response<Paging<TaskDTO>> r = taskReadFacade.pagingTasks(request);
        if (!r.isSuccess()) {
            log.error("failed to paging task, cause: {}", r.getResult());
        }

        return CommonConverter.batchConvert(r.getResult(), this::convert);
    }

    private SupplyRuleImportTaskDTO convert(TaskDTO taskDTO) {
        SupplyRuleImportTaskDTO d = new SupplyRuleImportTaskDTO();
        d.setId(taskDTO.getId());
        d.setStatus(taskDTO.getStatus());
        d.setCreatedAt(taskDTO.getCreatedAt());
        d.setUpdatedAt(taskDTO.getCreatedAt());
        d.setFileName((String) taskDTO.getDetail().get("fileName"));
        d.setFilePath((String) taskDTO.getDetail().get("filePath"));
        d.setMessage((String) taskDTO.getContent().get("message"));
        d.setProcessDetails(
                CommonConverter.batchConvert(
                        (List<Map<String, Object>>) taskDTO.getContent().getOrDefault("processDetails", Collections.emptyList()),
                        this::convertProcessDetail));
        return d;
    }

    private ProcessDetailDTO convertProcessDetail(Map<String, Object> m) {
        if (CollectionUtils.isEmpty(m)) {
            return null;
        }
        ProcessDetailDTO d = new ProcessDetailDTO();
        d.setFileName((String) m.get("fileName"));
        d.setFilePath((String) m.get("filePath"));
        return d;
    }
}
