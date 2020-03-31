package com.pousheng.middle.web.warehouses.component;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.common.utils.component.FileUtils;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.warehouses.dto.StockSendImprtDTO;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.Joiners;
import io.terminus.excel.read.ExcelEventReader;
import io.terminus.excel.read.ExcelEventRowReader;
import io.terminus.excel.read.ExcelReaderFactory;
import io.terminus.excel.read.handler.AbstractExecuteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-20 11:45<br/>
 */
@Slf4j
@Component
public class StockSendRuleExcelParser {
    private final FileUtils fileUtils;
    private final WarehouseCacher warehouseCacher;

    public StockSendRuleExcelParser(FileUtils fileUtils, WarehouseCacher warehouseCacher) {
        this.fileUtils = fileUtils;
        this.warehouseCacher = warehouseCacher;
    }

    public List<StockSendImprtDTO> parse(String url) {
        String filePath = fileUtils.downloadUrl("temp-data", "stock-send-", url);
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);

        log.info("[SUPPLY-RULE-EXCEL-PARSER] start process excel: {}, size: {}", filePath, file.length());

        List<StockSendImprtDTO> data = Lists.newArrayList();
        XlsRowReader rowReader = new XlsRowReader();
        ExcelEventReader<StockSendImprtDTO> excelReader = ExcelReaderFactory.createExcelEventReader(url);

        excelReader.setRowReader(rowReader);
        excelReader.setExecuteHandler(new ExecuteHandler(data));

        try (FileInputStream fis = new FileInputStream(file)) {
            excelReader.process(fis);
        } catch (Exception e) {
            log.error("failed to process excel {}/{}, cause: {}", filePath, url, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e);
        }

        return data;
    }

    private class ExecuteHandler extends AbstractExecuteHandler<StockSendImprtDTO> {
        private final List<StockSendImprtDTO> data;

        public ExecuteHandler(List<StockSendImprtDTO> data) {
            this.data = data;
        }

        @Override
        public void batchExecute(List<StockSendImprtDTO> list) {
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            data.addAll(list);
        }
    }

    private class XlsRowReader implements ExcelEventRowReader<StockSendImprtDTO> {
        @Override
        public StockSendImprtDTO getRows(int sheetIndex, int curRow, List<String> values) {
            if (curRow == 1) {
                return null;
            }

            String companyCode = values.get(0);
            String outCode = values.get(1);

            //跳过空行
            if ((! StringUtils.hasText(companyCode)) && (! StringUtils.hasText(outCode))) {
                return null;
            }

            StockSendImprtDTO stockSendImprtDTO = new StockSendImprtDTO(companyCode, outCode, null, false, null);

            if (StringUtils.isEmpty(stockSendImprtDTO.getCompanyCode())) {
                stockSendImprtDTO.setHasError(true);
                stockSendImprtDTO.setErrorMsg("账套不能为空");
                return stockSendImprtDTO;
            }
            if (StringUtils.isEmpty(stockSendImprtDTO.getOutCode())) {
                stockSendImprtDTO.setHasError(true);
                stockSendImprtDTO.setErrorMsg("发货仓外码不能为空");
                return stockSendImprtDTO;
            }

            try {
                WarehouseDTO warehouse = warehouseCacher.findByOutCodeAndBizId(values.get(1), values.get(0));
                stockSendImprtDTO.setWarehouseId(warehouse.getId());
            } catch (Exception e) {
                stockSendImprtDTO.setHasError(true);
                stockSendImprtDTO.setErrorMsg("没有查询到该仓库");
            }
            return stockSendImprtDTO;
        }
    }
}
