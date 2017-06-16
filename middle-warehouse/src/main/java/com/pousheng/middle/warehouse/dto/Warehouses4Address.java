package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 可以发送到指定地址的仓库列表
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@Data
public class Warehouses4Address implements Serializable{

    /**
     * 地址id
     */
    private Long addressId;

    /**
     * 仓库列表
     */
    private List<WarehouseWithPriority> warehouses;

}
