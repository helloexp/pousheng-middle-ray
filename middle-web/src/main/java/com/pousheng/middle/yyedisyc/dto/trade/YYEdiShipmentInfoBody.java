package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
@Data
public class YYEdiShipmentInfoBody implements Serializable {

    /**
     * 请求服务ID
     */
    private String sid;

    /**
     * 请求时间
     * yyyy-MM-dd HH:mm:ss
     */
    private String tranReqDate;


    private YYEdiShipmentInfo bizContent;

    public String getSid() {
        return sid;
    }

    public YYEdiShipmentInfoBody sid(String sid) {
        this.sid = sid;
        return this;
    }

    public String getTranReqDate() {
        return tranReqDate;
    }

    public YYEdiShipmentInfoBody tranReqDate(String tranReqDate) {
        this.tranReqDate = tranReqDate;
        return this;
    }

    public YYEdiShipmentInfo getBizContent() {
        return bizContent;
    }

    public YYEdiShipmentInfoBody bizContent(YYEdiShipmentInfo bizContent) {
        this.bizContent = bizContent;
        return this;
    }
}
