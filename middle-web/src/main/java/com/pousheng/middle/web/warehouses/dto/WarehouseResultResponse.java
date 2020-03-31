package com.pousheng.middle.web.warehouses.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/14
 */
@Data
@ToString
public class WarehouseResultResponse {

    private boolean success;

    private List<WarehouseResult> data;

    private String msg;

    public void fail(String msg){
        this.success = false;
        this.msg = msg;
    }

    public void success(List<WarehouseResult> data) {
        this.success = true;
        this.data = data;
    }
}
