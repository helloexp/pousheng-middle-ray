package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2018/1/12
 */
@Data
public class SkuIsMposDto implements Serializable{

    private static final long serialVersionUID = 2667051574527168628L;

    private String barcode;

    private Boolean isMpos;

    private Long stock;
}
