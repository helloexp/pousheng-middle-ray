package com.pousheng.middle.web.biz.Exception;

/**
 * 并发执行业务处理异常
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/29
 * pousheng-middle
 */
public class ConcurrentSkipBizException extends RuntimeException {
    public ConcurrentSkipBizException() {
    }

    public ConcurrentSkipBizException(String message) {
        super(message);
    }

    public ConcurrentSkipBizException(Throwable cause) {
        super(cause);
    }

    public ConcurrentSkipBizException(String message, Throwable cause) {
        super(message, cause);
    }
}
