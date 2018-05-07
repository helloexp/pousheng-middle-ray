package com.pousheng.middle.web.shop.event;

import lombok.Getter;

import java.io.Serializable;

/**
 *  更新门店地址信息
 * @author songrenfei
 */
public class UpdateShopEvent implements Serializable {

    @Override
    public String toString() {
        return "UpdateShopEvent{" +
                "shopId=" + shopId +
                ", companyId=" + companyId +
                ", storeCode='" + storeCode + '\'' +
                '}';
    }

    private static final long serialVersionUID = 3285534935695785921L;
    @Getter
    protected Long shopId;
    @Getter
    protected Long companyId;
    @Getter
    protected String storeCode;

    public UpdateShopEvent(Long shopId, Long companyId, String storeCode) {
        this.shopId = shopId;
        this.companyId = companyId;
        this.storeCode = storeCode;
    }
}
