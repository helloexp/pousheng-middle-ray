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
                "https://e1xossfilehdd.blob.core.chinacloudapi.cn/fileserver01/2018/10/11/40f487eb-d515-4671-ba43-e6ebacb7d822.xlsx");

            importItemSupplyRuleService.handle(biz);
        }catch (Exception e){
            log.error("test case error.",e);
        }
    }
}