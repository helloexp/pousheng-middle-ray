package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 * 原商品
 */
@Data
public class PoushengMaterial implements Serializable {

    private static final long serialVersionUID = 7436383982566463632L;
    private String material_code;  //货品编码

    private String material_name; //货品名称

    private String foreign_name; //外文名称

    private String card_id; //品牌id

    private String card_name; //品牌名称

    private String kind_name; //类别

    private String series_name; //系列

    private String item_name; //项目

    private String model_name; //款型

    private String color_id;// 颜色id

    private String color_name; //颜色名称

    private String stuff; //面料

    private String texture; //材质说明

    private String sex; //性别

    private Integer year_no; //年份

    private String inv_spec; //规格

    private Boolean season5; //长青

    private Boolean season6; //延续

    private Double pro_retail_price; //市场价

    private List<PoushengSize> size; //对应的sku信息列表

}
