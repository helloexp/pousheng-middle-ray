package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@Data
public class WarehouseAddressTree implements Serializable {
    private static final long serialVersionUID = -1080099775959648485L;

    /**
     * 当前地址
     */
    private AddressNode current;

    /**
     * 0: 不选,  1: 部分选中, 2: 全部选中
     */
    private int selected;

    /**
     * 对应的下级地址
     */
    private List<WarehouseAddressTree> children;
}
