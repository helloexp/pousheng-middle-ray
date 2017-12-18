package com.pousheng.middle.order.dto;

import lombok.Data;

import java.util.Map;

/**
 * 请求生成发货单预览的参数
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/8
 * pousheng-middle
 * @author tony
 */
@Data
public class ShipmentRequest implements java.io.Serializable {

    private static final long serialVersionUID = -155017114462660391L;

    private Map<Object,Integer> data;
    private Long warehouseId;
}
