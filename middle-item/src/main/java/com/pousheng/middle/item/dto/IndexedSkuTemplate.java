/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用于商品搜索的DTO
 */
public class IndexedSkuTemplate implements Serializable {

    private static final long serialVersionUID = 1905157270686430926L;

    /**
     * ID
     */
    @Getter
    @Setter
    private Long id;

    /**
     * 商品名称
     */
    @Getter
    @Setter
    private String name;


    /**
     * spu编码
     */
    @Getter
    @Setter
    private String spuCode;

    /**
     * 商品编码
     */
    @Getter
    @Setter
    private String skuCode;


    /**
     * SPU ID , 如果是通过spu发布商品
     */
    @Getter
    @Setter
    private Long spuId;

    /**
     * 品牌ID
     */
    @Getter
    @Setter
    private Long brandId;

    /**
     * 品牌名称
     */
    @Getter
    @Setter
    private String brandName;


    /**
     * 主图URL
     */
    @Getter
    @Setter
    private String mainImage;


    @Getter
    @Setter
    private Integer price;

    /**
     * 型号
     */
    @Getter
    @Setter
    private String specification;


    /**
     * 商品类型 1, 非mpos, 2. mpos商品
     */
    @Getter
    @Setter
    private Integer type;


    /**
     * 后台类目1-4级
     */
    @Getter
    @Setter
    private List<Long> categoryIds;



    /**
     * 当前商品后台类目名称
     */
    @Getter
    @Setter
    private String categoryName;

    /**
     * 所有商品属性,包括sku属性及非sku属性, 属性以key:value的String来存储, 搜索也需要以key:value的term 来搜索
     */
    @Getter
    @Setter
    private List<String> attributes;


    /**
     * 最后更新时间
     */
    @Getter
    @Setter
    private Date updatedAt;
}