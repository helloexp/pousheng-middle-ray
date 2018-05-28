package com.pousheng.middle.item.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2018/1/14
 */
@Data
public class ItemNameAndStock implements Serializable{


    private static final long serialVersionUID = 5727159975734614210L;

    private String name;

    //总库存
    private Long stockQuantity = 0L;

    //当前店铺库存
    private Long currentShopQuantity = 0L;


}
