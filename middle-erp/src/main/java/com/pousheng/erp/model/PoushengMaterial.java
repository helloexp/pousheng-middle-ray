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
    private String materialCode;  //货品编码

    private String materialName; //货品名称

    private String foreignName; //外文名称

    private String cardID; //品牌id

    private String kindID; //类别

    private String seriesID; //系列

    private String itemID; //项目

    private String modelID; //款型

    private String stuff; //面料

    private String texture; //材质说明

    private String sex; //性别

    private Integer yearNo; //年份

    private String invSpec; //规格

    private Boolean Season5; //长青

    private Boolean Season6; //延续

    private List<PoushengSku> skus; //对应的sku信息列表

}
