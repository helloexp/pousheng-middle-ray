package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
@Data
public class YYEdiCancelBody implements java.io.Serializable {
    private static final long serialVersionUID = 6870272122005691814L;
    /**
     * 请求服务ID
     */
    private String sid;

    /**
     * 请求时间
     * yyyy-MM-dd HH:mm:ss
     */
    private String tranReqDate;

    private YYEdiCancelInfo bizContent;

    public String getSid() {
        return sid;
    }

    public YYEdiCancelBody sid(String sid) {
        this.sid = sid;
        return this;
    }

    public String getTranReqDate() {
        return tranReqDate;
    }

    public YYEdiCancelBody tranReqDate(String tranReqDate) {
        this.tranReqDate = tranReqDate;
        return this;
    }

    public YYEdiCancelInfo getBizContent() {
        return bizContent;
    }

    public YYEdiCancelBody bizContent(YYEdiCancelInfo bizContent) {
        this.bizContent = bizContent;
        return this;
    }
}
