package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

import java.util.Date;

@Data
public class ExpressNoRequest {
    //请求服务ID
    private String sid;
    //请求时间
    private Date tranReqDate;
    //业务报文
    private BizContent bizContent;
}