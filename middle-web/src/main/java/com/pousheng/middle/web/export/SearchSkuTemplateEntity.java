package com.pousheng.middle.web.export;

import com.pousheng.middle.common.utils.batchhandle.ExportEditable;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import io.terminus.parana.attribute.dto.OtherAttribute;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;


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

    @ExportTitle("年份")
    private String year;

    @ExportTitle("季节")
    private String season;

    @ExportTitle("品牌")
    private String brandName;

    @ExportTitle("类别")
    private String categoryName;

    @ExportTitle("货品条码")
    private String barcode;

    @ExportTitle("SPUID")
    private String spuId;

    @ExportTitle("折扣")
    @ExportEditable(true)
    private String discount;

    @ExportTitle("吊牌价")
    private String originPrice;

    @ExportTitle("销售价")
    private String price;

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
        if(searchSkuTemplate.getDiscount() != null)
            this.discount = String.valueOf(searchSkuTemplate.getDiscount());
        DecimalFormat df =new DecimalFormat("#.00");
        if(searchSkuTemplate.getPrice() != null)
            this.price = df.format(searchSkuTemplate.getPrice()/100);
        if(searchSkuTemplate.getOriginPrice() != null)
            this.originPrice = df.format(searchSkuTemplate.getOriginPrice()/100);
        this.barcode = searchSkuTemplate.getSkuCode();
        if(searchSkuTemplate.getOtherAttrs() != null){
            List<OtherAttribute> list = searchSkuTemplate.getOtherAttrs().get(0).getOtherAttributes();
            for (OtherAttribute attr:list) {
                if(Objects.equals(attr.getAttrKey(),"年份"))
                    this.year = attr.getAttrVal();
                if(Objects.equals(attr.getAttrKey(),"季节"))
                    this.season = attr.getAttrVal();
            }
        }
        if(searchSkuTemplate.getSpuId() != null)
            this.spuId = searchSkuTemplate.getSpuId().toString();
    }

    public SearchSkuTemplateEntity(String[] strs){
        if(StringUtils.isNotEmpty(strs[0]))
            this.id = Long.parseLong(strs[0].replace("\"",""));
        if(StringUtils.isNotEmpty(strs[1]))
            this.spuCode = strs[1].replace("\"","");
        if(StringUtils.isNotEmpty(strs[2]))
            this.name = strs[2].replace("\"","");
        if(StringUtils.isNotEmpty(strs[3]))
            this.color = strs[3].replace("\"","");
        if(StringUtils.isNotEmpty(strs[4]))
            this.size = strs[4].replace("\"","");
        if (StringUtils.isNotEmpty(strs[5]))
            this.year = strs[5].replace("\"","");
        if(StringUtils.isNotEmpty(strs[6]))
            this.season = strs[6].replace("\"","");
        if (StringUtils.isNotEmpty(strs[7]))
            this.brandName = strs[7].replace("\"","");
        if(StringUtils.isNotEmpty(strs[8]))
            this.categoryName = strs[8].replace("\"","");
        if(StringUtils.isNotEmpty(strs[9]))
            this.barcode = strs[9].replace("\"","");
        if(StringUtils.isNotEmpty(strs[10]))
            this.spuId = strs[10].replace("\"","");
        if(StringUtils.isNotEmpty(strs[11]))
            this.discount = strs[11].replace("\"","");
        if(StringUtils.isNotEmpty(strs[12]))
            this.originPrice = strs[12].replace("\"","");
        if(StringUtils.isNotEmpty(strs[13])) {
            this.price = strs[13].replace("\"","");
        }
        this.error = strs[14];
    }

}
