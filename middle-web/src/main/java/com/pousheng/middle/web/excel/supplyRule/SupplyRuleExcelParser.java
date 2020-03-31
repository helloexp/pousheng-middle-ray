package com.pousheng.middle.web.excel.supplyRule;

import com.google.common.base.Throwables;
import com.pousheng.middle.common.utils.component.AzureOSSBlobClient;
import com.pousheng.middle.common.utils.component.FileUtils;
import com.pousheng.middle.task.api.UpdateTaskRequest;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.excel.TaskBehaviour;
import com.pousheng.middle.web.excel.supplyRule.dto.SupplyRuleDTO;
import com.pousheng.middle.web.excel.supplyRule.factory.ProcessStatusFactory;
import com.pousheng.middle.web.excel.supplyRule.factory.RowExecuteHandlerFactory;
import com.pousheng.middle.web.excel.supplyRule.factory.RowReaderFactory;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.excel.exception.ExcelReadException;
import io.terminus.excel.read.ExcelEventReader;
import io.terminus.excel.read.ExcelReaderFactory;
import io.terminus.excel.read.handler.impl.ExcelEventXlsxParseHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-10 16:22<br/>
 */
@Slf4j
public class SupplyRuleExcelParser implements TaskBehaviour {
    private final Long taskId;
    private final String url;
    private final Boolean delta;
    private final AzureOSSBlobClient ossBlobClient;
    private final TaskWriteFacade taskWriteFacade;
    private final FileUtils fileUtils;

    private File file;
    private boolean stopped = false;

    public SupplyRuleExcelParser(Long taskId, String url, Boolean delta, FileUtils fileUtils, AzureOSSBlobClient ossBlobClient, TaskWriteFacade taskWriteFacade) {
        this.taskId = taskId;
        this.url = url;
        this.delta = delta;
        this.fileUtils = fileUtils;
        this.ossBlobClient = ossBlobClient;
        this.taskWriteFacade = taskWriteFacade;
    }

    private ImportProgressStatus processStatus;
    private ExcelEventReader<SupplyRuleDTO> excelReader;

    /**
     * 任务准备执行时的回调方法，让任务准备前置数据
     */
    @Override
    public void preStart() {
        String filePath = fileUtils.downloadUrl("temp-data", "supply-rule-import-", url);
        if (filePath == null) {
            return;
        }

        file = new File(filePath);
        log.info("[SUPPLY-RULE-EXCEL-PARSER] start process excel: {}, size: {}", filePath, file.length());
        processStatus = ProcessStatusFactory.get(taskId);

        excelReader = ExcelReaderFactory.createExcelEventReader(url);
        excelReader.setExecuteHandler(RowExecuteHandlerFactory.get(delta, processStatus));

        if (url.toLowerCase().endsWith("xlsx")) {
            ((ExcelEventXlsxParseHandler<SupplyRuleDTO>) excelReader).setRowReader(RowReaderFactory.getXlsxReader(processStatus));
        } else {
            excelReader.setRowReader(RowReaderFactory.getXlsReader(processStatus));
        }
        updateTaskStatus(TaskStatusEnum.EXECUTING);
    }

    @Override
    public void start() {
        parse();
    }

    public void parse() {
        try (FileInputStream bis = new FileInputStream(file)) {
            processStatus.start();
            excelReader.process(bis);
        } catch (ExcelReadException e) {
            if ("task.has.bean.stopped".equals(e.getMessage())) {
                log.info("excel import manual stopped.");
            } else {
                throw e;
            }
        } catch (IOException e) {
            log.error("[SUPPLY-RULE-EXCEL-PARSER] failed to open file {} for read, cause: {}", url, Throwables.getStackTraceAsString(e));
        } catch (Exception e) {
            log.error("[SUPPLY-RULE-EXCEL-PARSER] failed to parse excel file {} for read, cause: {}", url, Throwables.getStackTraceAsString(e));
            throw e;
        }
    }

    @Override
    public boolean alive() {  return  true; }

    /**
     * 任务执行完毕时的回调方法，让任务感知到被中止了
     */
    @Override
    public void onStop() {
        log.info("[SUPPLY-RULE-EXCEL-PARSER] parse stopped.");
        if (excelReader != null) {
            excelReader.stop();
        }
        this.stopped = true;
        updateTaskStatus(TaskStatusEnum.STOPPED);
    }

    /**
     * 任务执异常的回调方法
     */
    @Override
    public void onError() {
        log.info("[SUPPLY-RULE-EXCEL-PARSER] oops, error happened.");
        processStatus.stop();
        processStatus.flush();
        updateTaskStatus(TaskStatusEnum.ERROR);
    }

    @Override
    public void postStop() {
        log.info("[SUPPLY-RULE-EXCEL-PARSER] parse finished.");
        processStatus.stop();
        processStatus.flush();
        // 手动听停止则不再更新状态
        if (!stopped) {
            updateTaskStatus(TaskStatusEnum.FINISH);
        }
    }

    private void updateTaskStatus(TaskStatusEnum statusEnum) {
        log.info("[SUPPLY-RULE-EXCEL-PARSER] update task {} status to {}", taskId, statusEnum);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTaskId(taskId);
        request.setStatus(statusEnum.name());
        Response<Boolean> r = taskWriteFacade.updateTask(request);
        if (!r.isSuccess()) {
            log.error("failed to update task({}) status to {}, cause: {}", taskId, statusEnum, r.getError());
            throw new ServiceException(r.getError());
        }
    }
}
