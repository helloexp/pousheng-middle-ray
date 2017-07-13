package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
@Data
public class ThinShop implements Serializable {

    private static final long serialVersionUID = 5196822217885922590L;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 是否可编辑
     */
    private boolean editable = true;
}
