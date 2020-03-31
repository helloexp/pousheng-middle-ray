package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @Description: 中台取消订单推送云聚ERP
 * @author: yjc
 * @date: 2018/7/31下午6:01
 */
public class YJErpCancelInfo implements Serializable {
    private static final long serialVersionUID = -135291545325069816L;

    /**
     * 外部订单号 发货单号
     */
    @JsonProperty(value = "other_order_sn")
    private String other_order_sn;

    /**
     * 库房code
     */
    @JsonProperty(value = "warehouse_code")
    private String warehouse_code;

    @JsonIgnore
    public String getOther_order_sn() {
        return other_order_sn;
    }

    @JsonIgnore
    public void setOther_order_sn(String other_order_sn) {
        this.other_order_sn = other_order_sn;
    }

    @JsonIgnore
    public String getWarehouse_code() {
        return warehouse_code;
    }

    @JsonIgnore
    public void setWarehouse_code(String warehouse_code) {
        this.warehouse_code = warehouse_code;
    }
}
