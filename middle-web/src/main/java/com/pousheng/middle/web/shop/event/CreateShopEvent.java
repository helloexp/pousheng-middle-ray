package com.pousheng.middle.web.shop.event;

import lombok.Getter;

import java.io.Serializable;

/**
 *  创建门店地址信息
 * @author songrenfei
 */
public class CreateShopEvent implements Serializable {


    @Getter
    protected Long shopId;
    @Getter
    protected String companyId;
    @Getter
    protected String storeCode;

    public CreateShopEvent(Long shopId, String companyId, String storeCode) {
        this.shopId = shopId;
        this.companyId = companyId;
        this.storeCode = storeCode;
    }
}
