package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/1上午11:19
 */
public class YJErpShipmentProductInfo implements Serializable {
    private static final long serialVersionUID = -2718261898271093843L;

    /**
     * 货号
     */
    @JsonProperty(value = "goods_code")
    private String goods_code;

    /**
     * 尺码
     */
    @JsonProperty(value = "size")
    private String size;

    /**
     * 条码
     */
    @JsonProperty(value = "bar_code")
    private String bar_code;

    /**
     * 商品数量
     */
    @JsonProperty(value = "num")
    private Integer num;

    /**
     * 库房code
     */
    @JsonProperty(value = "warehouse_code")
    private String warehouse_code;


    @JsonIgnore
    public String getGoods_code() {
        return goods_code;
    }

    @JsonIgnore
    public void setGoods_code(String goods_code) {
        this.goods_code = goods_code;
    }

    @JsonIgnore
    public String getSize() {
        return size;
    }

    @JsonIgnore
    public void setSize(String size) {
        this.size = size;
    }

    @JsonIgnore
    public String getBar_code() {
        return bar_code;
    }

    @JsonIgnore
    public void setBar_code(String bar_code) {
        this.bar_code = bar_code;
    }

    @JsonIgnore
    public Integer getNum() {
        return num;
    }

    @JsonIgnore
    public void setNum(Integer num) {
        this.num = num;
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
