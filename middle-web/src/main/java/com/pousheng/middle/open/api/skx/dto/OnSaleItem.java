package com.pousheng.middle.open.api.skx.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 在售商品
 * Created by songrenfei on 2018/1/25
 */
@Data
public class OnSaleItem implements Serializable{

    private static final long serialVersionUID = -259660695361921772L;

    private Long itemId;

    private String itemName;

    //private String itemCode;

    private List<OnSaleSku> skus;




}
