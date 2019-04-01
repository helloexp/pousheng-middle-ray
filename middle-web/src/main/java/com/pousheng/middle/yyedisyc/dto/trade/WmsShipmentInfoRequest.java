package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

/**
 * @author tanlongjun
 */
@Data
public class WmsShipmentInfoRequest implements java.io.Serializable {

    /**
     * 请求服务ID
     */
    private String sid;

    /**
     * 请求时间
     * yyyy-MM-dd HH:mm:ss
     */
    private String tranReqDate;

    /**
     * 业务报文
     */
    private WmsShipmentInfo bizContent;
}
