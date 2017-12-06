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

    private String regionId;

    private String zoneId;

    private String zoneName;

}
