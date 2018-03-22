/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员中心店铺信息
 * @author : songrenfei
 */
@Data
public class MemberShop implements Serializable {

    private static final long serialVersionUID = 6085092795266341860L;

    private String id;

    private String storeId;

    private String storeCode;

    private String storeName;

    private String storeFullName;

    private String companyId;

    private String companyName;

    private String mainBrandName;

    private String regionId;

    private String zoneId;

    private String zoneName;

    private String mobile;

    private String email;



}
