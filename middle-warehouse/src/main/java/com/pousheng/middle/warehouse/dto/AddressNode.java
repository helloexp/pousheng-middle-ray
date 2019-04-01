package com.pousheng.middle.warehouse.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@EqualsAndHashCode
@ToString
public class AddressNode implements Serializable {
    private static final long serialVersionUID = -151687580140517292L;

    @Getter
    private final  Long id;

    @Getter
    private final String name;

    @Getter
    private final Long pid;

    @Getter
    private final Integer level;

    public AddressNode(Long id, String name, Long pid, Integer level) {
        this.id = id;
        this.name = name;
        this.pid = pid;
        this.level = level;
    }
}
