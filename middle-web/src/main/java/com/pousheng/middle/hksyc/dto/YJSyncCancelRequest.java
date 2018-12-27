package com.pousheng.middle.hksyc.dto;

import java.io.Serializable;

/**
 * @author zhaoxw
 * @date 2018/11/23
 */
public class YJSyncCancelRequest implements Serializable {

    private static final long serialVersionUID = 3013794694116159017L;

    private String order_sn;

    private int error_code;

    private String error_info;


    public String getOrder_sn() {
        return order_sn;
    }

    public YJSyncCancelRequest order_sn(String order_sn) {
        this.order_sn = order_sn;
        return this;
    }

    public int getError_code() {
        return error_code;
    }

    public YJSyncCancelRequest error_code(int error_code) {
        this.error_code = error_code;
        return this;
    }

    public String getError_info() {
        return error_info;
    }

    public YJSyncCancelRequest error_info(String error_info) {
        this.error_info = error_info;
        return this;
    }




}
