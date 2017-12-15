package com.pousheng.middle.web.export;

import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.web.utils.export.ExportEditable;
import com.pousheng.middle.web.utils.export.ExportTitle;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;


@Data
public class SearchSkuTemplateEntity {

    @ExportTitle("编号")
    private Long id;

    @ExportTitle("货号")
    private String spuCode;

    @ExportTitle("货品名称")
    private String name;

    @ExportTitle("颜色")
    private String color;

    @ExportTitle("尺码")
    private String size;

    @ExportTitle("季度")
    private String season;

    @ExportTitle("品牌")
    private String brandName;

    @ExportTitle("类别")
    private String categoryName;

    @ExportTitle("折扣")
    @ExportEditable
    private Integer discount;

    @ExportTitle("销售价")
    private Integer price;

    @ExportTitle("吊牌价")
    private Integer originPrice;

    @ExportTitle("异常原因")
    private String error;


    public SearchSkuTemplateEntity(SearchSkuTemplate searchSkuTemplate){
        this.id = searchSkuTemplate.getId();
        this.spuCode = searchSkuTemplate.getSpuCode();
        this.name = searchSkuTemplate.getName();
        if(CollectionUtils.isNotEmpty(searchSkuTemplate.getAttrs()) && searchSkuTemplate.getAttrs().size() > 1){
            this.color = searchSkuTemplate.getAttrs().get(0).getAttrVal();
            this.size = searchSkuTemplate.getAttrs().get(1).getAttrVal();
        }
        this.brandName = searchSkuTemplate.getBrandName();
        this.categoryName = searchSkuTemplate.getCategoryName();
        this.discount = searchSkuTemplate.getDiscount();
        this.price = searchSkuTemplate.getPrice();
        this.originPrice = searchSkuTemplate.getOriginPrice();
    }

}
