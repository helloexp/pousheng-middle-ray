package com.pousheng.middle.open.qimen;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by cp on 8/22/17.
 */
@XStreamAlias("response")
@Data
public class QimenResponse implements Serializable {

    private static final long serialVersionUID = -4690249475876734603L;

    @XStreamAlias("flag")
    private String flag;

    @XStreamAlias("code")
    private String code;

    @XStreamAlias("message")
    private String message;

    public static QimenResponse ok() {
        QimenResponse response = new QimenResponse();
        response.setFlag("success");
        response.setCode("0");
        return response;
    }

    public static QimenResponse fail(String msg) {
        QimenResponse response = new QimenResponse();
        response.setFlag("fail");
        response.setCode("-1");
        response.setMessage(msg);
        return response;
    }

}
