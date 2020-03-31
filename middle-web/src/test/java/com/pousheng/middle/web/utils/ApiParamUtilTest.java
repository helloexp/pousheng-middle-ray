package com.pousheng.middle.web.utils;

import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Map;

@Slf4j
public class ApiParamUtilTest {

    @Test
    public void validateRequired() {
        try {
            A aaa = new A();
            aaa.setA("s");
            ApiParamUtil.validateRequired(aaa, "a","d");
        }catch (OPServerException e){
            log.error("验证失败",e);
        }
    }

    @Data
    public class A{

        private int aa;

        private String a;

        private String b;

        private Integer c;

        private Long d;

        private Map<String,String> e;
    }
}