package com.pousheng.middle.yyedisyc.dto;

import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentResponseField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 同步发货单或者售后单时恒康返回消息的消息头
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class YYEdiShipmentResponse implements Serializable{
    private static final long serialVersionUID = -4030774668654863201L;
    //200:整体成功,100:部分成功,-100:整体失败
    private String errorCode;

    private String description;
    private List<YYEdiShipmentResponseField> fields;
}
