package com.pousheng.middle.open.api.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/23
 * pousheng-middle
 */
@Data
public class YyEdiResponseDetail implements java.io.Serializable{
    private static final long serialVersionUID = 5145034654292054894L;
    private Long shipmentId;
    private String yyEdiShipmentId;
}
