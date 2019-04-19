package com.pousheng.middle.web.excel.supplyRule.parser;

import com.pousheng.middle.web.excel.supplyRule.ImportProgressStatus;
import com.pousheng.middle.web.excel.supplyRule.dto.SupplyRuleDTO;
import io.terminus.excel.read.ExcelEventRowWithCountReader;

import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-10 15:32<br/>
 */
public class XlsxSupplyRuleRowReader implements ExcelEventRowWithCountReader<SupplyRuleDTO> {
    private final SupplyRuleRowBuildLogic supplyRuleRowBuildLogic;
    private final ImportProgressStatus processStatus;

    public XlsxSupplyRuleRowReader(SupplyRuleRowBuildLogic supplyRuleRowBuildLogic, ImportProgressStatus processStatus) {
        this.supplyRuleRowBuildLogic = supplyRuleRowBuildLogic;
        this.processStatus = processStatus;
    }

    @Override
    public SupplyRuleDTO getRows(int sheetIndex, int curRow, long rowCount, List<String> rValues) {
        if (curRow == 1) {
            processStatus.setTotalSize(rowCount - 1);
            return null;
        }

        SupplyRuleDTO supplyRuleDTO = supplyRuleRowBuildLogic.build(curRow, rValues, processStatus);
        if (supplyRuleDTO != null) {
            supplyRuleDTO.setRowNo(curRow);
        }
        return supplyRuleDTO;
    }
}
