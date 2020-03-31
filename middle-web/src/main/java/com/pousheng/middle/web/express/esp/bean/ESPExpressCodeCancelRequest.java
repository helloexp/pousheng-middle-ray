package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

import java.util.Date;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/27
 */
@Data
public class ESPExpressCodeCancelRequest {
    private String sid;
    private Date tranReqDate;
    private ESPExpressCodeCancelRequestContent bizContent;
}
