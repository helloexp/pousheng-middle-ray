package com.pousheng.middle.order.dto;

import io.terminus.parana.attribute.dto.SkuAttribute;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品基本信息
 * Created by songrenfei on 2017/7/1
 */
@Data
public class BasicItemInfo implements Serializable{


    private static final long serialVersionUID = 503660743978795391L;
    //条码：电商商品中外部id（也是中台货品条码）
    private String skuCode;
    //外部商品id：电商订单中商品id。
    private String outSkuCode;
    //商品名称
    private String skuName;
    //分摊优惠
    private Integer apportionDiscount;
    //商品净价
    private Integer cleanPrice;
    //商品总净价
    private Integer cleanFee;

    /**
     * sku的销售属性
     */
    private List<SkuAttribute> attrs;

}
