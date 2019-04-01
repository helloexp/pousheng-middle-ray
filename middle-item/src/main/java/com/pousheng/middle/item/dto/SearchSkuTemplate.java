/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.dto;

import io.terminus.parana.attribute.dto.GroupedOtherAttribute;
import io.terminus.parana.attribute.dto.SkuAttribute;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 商品搜索
 */
public class SearchSkuTemplate implements Serializable {

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

    /**
     * 型号
     */
    @Getter
    @Setter
    private String specification;


    /**
     * 商品类型 0, 非mpos, 1. mpos商品
     */
    @Getter
    @Setter
    private Integer type;


    /**
     * 最后更新时间
     */
    @Getter
    @Setter
    private Date updatedAt;




    //from db

    /**
     * 折扣
     */
    @Getter
    @Setter
    private Integer discount;

    /**
     * 销售价
     */
    @Getter
    @Setter
    private Integer price;

    /**
     * 吊牌价
     */
    @Getter
    @Setter
    private Integer originPrice;

    /**
     * 类目名称
     */
    @Getter
    @Setter
    private String categoryName;


    /**
     * 属性信息
     */
    @Getter
    @Setter
    List<GroupedOtherAttribute> otherAttrs;


    /**
     * 属性信息
     */
    @Getter
    @Setter
    private List<SkuAttribute> attrs;


    /**
     * 所属分组
     */
    @Getter
    @Setter
    private Set<Long> groupIds;


    /**
     * 将其除外的分组
     */
    @Getter
    @Setter
    private Set<Long> excludeGroupIds;


    /**
     * 后台类目1-4级
     */
    @Getter
    @Setter
    private List<Long> categoryIds;


    /**
     * 所有商品属性,包括sku属性及非sku属性, 属性以key:value的String来存储, 搜索也需要以key:value的term 来搜索
     */
    @Getter
    @Setter
    private List<String> attributes;


    /**
     * 上市时间
     */
    @Getter
    @Setter
    private Date saleDate;


}
