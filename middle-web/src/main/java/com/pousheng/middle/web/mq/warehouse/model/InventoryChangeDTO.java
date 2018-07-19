package com.pousheng.middle.web.mq.warehouse.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存变动后通知消息的数据结构
 *
 * @auther feisheng.ch
 * @time 2018/5/24
 */
@Data
@AllArgsConstructor
@Builder
public class InventoryChangeDTO implements Serializable {

    private static final long serialVersionUID = -7693510179844801115L;

    private Long warehouseId;

    private String skuCode;

}
