package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhaoxw
 * @date 2018/8/9
 */
@Data
public class SkuStockRuleImportInfo implements Serializable{

    private static final long serialVersionUID = 4075636810006283211L;

    private String filePath;

    private Long openShopId;

    private Long userId;

    private Long shopRuleId;

    public String getFilePath() {
        return filePath;
    }

    public SkuStockRuleImportInfo filePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public Long getOpenShopId() {
        return openShopId;
    }

    public SkuStockRuleImportInfo openShopId(Long openShopId) {
        this.openShopId = openShopId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public SkuStockRuleImportInfo userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Long getShopRuleId() {
        return shopRuleId;
    }

    public SkuStockRuleImportInfo shopRuleId(Long shopRuleId) {
        this.shopRuleId = shopRuleId;
        return this;
    }
}
