package com.pousheng.middle.web.biz.Exception;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/14
 */
public class NoTryException extends RuntimeException {

    public NoTryException() {
    }

    public NoTryException(String message) {
        super(message);
    }

    public NoTryException(Throwable cause) {
        super(cause);
    }

    public NoTryException(String message, Throwable cause) {
        super(message, cause);
    }
}
