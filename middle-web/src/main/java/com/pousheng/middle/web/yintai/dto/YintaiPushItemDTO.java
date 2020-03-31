package com.pousheng.middle.web.yintai.dto;

import com.google.common.collect.Maps;
import com.pousheng.middle.web.yintai.YintaiAttributeEnum;
import lombok.Data;
import org.assertj.core.util.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 推送商品dto
 * AUTHOR: zhangbin
 * ON: 2019/7/3
 */
@Data
public class YintaiPushItemDTO implements Serializable {
    private static final long serialVersionUID = 5123291174417167185L;

    private Long skuId;

    private Long spuId;

    private String spuCode;//货号

    private String skuCode;//条码

    private String price;//零售价

    private String name;

    private Long brandId;

    private String outBrandId;//银泰品牌id

    private String brandName;

    //货品年份,货品季节,颜色,类别,性别，尺码
    private Map<YintaiAttributeEnum, String> attrs = Maps.newConcurrentMap();

}
