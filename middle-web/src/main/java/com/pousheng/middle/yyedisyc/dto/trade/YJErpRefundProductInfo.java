package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/1下午3:41
 */
public class YJErpRefundProductInfo implements Serializable {
    private static final long serialVersionUID = 8649746912690058446L;

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
     * 退货原因
     */
    @JsonProperty(value = "exchange_reason_id")
    private Integer exchange_reason_id;

    /**
     * 外观包装类型
     */
    @JsonProperty(value = "appearance")
    private Integer appearance;

    /**
     * 包装情况
     */
    @JsonProperty(value = "packaging")
    private Integer packaging;

    /**
     * 问题类型
     */
    @JsonProperty(value = "problem_type")
    private Integer problem_type;

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
    public Integer getExchange_reason_id() {
        return exchange_reason_id;
    }

    @JsonIgnore
    public void setExchange_reason_id(Integer exchange_reason_id) {
        this.exchange_reason_id = exchange_reason_id;
    }

    @JsonIgnore
    public Integer getAppearance() {
        return appearance;
    }

    @JsonIgnore
    public void setAppearance(Integer appearance) {
        this.appearance = appearance;
    }

    @JsonIgnore
    public Integer getPackaging() {
        return packaging;
    }

    @JsonIgnore
    public void setPackaging(Integer packaging) {
        this.packaging = packaging;
    }

    @JsonIgnore
    public Integer getProblem_type() {
        return problem_type;
    }

    @JsonIgnore
    public void setProblem_type(Integer problem_type) {
        this.problem_type = problem_type;
    }
}
