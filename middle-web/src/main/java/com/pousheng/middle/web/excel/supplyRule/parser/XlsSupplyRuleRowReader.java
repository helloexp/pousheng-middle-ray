package com.pousheng.middle.web.excel.supplyRule.parser;

import com.pousheng.middle.web.excel.supplyRule.ImportProgressStatus;
import com.pousheng.middle.web.excel.supplyRule.dto.SupplyRuleDTO;
import io.terminus.excel.read.ExcelEventRowReader;

import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-10 15:32<br/>
 */
public class XlsSupplyRuleRowReader implements ExcelEventRowReader<SupplyRuleDTO> {
    private final SupplyRuleRowBuildLogic buildLogic;
    private final ImportProgressStatus processStatus;

    public XlsSupplyRuleRowReader(SupplyRuleRowBuildLogic buildLogic, ImportProgressStatus processStatus) {
        this.buildLogic = buildLogic;
        this.processStatus = processStatus;
    }

    @Override
    public SupplyRuleDTO getRows(int sheetIndex, int curRow, List<String> rValues) {
        if (curRow == 1) {
            return null;
        }

        SupplyRuleDTO supplyRuleDTO = buildLogic.build(curRow, rValues, processStatus);
        if (supplyRuleDTO != null) {
            supplyRuleDTO.setRowNo(curRow);
        }
        return supplyRuleDTO;
    }
}
