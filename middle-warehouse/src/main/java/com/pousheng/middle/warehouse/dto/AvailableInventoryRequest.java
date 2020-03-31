package com.pousheng.middle.warehouse.dto;

import lombok.*;

import java.io.Serializable;

/**
 * 获取可用库存时接收的参数
 *
 * @author feisheng.ch
 * @date 2018/7/2
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AvailableInventoryRequest implements Serializable {

    private static final long serialVersionUID = 3102591371895494557L;

    private String skuCode;

    private Long warehouseId;

}
