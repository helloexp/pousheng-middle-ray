package com.pousheng.middle.yyedisyc.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 同步发货单或者售后单时恒康返回消息的消息头
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class YYEdiResponseHead implements Serializable{
    private static final long serialVersionUID = -4030774668654863201L;
    private String code;
    private String message;
    private String sendTime;
    private String result;
}
