package com.pousheng.middle.yyedisyc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 同步发货单或者售后单时云聚ERP返回消息的消息头
 * @author: yjc
 * @date: 2018/7/31下午5:19
 */
public class YJErpResponse implements Serializable {
    private static final long serialVersionUID = -7772204535078453285L;

    private Integer error;

    private String error_info;

    private List<YJErpResponseField> data;

    public Integer getError() {
        return error;
    }

    public void setError(Integer error) {
        this.error = error;
    }

    public String getError_info() {
        return error_info;
    }

    public void setError_info(String error_info) {
        this.error_info = error_info;
    }

    public List<YJErpResponseField> getData() {
        return data;
    }

    public void setData(List<YJErpResponseField> data) {
        this.data = data;
    }

    public static class YJErpResponseField implements Serializable{

        private static final long serialVersionUID = -1071381464439932728L;

        // 下单成功返回订单号
        @JsonProperty(value = "order_sn")
        private String order_sn;

        // 失败原因
        @JsonProperty(value = "error_info")
        private String error_info;

        @JsonIgnore
        public String getOrder_sn() {
            return order_sn;
        }

        @JsonIgnore
        public void setOrder_sn(String order_sn) {
            this.order_sn = order_sn;
        }

        @JsonIgnore
        public String getError_info() {
            return error_info;
        }

        @JsonIgnore
        public void setError_info(String error_info) {
            this.error_info = error_info;
        }
    }
}
