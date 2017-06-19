package com.pousheng.middle.web.warehouses.dto;

import io.terminus.parana.attribute.dto.SkuAttribute;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@Data
public class SkuStock implements Serializable {

    private static final long serialVersionUID = 1439907674352203884L;

    private Long skuId;

    private String skuCode;

    private String name;

    private List<SkuAttribute> skuAttrs;

    private Long stock;
}
