package com.pousheng.middle.warehouse.dto;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
public class WarehouseAddressDto implements Serializable{
    private static final long serialVersionUID = -8928998224540423389L;

    @Getter
    @Setter
    private String addressName;

    @Getter
    @Setter
    private Long addressId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarehouseAddressDto that = (WarehouseAddressDto) o;
        return Objects.equal(addressId, that.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(addressId);
    }

    @Override
    public String toString() {
        return "WarehouseAddressDto{" +
                "addressName='" + addressName + '\'' +
                ", addressId=" + addressId +
                '}';
    }
}
