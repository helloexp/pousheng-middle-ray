package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2018/4/1
 */
@Data
public class OutOrderRefundItem implements Serializable{

    private static final long serialVersionUID = 2990157894117999515L;

    private String afterSaleId;//售后单id
    private String skuAfterSaleId;//售后子单id
    private String skuCode;
    private String itemName;
    private Integer quantity;
    private Long fee; //单位分



}
