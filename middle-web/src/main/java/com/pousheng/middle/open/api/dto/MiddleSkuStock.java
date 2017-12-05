package com.pousheng.middle.open.api.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/13
 * pousheng-middle
 */
@Data
public class MiddleSkuStock implements java.io.Serializable{
    private static final long serialVersionUID = -5289492040650110319L;
    private String skuCode;
    private Integer stock;
}
