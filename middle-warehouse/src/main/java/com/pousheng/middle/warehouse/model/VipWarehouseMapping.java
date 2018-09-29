package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Author: zhaoxiaowei
 * Desc: Model类
 * Date: 2018-09-29
 */
@Data
public class VipWarehouseMapping implements Serializable {

    private static final long serialVersionUID = 821533604091591514L;

    private Long id;

    /**
     * 中台仓库id
     */
    private Long warehouseId;

    /**
     * 唯品会仓库编码
     */
    private String vipStoreSn;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public VipWarehouseMapping warehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public String getVipStoreSn() {
        return vipStoreSn;
    }

    public VipWarehouseMapping vipStoreSn(String vipStoreSn) {
        this.vipStoreSn = vipStoreSn;
        return this;
    }
}
