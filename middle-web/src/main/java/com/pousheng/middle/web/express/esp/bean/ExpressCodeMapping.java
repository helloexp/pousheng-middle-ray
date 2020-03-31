package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/9/2
 */
@Data
public class ExpressCodeMapping {
    //中台快递代码
    private String middleExpressCode;
    //esp快递代码
    private String espExpressCode;
}
