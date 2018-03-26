/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员中心运动城
 * @author : songrenfei
 */
@Data
public class MemberSportCity implements Serializable {

    private static final long serialVersionUID = -622843127424434294L;

    private String id;

    private String sportCityId;

    private String sportCityCode;

    private String sportCityName;

    private String sportCityFullName;

    private String companyId;

    private String companyName;

    private String regionId;

    private String zoneId;

    private String zoneName;

    //"省名称"
    private String provinceName;

    //"市名称")
    private String cityName;

    //"区名称")
    private String areaName;

    // "省代码", position = 37)
    private String provinceCode;

    //"市代码", position = 38)
    private String cityCode;

    //"区代码", position = 39)
    private String areaId;

    //"店铺电话", position = 19)
    private String telphone;

    //"邮箱", position = 19)
    private String email;

    // "店铺地址", position = 18)
    private String address;

}
