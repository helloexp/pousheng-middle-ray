package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/9/4
 */
@Data
public class MposResponse {
    private boolean success;            // 是否成功
    private String msg;
    private String expressOrderId;      // 快递单号
    //是否使用中台ESP快递服务
    private boolean useMiddleService;
    private boolean process;
}
