package com.pousheng.middle.web.excel.supplyRule;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.common.utils.batchhandle.ItemSupplyRuleAbnormalRecord;
import com.pousheng.middle.task.api.QuerySingleTaskByIdRequest;
import com.pousheng.middle.task.api.UpdateTaskRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.excel.supplyRule.dto.ProcessDetailDTO;
import com.pousheng.middle.web.export.UploadFileComponent;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-12 10:49<br/>
 */
@Slf4j
public class ImportProgressStatus {
    private boolean started = Boolean.FALSE;
    private ScheduledExecutorService EXECUTOR;

    private final UploadFileComponent uploadFileComponent;
    private final TaskReadFacade taskReadFacade;
    private final TaskWriteFacade taskWriteFacade;
    private final Long taskId;

    private Long totalSize = null;
    private Long processedCount = 0L;
    private Long successCount = 0L;
    private Long failedCount = 0L;

    private int waitFlush = 0;
    private LinkedBlockingDeque<RowFail> fails = new LinkedBlockingDeque<>();

    public ImportProgressStatus(UploadFileComponent uploadFileComponent, TaskReadFacade taskReadFacade, TaskWriteFacade taskWriteFacade, Long taskId) {
        this.uploadFileComponent = uploadFileComponent;
        this.taskReadFacade = taskReadFacade;
        this.taskWriteFacade = taskWriteFacade;
        this.taskId = taskId;
    }

    public void success() {
        successCount += 1;
        processedCount += 1;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public void fail(List<String> rowValues, String reason, long rowCnt) {
        failedCount += 1;
        processedCount += 1;
        fails.offer(new RowFail(rowValues, reason, rowCnt));
    }

    public void start() {
        if (started) {
            return;
        }

        EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        EXECUTOR.scheduleWithFixedDelay(this::report, 12, 12, TimeUnit.SECONDS);
        started = Boolean.TRUE;
    }

    public void stop() {
        try {
            EXECUTOR.shutdown();
            EXECUTOR.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("[ImportProgressStatus] failed to stop process status, cause: {}", Throwables.getStackTraceAsString(e));
        }
    }

    public void flush() {
        report(true);
    }

    private void report() {
        report(false);
    }

    private void report(boolean force) {
        try {
            updateProgress();
            exportAbnormalRecord(force);
        } catch (Exception e) {
            log.error("[ImportProgressStatus] failed to handle progress status, cause: {}", Throwables.getStackTraceAsString(e));
        }
    }

    private void updateProgress() {
        String message = String.format("【已处理】%d,【成功】%d,【失败】%d", processedCount, successCount, failedCount);
        if (totalSize != null) {
            message = "【总行数】" + totalSize.toString() + "," + message;
        }

        updateTaskContent(ImmutableMap.of("message", message));
    }

    private void exportAbnormalRecord(boolean force) {
        // 每1000条错误，或者不满1000但是满1分钟导出一次
        if (!force) {
            waitFlush += 1;
            if (waitFlush < 5 && fails.size() < 1000) {
                return;
            } else {
                waitFlush = 0;
            }
        } else {
            waitFlush = 0;
        }

        if (fails.isEmpty()) {
            log.info("[ImportProgressStatus] fail message empty, nothing to export.");
            return;
        }

        // load current task
        Response<TaskDTO> r = taskReadFacade.querySingleTaskById(new QuerySingleTaskByIdRequest(taskId));
        if (!r.isSuccess()) {
            log.error("[ImportProgressStatus] failed to find task by id: {}, cause: {}", taskId, r.getError());
            throw new ServiceException(r.getError());
        } else if (r.getResult() == null) {
            throw new ServiceException("task.not.found");
        }
        TaskDTO task = r.getResult();

        // export & upload excel
        ExcelExportHelper<ItemSupplyRuleAbnormalRecord> helper = ExcelExportHelper.newExportHelper(ItemSupplyRuleAbnormalRecord.class);
        List<RowFail> allFails = Lists.newArrayList();
        fails.drainTo(allFails);

        log.info("[ImportProgressStatus] about to export {} message.", allFails.size());
        for (RowFail fail : allFails) {
            ItemSupplyRuleAbnormalRecord record = buildRecord(fail);
            if (record != null) {
                helper.appendToExcel(record);
            }
        }
        String fileName = DateTime.now().toString("yyyyMMddHHmmss") + ".xlsx";
        String abnormalUrl = uploadFileComponent.exportAbnormalRecord(helper.transformToFile(fileName));

        // append abnormal excel to file list
        List<Map<String, Object>> processDetails = (List) task.getContent().get("processDetails");
        if (CollectionUtils.isEmpty(processDetails)) {
            processDetails = Lists.newArrayList();
        }

        ProcessDetailDTO detail = new ProcessDetailDTO(fileName, abnormalUrl);
        try {
            processDetails.add(BeanMapper.convertObjectToMap(detail));
        } catch (Exception e) {
            log.error("[ImportProgressStatus] failed to convert detail {} to map, cause: {}", detail, Throwables.getStackTraceAsString(e));
        }
        task.getContent().put("processDetails", processDetails);
        updateTaskContent(task.getContent());
    }

    private ItemSupplyRuleAbnormalRecord buildRecord(RowFail fail) {
        try {
            List<String> str = fail.getValues();
            ItemSupplyRuleAbnormalRecord record = new ItemSupplyRuleAbnormalRecord();
            if (!StringUtils.isEmpty(str.get(0))) {
                record.setShop(str.get(0).replace("\"", ""));
            }
            if (!StringUtils.isEmpty(str.get(1))) {
                record.setMaterialCode(str.get(1).replace("\"", ""));
            }
            if (!StringUtils.isEmpty(str.get(2))) {
                record.setSkuCode(str.get(2).replace("\"", ""));
            }
            if (!StringUtils.isEmpty(str.get(3))) {
                record.setType(str.get(3).replace("\"", ""));
            }
            if (!StringUtils.isEmpty(str.get(4))) {
                record.setWarehouseCodes(str.get(4).replace("\"", ""));
            }
            if (!StringUtils.isEmpty(str.get(5))) {
                record.setStatus(str.get(5).replace("\"", ""));
            }
            record.setFailReason(fail.getReason());
            return record;
        } catch (Exception e) {
            log.error("[ImportProgressStatus] failed to create new fail record for {}, cause: {}", fail, Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    private void updateTaskContent(Map<String, Object> content) {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTaskId(taskId);
        request.setContent(content);


        Response<Boolean> r = taskWriteFacade.updateTask(request);
        if (!r.isSuccess()) {
            log.error("[ImportProgressStatus] failed to update task:{}, cause: {}", request, r.getError());
            throw new ServiceException(r.getError());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class RowFail {
        private List<String> values;
        private String reason;
        private long rowCnt;
    }
}
