package com.pousheng.middle.web.excel.supplyRule.factory;

import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.excel.supplyRule.ImportProgressStatus;
import com.pousheng.middle.web.export.UploadFileComponent;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-12 10:57<br/>
 */
@Component
public class ProcessStatusFactory {
    private static UploadFileComponent uploadFileComponent;
    private static TaskReadFacade taskReadFacade;
    private static TaskWriteFacade taskWriteFacade;

    public ProcessStatusFactory(UploadFileComponent uploadFileComponent, TaskReadFacade taskReadFacade, TaskWriteFacade taskWriteFacade) {
        ProcessStatusFactory.uploadFileComponent = uploadFileComponent;
        ProcessStatusFactory.taskReadFacade = taskReadFacade;
        ProcessStatusFactory.taskWriteFacade = taskWriteFacade;
    }

    public static ImportProgressStatus get(Long taskId) {
        return new ImportProgressStatus(uploadFileComponent, taskReadFacade, taskWriteFacade, taskId);
    }
}
