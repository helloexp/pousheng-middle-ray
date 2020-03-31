package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-27
 */
@Data
public class PoushengSku implements Serializable{
    private static final long serialVersionUID = -5770401286852001678L;

    private String barCode; //条码

    private String sizeId; //尺码id

    private String sizeName; //尺码名称

    private String colorId; //颜色id

    private String colorName; //颜色名称

    private String marketPrice; //市场价

    private String materialId; //物料id

    private String materialCode; //物料编码
}
