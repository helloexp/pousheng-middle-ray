package com.pousheng.middle.open.api.skx.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 在售sku
 * Created by songrenfei on 2018/1/25
 */
@Data
public class OnSaleSku implements Serializable{

    private static final long serialVersionUID = 8196914359985732917L;

    private Long skuId;

    private String skuCode;

}
