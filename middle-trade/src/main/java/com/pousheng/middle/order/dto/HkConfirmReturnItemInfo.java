package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 恒康退货完成，确认收到买家退换货物信息
 * Created by songrenfei on 2017/7/25
 */
@Data
public class HkConfirmReturnItemInfo implements Serializable {

    private static final long serialVersionUID = -7701262851252290310L;


    private String itemCode;

    private Integer quantity;
}
