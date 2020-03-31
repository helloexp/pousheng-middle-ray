package com.pousheng.middle.web.express.esp.exception;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/9/4
 */
public class UnsuportExpressCompany extends RuntimeException {

    public UnsuportExpressCompany(String error) {
        super(error);
    }

    public UnsuportExpressCompany() {
    }
}
