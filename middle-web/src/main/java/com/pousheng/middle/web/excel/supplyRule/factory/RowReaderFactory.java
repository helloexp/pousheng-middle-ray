package com.pousheng.middle.web.excel.supplyRule.factory;

import com.pousheng.middle.web.excel.supplyRule.ImportProgressStatus;
import com.pousheng.middle.web.excel.supplyRule.parser.SupplyRuleRowBuildLogic;
import com.pousheng.middle.web.excel.supplyRule.parser.XlsSupplyRuleRowReader;
import com.pousheng.middle.web.excel.supplyRule.parser.XlsxSupplyRuleRowReader;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-11 11:37<br/>
 */
@Component
public class RowReaderFactory {
    private static SupplyRuleRowBuildLogic buildLogic;

    public RowReaderFactory(SupplyRuleRowBuildLogic buildLogic) {
        RowReaderFactory.buildLogic = buildLogic;
    }

    public static XlsSupplyRuleRowReader getXlsReader(ImportProgressStatus processStatus) {
        return new XlsSupplyRuleRowReader(buildLogic, processStatus);
    }

    public static XlsxSupplyRuleRowReader getXlsxReader(ImportProgressStatus processStatus) {
        return new XlsxSupplyRuleRowReader(buildLogic, processStatus);
    }
}
