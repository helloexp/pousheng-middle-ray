package com.pousheng.middle.web.yintai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品上传日志
 * AUTHOR: zhangbin
 * ON: 2019/7/17
 */
@Data
public class YintaiPushItemLogDTO implements Serializable {
    private static final long serialVersionUID = -6069609252178264901L;

    private String channel;

    private String spuCode;

    private String name;

    private String skuCode;

    private String color;

    private String size;

    private String price;

    private Long brandId;

    private String brand;

    private String outBrand;

    private String syncStatus;

    private Date syncTime;
}
