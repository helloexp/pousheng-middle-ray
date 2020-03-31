package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
@Data
public class YYEdiReturnInfoBody implements java.io.Serializable{
    private static final long serialVersionUID = 7695791106328104770L;
    /**
     * 请求服务ID
     */
    private String sid;

    /**
     * 请求时间
     * yyyy-MM-dd HH:mm:ss
     */
    private String tranReqDate;

    private YYEdiReturnInfo bizContent;

    public String getSid() {
        return sid;
    }

    public YYEdiReturnInfoBody sid(String sid) {
        this.sid = sid;
        return this;
    }

    public String getTranReqDate() {
        return tranReqDate;
    }

    public YYEdiReturnInfoBody tranReqDate(String tranReqDate) {
        this.tranReqDate = tranReqDate;
        return this;
    }

    public YYEdiReturnInfo getBizContent() {
        return bizContent;
    }

    public YYEdiReturnInfoBody bizContent(YYEdiReturnInfo bizContent) {
        this.bizContent = bizContent;
        return this;
    }
}
