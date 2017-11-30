package com.pousheng.middle.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 * @author tony
 */
public class PoushengGiftActivity implements java.io.Serializable{
    protected static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final long serialVersionUID = -6297996810819781590L;
    /**
     * 活动id
     */
    private Long id;
    /**
     * 活动名称
     */
    private String name;
    /**
     * 订单规则：类型1.订单满足多少钱不限定活动商品,2.订单满足多少钱限定活动商品,3.订单满足多少件不限定活动商品,4.订单满足多少件限定活动商品
     */
    private Integer orderRule;
    /**
     * 满足活动要求的订单金额(支付金额)
     */
    private Integer orderFee;
    /**
     * 满足活动要求的订单商品数量
     */
    private Integer orderQuantity;
    /**
     * 活动商品的条码
     */
    private String skuCode;

    /**
     * 赠品总价格
     */
    private Integer totalPrice;
    /**
     * 活动状态
     */
    private Integer status;
    /**
     * 活动名额规则：类型:1.不限制前多少人参与活动,2.限制前多少位参与活动
     */
    private Integer quantityRule;
    /**
     * 已经占用的名额
     */
    private Integer alreadyActivityQuantity;
    /**
     * 总的名额
     */
    private Integer activityQuantity;

    @JsonIgnore
    protected String extraJson;
    protected Map<String, String> extra;
    /**
     * 活动开始时间
     */
    private Date activityStartAt;
    /**
     * 活动结束时间
     */
    private Date activityEndAt;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 更新时间
     */
    private Date updatedAt;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrderRule() {
        return orderRule;
    }

    public void setOrderRule(Integer orderRule) {
        this.orderRule = orderRule;
    }

    public Integer getOrderFee() {
        return orderFee;
    }

    public void setOrderFee(Integer orderFee) {
        this.orderFee = orderFee;
    }

    public Integer getOrderQuantity() {
        return orderQuantity;
    }

    public void setOrderQuantity(Integer orderQuantity) {
        this.orderQuantity = orderQuantity;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getQuantityRule() {
        return quantityRule;
    }

    public void setQuantityRule(Integer quantityRule) {
        this.quantityRule = quantityRule;
    }

    public Integer getAlreadyActivityQuantity() {
        return alreadyActivityQuantity;
    }

    public void setAlreadyActivityQuantity(Integer alreadyActivityQuantity) {
        this.alreadyActivityQuantity = alreadyActivityQuantity;
    }

    public Integer getActivityQuantity() {
        return activityQuantity;
    }

    public void setActivityQuantity(Integer activityQuantity) {
        this.activityQuantity = activityQuantity;
    }

    public String getExtraJson() {
        return extraJson;
    }


    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtraJson(String extraJson) throws Exception {
        this.extraJson = extraJson;
        if (Strings.isNullOrEmpty(extraJson)) {
            this.extra = Collections.emptyMap();
        } else {
            this.extra = (Map)objectMapper.readValue(extraJson, JacksonType.MAP_OF_STRING);
        }

    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if (extra != null && !extra.isEmpty()) {
            try {
                this.extraJson = objectMapper.writeValueAsString(extra);
            } catch (Exception var3) {
                ;
            }
        } else {
            this.extraJson = null;
        }

    }

    public Date getActivityStartAt() {
        return activityStartAt;
    }

    public void setActivityStartAt(Date activityStartAt) {
        this.activityStartAt = activityStartAt;
    }

    public Date getActivityEndAt() {
        return activityEndAt;
    }

    public void setActivityEndAt(Date activityEndAt) {
        this.activityEndAt = activityEndAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }
}
