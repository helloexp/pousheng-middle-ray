package com.pousheng.middle.web.shop.event;

import lombok.Getter;

import java.io.Serializable;

/**
 *  创建门店地址信息
 * @author songrenfei
 */
public class CreateShopEvent implements Serializable {


    private static final long serialVersionUID = 8144976237694246387L;
    @Getter
    protected Long shopId;
    @Getter
    protected Long companyId;
    @Getter
    protected String outerId;
    @Getter
    protected String storeCode;

    public CreateShopEvent(Long shopId, Long companyId,String outerId, String storeCode) {
        this.shopId = shopId;
        this.companyId = companyId;
        this.outerId = outerId;
        this.storeCode = storeCode;
    }
}
