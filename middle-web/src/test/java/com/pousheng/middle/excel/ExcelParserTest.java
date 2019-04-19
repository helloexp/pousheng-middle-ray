package com.pousheng.middle.excel;

import com.github.kevinsawicki.http.HttpRequest;
import io.terminus.excel.read.ExcelEventReader;
import io.terminus.excel.read.ExcelReaderFactory;
import io.terminus.excel.read.handler.AbstractExecuteHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-15 15:23<br/>
 */
@Slf4j
public class ExcelParserTest {
    @Test
    public void test() throws FileNotFoundException {
        String p = "supply-rule-import-temp/DE-190227-0056.xlsx";
        ExcelEventReader<String> r = ExcelReaderFactory.createExcelEventReader(p);
        r.setRowReader((sheetIndex, curRow, rValues) -> {
            boolean b = curRow == 1;
            return rValues.get(0);
        });
        r.setExecuteHandler(new AbstractExecuteHandler<String>() {
            @Override
            public void batchExecute(List<String> result) {
                result.forEach(it -> System.out.println(it));
                throw new RuntimeException("aa");
            }
        });
        File f = new File(p);
        FileInputStream fis = new FileInputStream(f);
        r.process(fis);
    }

    @Test
    public void testDownload() throws IOException {
        log.info("start");
        String url = "https://e1xossfilehdd.blob.core.chinacloudapi.cn/fileserver01/2019/04/15/b024e1fe-13d5-42c0-9db1-d72074c2038b.xlsx";

        // 创建临时文件
        File data = new File("supply-rule-import-temp");
        if (!data.exists()) {
            data.mkdir();
        }
        File file = File.createTempFile("supply-rule-import-", ".xlsx", data);

        HttpRequest requst = HttpRequest.get(url);
        requst.receive(file);

        log.info("stop {}", file.length());
    }
}
