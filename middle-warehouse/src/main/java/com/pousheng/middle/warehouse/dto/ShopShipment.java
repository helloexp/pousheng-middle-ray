package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 门店发货明细
 *
 * Author: songrenfei
 * Date: 2017-12-19
 */
@Data
public class ShopShipment implements Serializable {

    private static final long serialVersionUID = 2584014003960882567L;
    private Long shopId;

    private String shopName;

    private List<SkuCodeAndQuantity> skuCodeAndQuantities;
}
