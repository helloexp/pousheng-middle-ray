package com.pousheng.middle.web.biz.dto;

import lombok.Data;

@Data
public class HeadResponse {

    private String code;
    private String message;
    private String result;
    private String serialNo;
    private String sendTime;

}
