package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@Data
public class WarehouseAddressDto implements Serializable{
    private static final long serialVersionUID = -8928998224540423389L;

    private String addressName;

    private Integer addressId;
}
