package com.pousheng.middle.web.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/12/15
 */
@Data
public class MemberCenterAddressDto implements Serializable {

    private static final long serialVersionUID = 2365234386396013436L;

    private String address;
    private String areaCode;
    private String areaName;
    private String cityCode;
    private String cityName;
    private String provinceCode;
    private String provinceName;
}
