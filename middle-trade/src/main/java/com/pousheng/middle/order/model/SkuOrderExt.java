package com.pousheng.middle.order.model;

import lombok.Data;

/**
 * Created by tony on 2017/8/2.
 * pousheng-middle
 */
@Data
public class SkuOrderExt implements java.io.Serializable {
    private static final long serialVersionUID = 5332103712124150361L;

    private Long id;

    private String skuCode;

    private Long skuId;
}
