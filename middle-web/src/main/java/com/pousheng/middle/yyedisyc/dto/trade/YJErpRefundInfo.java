package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 订单退货同步到云聚ERP
 * @author: yjc
 * @date: 2018/7/31下午7:51
 */
public class YJErpRefundInfo implements Serializable {

    private static final long serialVersionUID = -4833279064315892919L;

    /**
     * 联系人名称
     */
    @JsonProperty(value = "consignee")
    private String consignee;

    /**
     * 联系人手机
     */
    @JsonProperty(value = "mobile")
    private String mobile;

    /**
     * 退货快递单号,发货单的快递单号
     */
    @JsonProperty(value = "express_num")
    private String express_num;

    /**
     * 备注信息
     */
    @JsonProperty(value = "message")
    private String message;

    /**
     * 中台退货单号
     */
    @JsonProperty(value = "mg_exchange_sn")
    private String mg_exchange_sn;


    /**
     * 发货的库房的库房单号
     */
    @JsonProperty(value = "warehouse_code")
    private String warehouse_code;

    /**
     * 订单号，中台发货单号
     */
    @JsonProperty(value = "order_sn")
    private String order_sn;

    /**
     * 退货商品信息
     */
   private List<YJErpRefundProductInfo> product_info;


    @JsonIgnore
    public String getConsignee() {
        return consignee;
    }

    @JsonIgnore
    public void setConsignee(String consignee) {
        this.consignee = consignee;
    }

    @JsonIgnore
    public String getMobile() {
        return mobile;
    }

    @JsonIgnore
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @JsonIgnore
    public String getExpress_num() {
        return express_num;
    }

    @JsonIgnore
    public void setExpress_num(String express_num) {
        this.express_num = express_num;
    }

    @JsonIgnore
    public String getMessage() {
        return message;
    }

    @JsonIgnore
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonIgnore
    public String getMg_exchange_sn() {
        return mg_exchange_sn;
    }

    @JsonIgnore
    public void setMg_exchange_sn(String mg_exchange_sn) {
        this.mg_exchange_sn = mg_exchange_sn;
    }

    @JsonIgnore
    public String getWarehouse_code() {
        return warehouse_code;
    }

    @JsonIgnore
    public void setWarehouse_code(String warehouse_code) {
        this.warehouse_code = warehouse_code;
    }

    @JsonIgnore
    public String getOrder_sn() {
        return order_sn;
    }

    @JsonIgnore
    public void setOrder_sn(String order_sn) {
        this.order_sn = order_sn;
    }

    public List<YJErpRefundProductInfo> getProduct_info() {
        return product_info;
    }

    public void setProduct_info(List<YJErpRefundProductInfo> product_info) {
        this.product_info = product_info;
    }
}
