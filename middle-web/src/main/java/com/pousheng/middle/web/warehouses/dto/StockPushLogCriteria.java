package com.pousheng.middle.web.warehouses.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhaoxw
 * @date 2018/8/31
 */
@Data
public class StockPushLogCriteria extends PagingCriteria implements Serializable {

    private static final long serialVersionUID = 3460075922213525789L;
    /**
     * 创建开始时间
     */
    private Date startAt;

    /**
     * 创建结束时间
     */
    private Date endAt;

    /**
     * 　商品编号
     */
    private String skuCode;

    /**
     * 货号
     */
    private String materialId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 订单来源
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;


    /**
     * 店铺外码
     */
    private String outId;


    public Date getStartAt() {
        return startAt;
    }

    public StockPushLogCriteria startAt(Date startAt) {
        this.startAt = startAt;
        return this;
    }

    public Date getEndAt() {
        return endAt;
    }

    public StockPushLogCriteria endAt(Date endAt) {
        this.endAt = endAt;
        return this;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public StockPushLogCriteria skuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }

    public String getMaterialId() {
        return materialId;
    }

    public StockPushLogCriteria materialId(String materialId) {
        this.materialId = materialId;
        return this;
    }

    public Integer getStatus() {
        return status;
    }

    public StockPushLogCriteria status(Integer status) {
        this.status = status;
        return this;
    }

    public Long getShopId() {
        return shopId;
    }

    public StockPushLogCriteria shopId(Long shopId) {
        this.shopId = shopId;
        return this;
    }

    public String getShopName() {
        return shopName;
    }

    public StockPushLogCriteria shopName(String shopName) {
        this.shopName = shopName;
        return this;
    }

    public String getOutId() {
        return outId;
    }

    public StockPushLogCriteria outId(String outId) {
        this.outId = outId;
        return this;
    }

    /**
     * 如果Start的时间和End的时间一致, 则End+1day
     */
    @Override
    public void formatDate() {
        if (startAt != null && endAt != null) {
            if (startAt.equals(endAt)) {
                endAt = new DateTime(endAt.getTime()).plusDays(1).minusSeconds(1).toDate();
            }
        }
    }

}
