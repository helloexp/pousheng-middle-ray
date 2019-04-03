package com.pousheng.middle.web.excel.supplyRule.factory;

import com.pousheng.middle.common.utils.component.AzureOSSBlobClient;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.excel.supplyRule.SupplyRuleExcelParser;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-11 11:45<br/>
 */
@Component
public class ExcelParserFactory {
    private static AzureOSSBlobClient azureOSSBlobClient;
    private static TaskWriteFacade taskWriteFacade;

    public ExcelParserFactory(AzureOSSBlobClient azureOSSBlobClient, TaskWriteFacade taskWriteFacade) {
        ExcelParserFactory.azureOSSBlobClient = azureOSSBlobClient;
        ExcelParserFactory.taskWriteFacade = taskWriteFacade;
    }

    public static SupplyRuleExcelParser get(Long taskId, String url, Boolean delta) {
        return new SupplyRuleExcelParser(taskId, url, delta, azureOSSBlobClient, taskWriteFacade);
    }
}
