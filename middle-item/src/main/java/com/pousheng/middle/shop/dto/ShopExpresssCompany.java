package com.pousheng.middle.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 门店拥有的快递信息
 * Created by songrenfei on 2017/12/5
 */
@Data
public class ShopExpresssCompany implements Serializable{

    //代码
    private String code;

    //名称
    private String name;
}
