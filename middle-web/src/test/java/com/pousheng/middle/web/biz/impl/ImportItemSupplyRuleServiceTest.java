package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.model.PoushengCompensateBiz;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ImportItemSupplyRuleServiceTest {

    @InjectMocks
    private ImportItemSupplyRuleService importItemSupplyRuleService;
    @Test
    public void handle() {
        try {
            PoushengCompensateBiz biz = new PoushengCompensateBiz();
            biz.setContext(
                "https://e1xossfilehdd.blob.core.chinacloudapi.cn/fileserver01/2018/10/12/fd63f6a9-0e4a-4ef3-9977-fe73da5449b6.xlsx");

            importItemSupplyRuleService.handle(biz);
        }catch (Exception e){
            log.error("test case error.",e);
        }
    }
}